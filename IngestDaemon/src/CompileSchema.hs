{-# LANGUAGE TupleSections     #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE ViewPatterns      #-}
{-# LANGUAGE FlexibleInstances #-}
{-# LANGUAGE ParallelListComp  #-}
module CompileSchema
  ( -- * Operations
    compile, compileNode, compileEdge, serializeOperation
    -- * Types
    , GremlinValue(..)
    , Operation(..)
  ) where

import qualified Data.Aeson as A
import           Data.Binary (encode,decode)
import qualified Data.ByteString as BS
import qualified Data.ByteString.Base64 as B64
import qualified Data.ByteString.Lazy as ByteString
import           Data.Char (isUpper)
import qualified Data.Char as C
import           Data.Foldable as F
import           Data.Int (Int64)
import           Data.List (intersperse)
import           Data.Map (Map)
import qualified Data.Map as Map
import           Data.Maybe (catMaybes)
import           Data.Monoid
import qualified Data.Set as Set
import           Data.Text (Text)
import qualified Data.Text as T
import qualified Data.Text.Encoding as T
import           Schema hiding (Env)
import           System.Entropy (getEntropy)

-- Operations represent gremlin-groovy commands such as:
-- assume: graph = TinkerGraph.open()
--         g = graph.traversal(standard())
data Operation id = InsertVertex { vertexType :: Text
                                 , ident      :: Text
                                 , properties :: [(Text,GremlinValue)]
                                 }
                  | InsertEdge { ident            :: Text
                               , src,dst          :: id
                               , generateVertices :: Bool
                               }
                  | InsertReifiedEdge
                            { labelNode :: Text
                            , labelE1   :: Text
                            , labelE2   :: Text
                            , nodeIdent,src,dst :: id
                            }
  deriving (Eq,Ord,Show)

data GremlinValue = GremlinNum Integer
                  | GremlinString Text
                  | GremlinList [GremlinValue]
                  | GremlinMap [(Text, GremlinValue)]
  deriving (Eq,Ord,Show)

instance A.ToJSON GremlinValue where
  toJSON gv =
    case gv of
      GremlinNum i    -> A.toJSON i
      x               -> A.toJSON (encodeGremlinValue x)

compile :: ([Node], [Edge]) -> IO [Operation Text]
compile (ns,es) =
  do let vertOfNodes = concatMap compileNode ns
     (concat -> vertOfEdges, concat -> edgeOfEdges) <- unzip <$> mapM compileEdge es
     pure $ concat [vertOfNodes , vertOfEdges , edgeOfEdges]

compileNode :: Node -> [Operation Text]
compileNode n = [InsertVertex ty (nodeUID_base64 n) props]
 where
  (ty,props) = propertiesAndTypeOf n

compileEdge :: Edge -> IO ([Operation Text], [Operation Text])
compileEdge e =
  do euid <- newUID
     let e1Lbl = vLbl <> " out"
         e2Lbl = vLbl <> " in"
         eNode   = uidToBase64 euid
         [esrc, edst] = map uidToBase64 [edgeSource e, edgeDestination e]
         fixCamelCase str =
           let go x | isUpper x = T.pack ['_' , x]
                    | otherwise = T.pack [C.toUpper x]
           in T.take 1 str <> T.concatMap go (T.drop 1 str)
         vLbl  = fixCamelCase $ T.pack $ show (edgeRelationship e)
         eTriple = InsertReifiedEdge vLbl e1Lbl e2Lbl eNode esrc edst
     return ([], [eTriple])

class PropertiesOf a where
  propertiesOf :: a -> [(Text,GremlinValue)]

class PropertiesAndTypeOf a where
  propertiesAndTypeOf :: a -> (Text,[(Text,GremlinValue)])

instance PropertiesAndTypeOf Node where
  propertiesAndTypeOf node =
   case node of
    NodeEntity entity     -> propertiesAndTypeOf entity
    NodeResource resource -> propertiesAndTypeOf resource
    NodeSubject subject   -> propertiesAndTypeOf subject
    NodeHost host         -> propertiesAndTypeOf host
    NodeAgent agent       -> propertiesAndTypeOf agent

enumOf :: Enum a => a -> GremlinValue
enumOf = GremlinNum . fromIntegral . fromEnum

mkSource :: InstrumentationSource -> (Text,GremlinValue)
mkSource = ("source",) . enumOf

mayAppend :: Maybe (Text,GremlinValue) -> [(Text,GremlinValue)] -> [(Text,GremlinValue)]
mayAppend Nothing  = id
mayAppend (Just x) = (x :)

instance PropertiesOf OptionalInfo where
  propertiesOf (Info {..}) =
        catMaybes [ ("time",) . gremlinTime <$> infoTime
                  , ("permissions",)  . gremlinNum <$> infoPermissions
          ] <> propertiesOf infoOtherProperties

instance PropertiesAndTypeOf Entity where
  propertiesAndTypeOf e =
   case e of
      File {..} -> ("Entity-File"
                   , mkSource entitySource
                     : ("url", GremlinString entityURL)
                     : ("file-version", gremlinNum entityFileVersion)
                     : mayAppend ( (("size",) . gremlinNum) <$> entityFileSize)
                                 (propertiesOf entityInfo)
                   )
      NetFlow {..} -> 
                ("Entity-NetFlow"
                , mkSource entitySource
                  : ("srcAddress", GremlinString entitySrcAddress)
                  : ("dstAddress", GremlinString entityDstAddress)
                  : ("srcPort", gremlinNum entitySrcPort)
                  : ("dstPort", gremlinNum entityDstPort)
                  : propertiesOf entityInfo
                )
      Memory {..} ->
               ("Entity-Memory"
               , mkSource entitySource
                 : maybe id (\p -> (("pageNumber", gremlinNum p):)) entityPageNumber
                 ( ("address", gremlinNum entityAddress)
                 : propertiesOf entityInfo
                 )
               )

instance PropertiesAndTypeOf Resource where
  propertiesAndTypeOf (Resource {..}) =
               ("Resource"
               , ("srcSinkType", enumOf resourceSource)
                 : propertiesOf resourceInfo
               )

instance PropertiesOf SubjectType where
  propertiesOf s = [("subjectType", gremlinNum (fromEnum s))]

instance PropertiesOf EventType where
  propertiesOf s = [("eventType", gremlinNum (fromEnum s))]

gremlinTime :: Int64 -> GremlinValue
gremlinTime = gremlinNum

gremlinList :: [Text] -> GremlinValue
gremlinList = GremlinList . map GremlinString

gremlinArgs :: [BS.ByteString] -> GremlinValue
gremlinArgs = gremlinList . map T.decodeUtf8

instance PropertiesOf a => PropertiesOf (Maybe a) where
  propertiesOf Nothing  = []
  propertiesOf (Just x) = propertiesOf x

instance PropertiesAndTypeOf Subject where
  propertiesAndTypeOf (Subject {..}) =
              ("Subject"
              , mkSource subjectSource
                : maybe id (\s -> (("startedAtTime", gremlinTime s) :)) subjectStartTime
                ( concat
                   [ propertiesOf subjectType
                   , propertiesOf subjectEventType
                   , F.toList (("sequence"   ,) . gremlinNum <$> subjectEventSequence)
                   , F.toList (("pid"        ,) . gremlinNum    <$> subjectPID        )
                   , F.toList (("ppid"       ,) . gremlinNum    <$> subjectPPID       )
                   , F.toList (("unitid"     ,) . gremlinNum    <$> subjectUnitID     )
                   , F.toList (("endedAtTime"    ,) . gremlinTime   <$> subjectEndTime    )
                   , F.toList (("commandLine",) . GremlinString <$> subjectCommandLine)
                   , F.toList (("importLibs" ,) . gremlinList   <$> subjectImportLibs )
                   , F.toList (("exportLibs" ,) . gremlinList   <$> subjectExportLibs )
                   , F.toList (("pInfo",) . GremlinString <$> subjectProcessInfo)
                   , F.toList (("location"   ,) . gremlinNum    <$> subjectLocation   )
                   , F.toList (("size"       ,) . gremlinNum    <$> subjectSize       )
                   , F.toList (("ppt"        ,) . GremlinString <$> subjectPpt        )
                   , F.toList (("env"        ,) . GremlinMap . propertiesOf  <$> subjectEnv)
                   , F.toList (("args"       ,) . gremlinArgs   <$> subjectArgs       )
                   , propertiesOf subjectOtherProperties
                   ])
               )

instance PropertiesOf (Map Text Text) where
  propertiesOf x
    | Map.null x = []
    | otherwise  = [("properties", GremlinMap (map (\(a,b) -> (a,GremlinString b)) (Map.toList x)))]

instance PropertiesAndTypeOf Host  where
  propertiesAndTypeOf (Host {..}) =
          ( "Host"
          , catMaybes [ mkSource <$> hostSource
                      , ("hostIP",) . GremlinString <$> hostIP
                      ]
          )

instance PropertiesAndTypeOf Agent where
  propertiesAndTypeOf (Agent {..}) =
      ("Agent"
      , ("userID", GremlinString agentUserID)
        : concat [ gidProps agentGID
                 , F.toList (("agentType",) . enumOf <$> agentType)
                 , F.toList (mkSource <$> agentSource)
                 , propertiesOf agentProperties
                 ]
      )

gidProps :: Maybe GID -> [(Text, GremlinValue)]
gidProps Nothing = []
gidProps (Just xs) = map (("gid",) . GremlinString) xs

nodeUID_base64 :: Node -> Text
nodeUID_base64 = uidToBase64 . nodeUID

uidToBase64 :: UID -> Text
uidToBase64 = T.decodeUtf8 . B64.encode . ByteString.toStrict . encode

newUID :: IO UID
newUID = (decode . ByteString.fromStrict) <$> getEntropy (8 * 4)

gremlinNum :: Integral i => i -> GremlinValue
gremlinNum = GremlinNum . fromIntegral

--------------------------------------------------------------------------------
--  Gremlin language serialization

class GraphId a where
  serializeOperation :: Operation a -> (Text,Env)

type Env = Map.Map Text A.Value

instance GraphId Text where
  serializeOperation (InsertVertex ty l ps) = (cmd,env)
    where
       cmd = escapeChars call
       -- g.addV(label, tyParam, 'ident', vertexName, param1, val1, param2, val2 ...)
       call = T.unwords
                [ "g.addV(label, tyParam, 'ident', l "
                , if (not (null ps)) then "," else ""
                , T.unwords $ intersperse "," (map mkParams [1..length ps])
                , ")"
                ]
       env = Map.fromList $ ("tyParam", A.String ty) : ("l", A.String l) : mkBinding ps
  serializeOperation (InsertReifiedEdge  lNode lE1 lE2 nId srcId dstId) = (cmd,env)
    where
    cmd = escapeChars call
    call = T.unwords
            [ "edgeNode = graph.addVertex(label, lNode, 'ident', nId) ; "
            , "g.V().has('ident',srcId).next().addEdge(lE1,edgeNode) ; "
            , "edgeNode.addEdge(lE2, g.V().has('ident', dstId).next())"
            ]
    env = Map.fromList [ ("lNode", A.String lNode)
                       , ("lE1", A.String lE1), ("lE2", A.String lE2)
                       , ("nId", A.String nId)
                       , ("srcId", A.String srcId), ("dstId", A.String dstId)
                       ]

  serializeOperation (InsertEdge l src dst genVerts)   =
     if genVerts
      then (testAndInsertCmd, env)
      else (nonTestCmd, env)
    where
      -- g.V().has('ident',src).next().addEdge(edgeTy, g.V().has('ident',dst).next(), param1, val1, ...)
      nonTestCmd =
        escapeChars $
            "g.V().has('ident',src).next().addEdge(edgeTy, g.V().has('ident',dst).next())"
      -- x = g.V().has('ident',src)
      -- y = g.V().has('ident',dst)
      -- if (!x.hasNext()) { x = g.addV('ident',src) }
      -- if (!y.hasNext()) { y = g.addV('ident',dst) }
      -- x.next().addEdge(edgeName, y.next())
      testAndInsertCmd = escapeChars $
             T.unwords
              [ "x = g.V().has('ident',src) ;"
              , "y = g.V().has('ident',dst) ;"
              , "if (!x.hasNext()) { x = g.addV('ident',src) } ;"
              , "if (!y.hasNext()) { y = g.addV('ident',dst) } ;"
              , "x.next().addEdge(edgeName, y.next())"
              ]
      env = Map.fromList [ ("src", A.String src)
                         , ("dst", A.String dst)
                         , ("edgeTy", A.String l)
                         ]

encodeQuoteText :: Text -> Text
encodeQuoteText = quote . subChars . escapeChars

encodeGremlinValue :: GremlinValue -> Text
encodeGremlinValue gv =
  case gv of
    GremlinString s -> escapeChars s
    GremlinNum  n   -> T.pack (show n)
    -- XXX maps and lists are only notionally supported
    GremlinMap xs   -> T.concat ["'["
                                 , T.concat (intersperse "," $ map renderKV xs)
                                 , "]'"
                                 ]
    GremlinList vs  -> T.concat ["'[ "
                                 , T.concat (intersperse "," $ map encodeGremlinValue vs)
                                 , " ]'"
                                 ]
  where renderKV (k,v) = encodeQuoteText k <> " : " <> encodeGremlinValue v

quote :: Text -> Text
quote b = T.concat ["\'", b, "\'"]

mkBinding :: [(Text, GremlinValue)] -> [(Text, A.Value)]
mkBinding pvs =
  let lbls = [ (param n, val n) | n <- [1..length pvs] ]
  in concat [ [(pstr, A.String p), (vstr, A.toJSON v)]
                    | (pstr,vstr) <- lbls
                    | (p,v) <- pvs ]

-- Build strin g"param1, val1, param2, val2, ..."
mkParams :: Int -> Text
mkParams n = T.concat [param n, ",", val n]

-- Construct the variable name for the nth parameter name.
param :: Int -> Text
param n = "param" <> T.pack (show n)

-- Construct the variable name for the Nth value
val :: Int -> Text
val n = "val" <> T.pack (show n)

escapeChars :: Text -> Text
escapeChars b
  | not (T.any (`Set.member` escSet) b) = b
  | otherwise = T.concatMap (\c -> if c `Set.member` escSet then T.pack ['\\', c] else T.singleton c) b

escSet :: Set.Set Char
escSet = Set.fromList ['\\', '"']

subChars :: Text -> Text
subChars b
  | not (T.any (`Set.member` badChars) b) = b
  | otherwise = T.map (\c -> maybe c id (Map.lookup c charRepl)) b

charRepl :: Map.Map Char Char
charRepl = Map.fromList [('\t',' ')]

badChars :: Set.Set Char
badChars = Map.keysSet charRepl
