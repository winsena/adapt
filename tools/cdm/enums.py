from enum import Enum


class Instrumentationsource(Enum):
    LINUX_AUDIT_TRACE = 0
    LINUX_PROC_TRACE = 1
    LINUX_BEEP_TRACE = 2
    FREEBSD_OPENBSM_TRACE = 3
    ANDROID_JAVA_CLEARSCOPE = 4
    ANDROID_NATIVE_CLEARSCOPE = 5
    FREEBSD_DTRACE_CADETS = 6
    FREEBSD_TESLA_CADETS = 7
    FREEBSD_LOOM_CADETS = 8
    FREEBSD_MACIF_CADETS = 9
    WINDOWS_DIFT_FAROS = 10
    LINUX_THEIA = 11
    WINDOWS_FIVEDIRECTIONS = 12


class Principal(Enum):
    LOCAL = 0
    REMOTE = 1


class Event(Enum):
    ACCEPT = 0
    BIND = 1
    CHANGE_PRINCIPAL = 2
    CHECK_FILE_ATTRIBUTES = 3
    CLONE = 4
    CLOSE = 5
    CONNECT = 6
    CREATE_OBJECT = 7
    CREATE_THREAD = 8
    EXECUTE = 9
    FORK = 10
    LINK = 11
    UNLINK = 12
    MMAP = 13
    MODIFY_FILE_ATTRIBUTES = 14
    MPROTECT = 15
    OPEN = 16
    READ = 17
    RECVFROM = 18
    RECVMSG = 19
    RENAME = 20
    WRITE = 21
    SIGNAL = 22
    TRUNCATE = 23
    WAIT = 24
    OS_UNKNOWN = 25
    KERNEL_UNKNOWN = 26
    APP_UNKNOWN = 27
    UI_UNKNOWN = 28
    UNKNOWN = 29
    BLIND = 30
    UNIT = 31
    UPDATE = 32
    SENDTO = 33
    SENDMSG = 34
    SHM = 35
    EXIT = 36


class Source(Enum):
    ACCELEROMETER = 0
    TEMPERATURE = 1
    GYROSCOPE = 2
    MAGNETIC_FIELD = 3
    HEART_RATE = 4
    LIGHT = 5
    PROXIMITY = 6
    PRESSURE = 7
    RELATIVE_HUMIDITY = 8
    LINEAR_ACCELERATION = 9
    MOTION = 10
    STEP_DETECTOR = 11
    STEP_COUNTER = 12
    TILT_DETECTOR = 13
    ROTATION_VECTOR = 14
    GRAVITY = 15
    GEOMAGNETIC_ROTATION_VECTOR = 16
    CAMERA = 17
    GPS = 18
    AUDIO = 19
    SYSTEM_PROPERTY = 20
    ENV_VARIABLE = 21
    SINK_IPC = 22
    UNKNOWN = 23


class Integritytag(Enum):
    UNTRUSTED = 0
    BENIGN = 1
    INVULNERABLE = 2


class Confidentialitytag(Enum):
    SECRET = 0
    SENSITIVE = 1
    PRIVATE = 2
    PUBLIC = 3


class Subject(Enum):
    PROCESS = 0
    THREAD = 1
    UNIT = 2
    BASIC_BLOCK = 3
