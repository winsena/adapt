{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import PP as PP
import Ingest
import SimpleGetOpt
import qualified Namespaces as NS
import Types as T
import qualified Graph as G
import qualified RDF

import Control.Applicative ((<$>))
import Control.Monad (when)
import Control.Exception
import Data.Monoid ((<>))
import Data.Maybe (catMaybes,isNothing)
import Data.List (partition,intersperse)
import Data.Graph
import qualified Data.Map as Map
import           Data.Map (Map)
import Numeric (showHex)
import MonadLib        hiding (handle)
import MonadLib.Monads hiding (handle)
import qualified Data.Text.Lazy as Text
import qualified Data.Text.Lazy.IO as Text
import System.FilePath ((<.>))
import System.Exit (exitFailure)

import Titan
import Network.HTTP.Types

data Config = Config { lintOnly   :: Bool
                     , quiet      :: Bool
                     , graph      :: Bool
                     , stats      :: Bool
                     , turtle     :: Bool
                     , ast        :: Bool
                     , provn      :: Bool
                     , verbose    :: Bool
                     , help       :: Bool
                     , upload     :: Maybe ServerInfo
                     , files      :: [FilePath]
                     } deriving (Show)

defaultConfig :: Config
defaultConfig = Config
  { lintOnly = False
  , quiet    = False
  , graph    = False
  , stats    = False
  , turtle   = False
  , ast      = False
  , provn    = False
  , verbose  = False
  , help     = False
  , upload   = Nothing
  , files    = []
  }

opts :: OptSpec Config
opts = OptSpec { progDefaults  = defaultConfig
               , progParamDocs = [("FILES",      "The Prov-N files to be scanned.")]
               , progParams    = \p s -> Right s { files = p : files s }
               , progOptions   =
                  [ Option ['l'] ["lint"]
                    "Check the given file for syntactic and type issues."
                    $ NoArg $ \s -> Right s { lintOnly = True }
                  , Option ['q'] ["quiet"]
                    "Quiet linter warnings"
                    $ NoArg $ \s -> Right s { quiet = True }
                  , Option ['v'] ["verbose"]
                    "Verbose debugging messages"
                    $ NoArg $ \s -> Right s { verbose = True }
                  , Option ['a'] ["ast"]
                    "Produce a pretty-printed internal AST"
                    $ NoArg $ \s -> Right s { ast = True }
                  , Option ['p'] ["provn"]
                    "Produce a pretty-printed, reformatted, PROV representation"
                    $ NoArg $ \s -> Right s { provn = True }
                  , Option ['g'] ["graph"]
                    "Produce a dot file representing a graph of the conceptual model."
                    $ NoArg $ \s -> Right s { graph = True }
                  , Option ['s'] ["stats"]
                    "Print statistics."
                    $ NoArg $ \s -> Right s { stats = True }
                  , Option ['t'] ["turtle"]
                    "Produce a turtle RDF description of the graph."
                    $ NoArg $ \s -> Right s { turtle = True }
                  , Option ['u'] ["upload"]
                    "Uploads the data by inserting it into a Titan database using gremlin."
                    $ OptArg "Database host" $ \str s -> Right s { upload = Just $ maybe defaultServer ServerInfo str }
                  , Option ['h'] ["help"]
                    "Prints this help message."
                    $ NoArg $ \s -> Right s { help = True }
                  ]
               }

main :: IO ()
main =
  do c <- getOpts opts
     if help c
      then dumpUsage opts
      else mapM_ (trint c) (files c)

trint :: Config -> FilePath -> IO ()
trint c fp = do
  eres <- ingest
  case eres of
    Left e  -> do
      putStrLn $ "Error ingesting " ++ fp ++ ":"
      print (pp e)
    Right (pxs,res,ws) -> do
      unless (quiet c) $ printWarnings ws
      processStmts c fp res pxs

  where
  ingest = do
    t <- handle onError (Text.readFile fp)
    return (ingestText t)
  onError :: IOException -> IO a
  onError e = do putStrLn ("Error reading " ++ fp ++ ":")
                 print e
                 exitFailure

processStmts :: Config -> FilePath -> [Stmt] -> [Prefix] -> IO ()
processStmts c fp res pxs
  | lintOnly c = return ()
  | otherwise = do
      when (graph  c) $ do
        let dotfile =  fp <.> "dot"
        dbg ("Writing dot to " ++ dotfile)
        output dotfile (G.graph res)
      when (stats  c) $ do
        printStats res

      when (turtle c) $ do
        let ttlfile = fp <.> "ttl"
        dbg ("Writing turtle RDF to " ++ ttlfile)
        output ttlfile (RDF.turtle res)
      when (ast c) $ do
        let astfile = fp <.> "trint"
        dbg ("Writing ast to " ++ astfile)
        output astfile $ Text.unlines $ map (Text.pack . show) res
      when (provn c) $ do
        let provfile = fp <.> "trint.provn"
        dbg ("Writing prettified prov-n to " ++ provfile)
        output provfile (renderProv pxs res)
      maybe (return ()) (doUpload c res) (upload c)
 where
  dbg s = when (verbose c) (putStrLn s)
  output f t = handle (onError f) $ Text.writeFile f t
  onError :: String -> IOException -> IO ()
  onError f e = do putStrLn ("Error writing " ++ f ++ ":")
                   print e

printWarnings :: [Warning] -> IO ()
printWarnings ws = Text.putStrLn doc
  where doc = Text.unlines $ intersperse "\n" $ map (Text.pack . show . pp) ws

--------------------------------------------------------------------------------
--  Pretty Printing

renderProv :: [Prefix] -> [Stmt] -> Text
renderProv pfx0 stmts =
   let pfx    = trintns : provtc : pfx0
       header = map renderPrefix pfx
       ss     = map (renderStmt pfx) (zipWith nameUsed stmts [0..])
   in Text.unlines $ concatMap (map (Text.pack . show))
                     [[text "document"]
                     , header
                     , ss
                     , [text "endDocument"]]

-- Any used predicate that does not have a self-identifier is given an
-- arbitrary ident of 'trint:NUM'.
nameUsed :: Stmt -> Integer -> Stmt
nameUsed (StmtPredicate p) i =
      case (predType p,predIdent p) of
        (Used,Nothing) -> StmtPredicate (p { predIdent = Just (trintURI NS..: Text.pack (showHex i "")) })
        _              -> StmtPredicate p
nameUsed (StmtLoc (Located r p)) i = StmtLoc (Located r (nameUsed p i))
nameUsed x _ = x

renderPrefix :: Prefix -> Doc
renderPrefix (Prefix p u) = text "prefix " PP.<> pp p PP.<> text " <" PP.<> pp (show u) PP.<> text ">"

renderStmt :: [Prefix] -> Stmt -> Doc
renderStmt pfx stmt = pp (onIdent abbreviate stmt)
  where
  abbreviate (Qualified q x) = Qualified (shorthand q) x
  abbreviate x               = x

  shorthand :: Text -> Text
  shorthand t = maybe t id (Map.lookup t dict)

  dict :: Map Text Text
  dict = Map.fromList [ (Text.pack $ show fq,sh) | Prefix sh fq <- pfx]

-- | A fake Prov prefix for use with manufactured data such as generated 'used'
-- identifiers.
trintns :: Prefix
trintns = Prefix "trint" trintURI

trintURI :: NS.URI
trintURI = NS.perr "http://galois.com/adapt/trint"

-- | The prefix for ProvTC
provtc :: Prefix
provtc = Prefix "prov-tc" NS.adapt
--------------------------------------------------------------------------------
--  Database Upload

doUpload :: Config -> [Stmt] -> ServerInfo -> IO ()
doUpload c stmts svr =
 do stats <- push (es ++ ps)
    -- let err = filter ( (/= status200)) stats
    -- when (not $ quiet c) $ putStrLn $ unlines $ map show err
    return ()
  where
  (es,ps) = partition isEntity stmts

  isEntity (StmtEntity {})         = True
  isEntity (StmtPredicate {})      = False
  isEntity (StmtLoc (Located _ s)) = isEntity s

  push :: [Stmt] -> IO ()
  push = mapM_ (titan svr . translateInsert)

translateInsert :: Stmt -> Operation
translateInsert (StmtPredicate (Predicate {..})) =
    InsertEdge { label = pretty predType
               , src   = pretty predSubject
               , dst   = pretty predObject
               , properties = pa }
   where
    pa = map transPA predAttrs
    transPA p =
      case p of
        AtTime t             -> ("atTime"             , textOfTime t   )
        StartTime t          -> ("startTime"          , textOfTime t   )
        EndTime t            -> ("endTime"            , textOfTime t   )
        GenOp gop            -> ("genOp"              , gop )
        Permissions t        -> ("permissions"        , t   )
        ReturnVal t          -> ("returnVal"          , t   )
        Operation uop        -> ("operation"          , uop )
        Args t               -> ("args"               , t   )
        Cmd t                -> ("cmd"                , t   )
        DeriveOp dop         -> ("deriveOp"           , dop )
        ExecOp eop           -> ("execOp"             , eop )
        MachineID mid        -> ("machineID"          , mid )
        SourceAddress t      -> ("sourceAddress"      , t   )
        DestinationAddress t -> ("destinationAddress" , t   )
        SourcePort t         -> ("sourcePort"         , t   )
        DestinationPort t    -> ("destinationPort"    , t   )
        Protocol t           -> ("protocol"           , t   )
        Raw k v              -> (k,v) -- XXX warn

translateInsert (StmtEntity e)    =
  case e of
    Agent i as           -> InsertVertex (pretty i) (("vertexType", "agent") : (map translateAA as))
    UnitOfExecution i as -> InsertVertex (pretty i) (("vertexType", "unitOfExecution") : (map translateUOEA as))
    Artifact i as        -> InsertVertex (pretty i) (("vertexType", "artifact") : (map translateArA as))
    Resource i devty as  -> InsertVertex (pretty i) (("vertexType", "resource") : (translateDevId as))
  where
  translateAA as   =
   case as of
    AAName t      -> ("name", t)
    AAUser t      -> ("user", t)
    AAMachine mid -> ("machine", mid)

  translateUOEA as =
    case as of
        UAUser t        -> ("user"        , t)
        UAPID p         -> ("PID"         , p)
        UAPPID p        -> ("PPID"        , p)
        UAMachine mid   -> ("machine"     , mid)
        UAStarted t     -> ("started"     , textOfTime t)
        UAHadPrivs p    -> ("hadPrivs"    , p)
        UAPWD t         -> ("PWD"         , t)
        UAEnded t       -> ("ended"       , textOfTime t)
        UAGroup t       -> ("group"       , t)
        UACommandLine t -> ("commandLine" , t)
        UASource t      -> ("source"      , t)
        UAProgramName t -> ("programName" , t)
        UACWD t         -> ("CWD"         , t)
        UAUID t         -> ("UID"         , t)

  translateArA as  =
    case as of
      ArtAType at              -> ("type"               , at)
      ArtARegistryKey t        -> ("registryKey"        , t)
      ArtACoarseLoc cl         -> ("coarseLoc"          , cl)
      ArtAFineLoc fl           -> ("fineLoc"            , fl)
      ArtACreated t            -> ("created"            , textOfTime t)
      ArtAVersion v            -> ("version"            , v)
      ArtADeleted t            -> ("deleted"            , textOfTime t)
      ArtAOwner t              -> ("owner"              , t)
      ArtASize int             -> ("size"               , Text.pack $ show int)
      ArtADestinationAddress t -> ("destinationAddress" , t)
      ArtADestinationPort t    -> ("destinationPort"    , t)
      ArtASourceAddress t      -> ("sourceAddress"      , t)
      ArtASourcePort t         -> ("sourcePort"         , t)
      Taint w                  -> ("Taint"              , Text.pack $ show w)
  translateDevId Nothing   = []
  translateDevId (Just i)  = [("devId", i)]

translateInsert (StmtLoc (Located _ s)) = translateInsert s

textOfTime :: Time -> Text
textOfTime = Text.pack . show -- XXX


--------------------------------------------------------------------------------
--  Statistics

printStats :: [Stmt] -> IO ()
printStats ss =
  do let g  = mkGraph ss
         vs = vertices g
         mn = length (take 1 ss) -- one node suggests the minimum subgraph is one.
         sz = min nrStmt $ maximum (mn : map (length . reachable g) vs) -- XXX O(n^2) algorithm!
         nrStmt = length ss
     putStrLn $ "Largest subgraph is: " ++ show sz
     putStrLn $ "\tEntities:         " ++ show (min (length vs) nrStmt)
     putStrLn $ "\tPredicates:       " ++ show (nrStmt - length vs)
     putStrLn $ "\tTotal statements: " ++ show nrStmt


--------------------------------------------------------------------------------
--  Graphing

data NodeInfo = Node Int | Edge (Int,Int)

-- Create a graph as an array of edges with nodes represented by Int.
mkGraph :: [Stmt] -> Graph
mkGraph ss =
  let (edges,(_,maxNodes)) = runState (Map.empty, 0) $ catMaybes <$> mapM mkEdge ss
  in buildG (0,maxNodes) edges

-- Memoizing edge creation
mkEdge :: Stmt -> State (Map Text Vertex,Vertex) (Maybe Edge)
mkEdge (StmtPredicate (Predicate s o _ _ _)) =
  do nS <- nodeOf (pretty s)
     nO <- nodeOf (pretty o)
     return $ Just (nS,nO)
mkEdge (StmtLoc (T.Located _ s)) = mkEdge s
mkEdge (StmtEntity {})         = return Nothing

-- Memoizing node numbering
nodeOf :: Text -> State (Map Text Vertex, Vertex) Vertex
nodeOf name =
  do (m,v) <- get
     case Map.lookup name m of
      Nothing -> do set (Map.insert name v m, v+1)
                    return v
      Just n  -> return n

