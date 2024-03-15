using import C.stdio

import C.bindings
from C.bindings.extern let feof

inline get-mode-string (value)
    T := (typeof value)

    switch value
    case T.Read
        "rb"
    case T.Write
        "wb"
    case T.ReadWrite
        "wb+"
    case T.Append
        "ab"
    case T.Update
        "ab+"
    default
        assert false "invalid enum value"

do
    FileHandle := (mutable@ FILE)

    fn open-file (path mode)
        fopen path (get-mode-string mode)

    fn close-file (handle)
        fclose handle
        ()

    inline is-handle-invalid? (handle)
        handle == null

    fn get-file-length (handle)
        cursor := ftell handle
        fseek handle 0 SEEK_END
        size := ftell handle
        fseek handle cursor SEEK_SET
        size as usize

    fn get-cursor-position (handle)
        (ftell handle) as usize

    fn set-cursor-position (handle position)
        fseek handle (position as i64) SEEK_SET
        ()

    fn read-bytes (handle ptr count)
        fread (ptr as voidstar) 1 count handle

    fn write-bytes (handle ptr count)
        fwrite ptr 1 count handle

    fn eof? (handle)
        (feof handle) as bool

    fn log-error (msg)
        printf "%s\n" (msg as rawstring)

    local-scope;
