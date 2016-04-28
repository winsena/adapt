module CommonDataModel.Types where

import Data.ByteString (ByteString)
import Data.Int
import Data.Text (Text)
import Data.Map
import Data.Word (Word16)

-- | A two byte value we keep as 16 bits in little endian.
newtype Short = Short { unShort :: Word16 }
  deriving (Eq,Ord,Show,Read)

-- | UUIDs are 256 bit (32 byte) values.
newtype UUID = UUID ByteString
  deriving (Eq,Ord,Show,Read)

data SubjectType = Process | Thread | Unit | BasicBlock
  deriving (Eq, Ord, Show, Enum, Bounded)

data SrcSinkType
        = SOURCE_ACCELEROMETER
        | SOURCE_TEMPERATURE
        | SOURCE_GYROSCOPE
        | SOURCE_MAGNETIC_FIELD
        | SOURCE_HEART_RATE
        | SOURCE_LIGHT
        | SOURCE_PROXIMITY
        | SOURCE_PRESSURE
        | SOURCE_RELATIVE_HUMIDITY
        -- composite sensors, sources
        | SOURCE_LINEAR_ACCELERATION
        | SOURCE_MOTION
        | SOURCE_STEP_DETECTOR
        | SOURCE_STEP_COUNTER
        | SOURCE_TILT_DETECTOR
        | SOURCE_ROTATION_VECTOR
        | SOURCE_GRAVITY
        | SOURCE_GEOMAGNETIC_ROTATION_VECTOR
        -- camera and GPS sources, temporary
        | SOURCE_CAMERA
        | SOURCE_GPS
        | SOURCE_AUDIO
        -- Environment variables
        | SOURCE_SYSTEM_PROPERTY
        -- IPC should only be used for internal IPC instead of network flows
        -- ClearScope might be using this in the interim for flows
        -- Can be a source or a sink
        | SOURCE_SINK_IPC
        | SOURCE_UNKNOWN -- ideally, this should never be used
  deriving (Eq, Ord, Enum, Bounded, Show)

data InstrumentationSource
        = SOURCE_LINUX_AUDIT_TRACE
        | SOURCE_LINUX_PROC_TRACE
        | SOURCE_LINUX_BEEP_TRACE
        | SOURCE_FREEBSD_OPENBSM_TRACE
        | SOURCE_ANDROID_JAVA_CLEARSCOPE
        | SOURCE_ANDROID_NATIVE_CLEARSCOPE
        | SOURCE_FREEBSD_DTRACE_CADETS
        | SOURCE_FREEBSD_TESLA_CADETS
        | SOURCE_FREEBSD_LOOM_CADETS
        | SOURCE_FREEBSD_MACIF_CADETS
        | SOURCE_WINDOWS_DIFT_FAROS
        | SOURCE_LINUX_THEIA
      deriving (Eq, Ord, Show, Read, Enum, Bounded)

data PrincipalType = PRINCIPAL_LOCAL | PRINCIPAL_REMOTE
  deriving (Eq, Ord, Enum, Bounded, Show)

data EventType
        = EVENT_ACCEPT
        | EVENT_BIND
        | EVENT_CHANGE_PRINCIPAL
        | EVENT_CHECK_FILE_ATTRIBUTES
        | EVENT_CLONE
        | EVENT_CLOSE
        | EVENT_CONNECT
        | EVENT_CREATE_OBJECT
        | EVENT_CREATE_THREAD
        | EVENT_EXECUTE
        | EVENT_FORK
        | EVENT_LINK
        | EVENT_UNLINK
        | EVENT_MMAP
        | EVENT_MODIFY_FILE_ATTRIBUTES
        | EVENT_MPROTECT
        | EVENT_OPEN
        | EVENT_READ
        | EVENT_RECVFROM
        | EVENT_RECVMSG
        | EVENT_RENAME
        | EVENT_WRITE
        | EVENT_SIGNAL
        | EVENT_TRUNCATE
        | EVENT_WAIT
        | EVENT_OS_UNKNOWN
        | EVENT_KERNEL_UNKNOWN
        | EVENT_APP_UNKNOWN
        | EVENT_UI_UNKNOWN
        | EVENT_UNKNOWN
        | EVENT_BLIND
        | EVENT_UNIT
        | EVENT_UPDATE
  deriving (Eq, Ord, Enum, Bounded, Show)

data TagEntity =
  TagEntity { teUUID       :: UUID
            , tePTN        :: ProvenanceTagNode
            , teProperties :: Maybe Properties
            }
         deriving (Eq,Ord,Show)

data EdgeType
        = EDGE_EVENT_AFFECTS_MEMORY
        | EDGE_EVENT_AFFECTS_FILE
        | EDGE_EVENT_AFFECTS_NETFLOW
        | EDGE_EVENT_AFFECTS_SUBJECT
        | EDGE_EVENT_AFFECTS_SRCSINK
        | EDGE_EVENT_HASPARENT_EVENT
        | EDGE_EVENT_CAUSES_EVENT
        | EDGE_EVENT_ISGENERATEDBY_SUBJECT
        | EDGE_SUBJECT_AFFECTS_EVENT
        | EDGE_SUBJECT_HASPARENT_SUBJECT
        | EDGE_SUBJECT_HASLOCALPRINCIPAL
        | EDGE_SUBJECT_RUNSON
        | EDGE_FILE_AFFECTS_EVENT
        | EDGE_NETFLOW_AFFECTS_EVENT
        | EDGE_MEMORY_AFFECTS_EVENT
        | EDGE_SRCSINK_AFFECTS_EVENT
        | EDGE_OBJECT_PREV_VERSION
        | EDGE_FILE_HAS_TAG
        | EDGE_NETFLOW_HAS_TAG
        | EDGE_MEMORY_HAS_TAG
        | EDGE_SRCSINK_HAS_TAG
        | EDGE_SUBJECT_HAS_TAG
        | EDGE_EVENT_HAS_TAG
  deriving (Eq, Ord, Enum, Bounded, Show)

data LocalAuthType
        = LOCALAUTH_NONE
        | LOCALAUTH_PASSWORD
        | LOCALAUTH_PUBLIC_KEY
        | LOCALAUTH_ONE_TIME_PASSWORD
  deriving (Eq, Ord, Enum, Bounded, Show)

data TagOpCode
        = TAG_OP_SEQUENCE
        | TAG_OP_UNION
        | TAG_OP_ENCODE
        | TAG_OP_STRONG
        | TAG_OP_MEDIUM
        | TAG_OP_WEAK
  deriving (Eq, Ord, Enum, Bounded, Show)

data IntegrityTag
        = INTEGRITY_UNTRUSTED
        | INTEGRITY_BENIGN
        | INTEGRITY_INVULNERABLE
  deriving (Eq, Ord, Enum, Bounded, Show)

data ConfidentialityTag
        = CONFIDENTIALITY_SECRET
        | CONFIDENTIALITY_SENSITIVE
        | CONFIDENTIALITY_PRIVATE
        | CONFIDENTIALITY_PUBLIC
  deriving (Eq, Ord, Enum, Bounded, Show)

data ProvenanceTagNode
    = PTN { ptnValue    :: PTValue
          , ptnChildren :: Maybe [ProvenanceTagNode]
          , ptnId       :: Maybe TagId
          , ptnProperties  :: Maybe Properties
          }
     deriving (Eq,Ord,Show)

type Properties = Map Text Text -- XXX map to dynamic?

type TagId = Int32

data PTValue = PTVInt Int64
             | PTVTagOpCode  TagOpCode
             | PTVIntegrityTag IntegrityTag
             | PTVConfidentialityTag ConfidentialityTag
     deriving (Eq,Ord,Show)

data Value = Value { valSize      :: Int32
                   , valType      :: ValueType
                   , valDataType  :: Maybe Text
                   , valBytes     :: Maybe ByteString
                   , valTags      :: Maybe [Int32] -- XXX Run Length and ID pairs
                   , valComponents :: Maybe [Value]
                   }
     deriving (Eq,Ord,Show)

data ValueType = TypeIn | TypeOut | TypeInOut
  deriving (Eq,Ord,Show,Enum,Bounded)

data Subject =
  Subject { subjUUID                 :: UUID
          , subjType                 :: SubjectType
          , subjPID                  :: Int32
          , subjPPID                 :: Int32
          , subjSource               :: InstrumentationSource
          , subjStartTimestampMicros :: Maybe Int64 -- Unix Epoch
          , subjUnitId               :: Maybe Int32
          , subjEndTimestampMicros   :: Maybe Int64
          , subjCmdLine              :: Maybe Text
          , subjImportedLibraries    :: Maybe [Text]
          , subjExportedLibraries    :: Maybe [Text]
          , subjPInfo                :: Maybe Text
          , subjProperties           :: Maybe Properties
          }
     deriving (Eq,Ord,Show)

data Event =
  Event { evtUUID               :: UUID
        , evtSequence           :: Int64
        , evtType               :: EventType
        , evtThreadId           :: Int32
        , evtSource             :: InstrumentationSource
        , evtTimestampMicros    :: Maybe Int64
        , evtName               :: Maybe Text
        , evtParameters         :: Maybe [Value]
        , evtLocation           :: Maybe Int64
        , evtSize               :: Maybe Int64
        , evtProgramPoint       :: Maybe Text
        , evtProperties         :: Maybe Properties
        }
     deriving (Eq,Ord,Show)

data AbstractObject =
  AbstractObject { aoSource              :: InstrumentationSource
                 , aoPermission          :: Maybe Short
                 , aoLastTimestampMicros :: Maybe Int64
                 , aoProperties          :: Maybe Properties
                 }
     deriving (Eq,Ord,Show)

data FileObject =
  FileObject  { foUUID       :: UUID
              , foBaseObject :: AbstractObject
              , foURL        :: Text
              , foIsPipe     :: Bool
              , foVersion    :: Int32
              , foSize       :: Maybe Int64
              }
     deriving (Eq,Ord,Show)

data NetFlowObject =
  NetFlowObject { nfUUID        :: UUID
                , nfBaseObject  :: AbstractObject
                , nfSrcAddress  :: Text
                , nfSrcPort     :: Int32
                , nfDstAddress  :: Text
                , nfDstPort     :: Int32
                }
     deriving (Eq,Ord,Show)

data MemoryObject =
  MemoryObject { moUUID          :: UUID
               , moBaseObject    :: AbstractObject
               , moPageNumber    :: Maybe Int64
               , moMemoryAddress :: Int64
               }
     deriving (Eq,Ord,Show)

data SrcSinkObject =
  SrcSinkObject { ssUUID        :: UUID
                , ssBaseObject  :: AbstractObject
                , ssType        :: SrcSinkType
                }
     deriving (Eq,Ord,Show)

data Principal =
  Principal { pUUID     :: UUID
            , pType     :: PrincipalType
            , pUserId   :: Int32
            , pGroupIds :: [Int32]
            , pSource   :: InstrumentationSource
            , pProperties :: Maybe Properties
            }
     deriving (Eq,Ord,Show)

data SimpleEdge =
  SimpleEdge { fromUUID         :: UUID
             , toUUID           :: UUID
             , edgeType         :: EdgeType
             , timestamp        :: Int64
             , edgeProperties   :: Maybe Properties
             }
     deriving (Eq,Ord,Show)

data TCCDMDatum
        = DatumPTN ProvenanceTagNode
        | DatumSub Subject
        | DatumEve Event
        | DatumNet NetFlowObject
        | DatumFil FileObject
        | DatumSrc SrcSinkObject
        | DatumMem MemoryObject
        | DatumPri Principal
        | DatumTag TagEntity
        | DatumSim SimpleEdge
      deriving (Eq,Ord,Show)