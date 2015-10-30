{-# LANGUAGE OverloadedStrings #-}
-- | Trint is a lint-like tool for the Prov-N transparent computing language.
module Main where

import Ingest
import SimpleGetOpt
import Types as T
import qualified Graph as G
import qualified RDF

import Control.Applicative ((<$>))
import Control.Monad (when)
import Control.Exception
import Data.Monoid ((<>))
import Data.Maybe (catMaybes)
import Data.List (intersperse)
import Data.Graph
import qualified Data.Map as Map
import           Data.Map (Map)
import MonadLib        hiding (handle)
import MonadLib.Monads hiding (handle)
import qualified Data.Text.Lazy as L
import qualified Data.Text.Lazy.IO as L
import System.FilePath ((<.>))

data Config = Config { lintOnly   :: Bool
                     , quiet      :: Bool
                     , graph      :: Bool
                     , stats      :: Bool
                     , turtle     :: Bool
                     , ast        :: Bool
                     , verbose    :: Bool
                     , files      :: [FilePath]
                     }

defaultConfig :: Config
defaultConfig = Config
  { lintOnly = False
  , quiet    = False
  , graph    = False
  , stats    = False
  , turtle   = False
  , ast      = False
  , verbose  = False
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
                  , Option ['g'] ["graph"]
                    "Produce a dot file representing a graph of the conceptual model."
                    $ NoArg $ \s -> Right s { graph = True }
                  , Option ['s'] ["stats"]
                    "Print statistics."
                    $ NoArg $ \s -> Right s { stats = True }
                  , Option ['t'] ["turtle"]
                    "Produce a turtle RDF description of the graph."
                    $ NoArg $ \s -> Right s { turtle = True }
                  ]
               }

main :: IO ()
main =
  do c <- getOpts opts
     mapM_ (trint c) (files c)

trint :: Config -> FilePath -> IO ()
trint c fp =
  do eres <- ingestText <$> L.readFile fp
     case eres of
      Left e            -> L.putStrLn $ "Error: " <> (L.pack $ show e)
      Right (res,ws)    ->
          do unless (quiet c) $ printWarnings ws
             doRest c fp res

doRest :: Config -> FilePath -> [Stmt] -> IO ()
doRest c fp res
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
        output astfile $ unlines $ map show res
        dbg "done"
  where
  dbg s = when (verbose c) (putStrLn s)
  output f s = handle (onError f) $ writeFile f s
  onError :: String -> IOException -> IO ()
  onError f e = do putStrLn ("Error writing " ++ f ++ ":")
                   print e


printWarnings :: [Warning] -> IO ()
printWarnings ws =
  do let doc = L.unlines $ intersperse "\n" $ map ppWarning ws
     L.putStrLn doc

printStats :: [Stmt] -> IO ()
printStats ss =
  do let g  = mkGraph ss
         vs = vertices g
         mn = length (take 1 ss) -- one node suggests the minimum subgraph is one.
         sz = maximum (mn : map (length . reachable g) vs) -- XXX O(n^2) algorithm!
     putStrLn $ "Largest subgraph is: " ++ show sz
     putStrLn $ "\tEntities:         " ++ show (length vs)
     putStrLn $ "\tPredicates:       " ++ show (length ss - length vs)
     putStrLn $ "\tTotal statements: " ++ show (length ss)

data NodeInfo = Node Int | Edge (Int,Int)

-- Create a graph as an array of edges with nodes represented by Int.
mkGraph :: [Stmt] -> Graph
mkGraph ss =
  let (edges,(_,maxNodes)) = runState (Map.empty, 0) $ catMaybes <$> mapM mkEdge ss
  in buildG (0,maxNodes) edges

-- Memoizing edge creation
mkEdge :: Stmt -> State (Map Text Vertex,Vertex) (Maybe Edge)
mkEdge (StmtPredicate (Predicate s o _ _)) =
  do nS <- nodeOf s
     nO <- nodeOf o
     return $ Just (nS,nO)
mkEdge _ = return Nothing

-- Memoizing node numbering
nodeOf :: Text -> State (Map Text Vertex, Vertex) Vertex
nodeOf name =
  do (m,v) <- get
     case Map.lookup name m of
      Nothing -> do set (Map.insert name v m, v+1)
                    return v
      Just n  -> return n
