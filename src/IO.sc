using import Array
using import C.stdio
using import enum
using import String
using import struct

enum FileError plain
    NotAccessible
    ReadError
    WriteError

enum FileMode plain
    Read
    Write
    ReadWrite
    Append
    Update

    inline get-mode-string (self)
        T := this-type

        switch self
        case T.Read
            "rb\x00"
        case T.Write
            "wb\x00"
        case T.ReadWrite
            "wb+"
        case T.Append
            "ab\x00"
        case T.Update
            "ab+"
        default
            assert false "invalid enum value"

struct FileStream
    _handle : (mutable@ FILE)
    _mode   : FileMode
    _path   : String

    inline __typecall (cls path mode)
        handle := fopen path ('get-mode-string mode)
        if (handle == null)
            raise FileError.NotAccessible

        super-type.__typecall cls
            _handle = handle
            _mode = mode

    inline __drop (self)
        fclose self._handle
        ()

    fn __countof (self)
        cursor := ftell self._handle
        fseek self._handle 0 SEEK_END
        size := ftell self._handle
        fseek self._handle cursor SEEK_SET

        size as usize

    fn tell (self)
        (ftell self._handle) as usize

    fn seek (self offset)
        fseek self._handle offset SEEK_CUR
        ()

    fn seek-absolute (self position)
        fseek self._handle position SEEK_SET
        ()

    fn rewind (self)
        rewind self._handle
        ()

    inline read-elements (self count arrT)
        local result : arrT
        'resize result count

        element-size := sizeof arrT.ElementType
        ptr := (imply result pointer) as voidstar
        elements-read := fread ptr element-size count self._handle

        if (elements-read < count)
            if (not ('eof? self))
                raise FileError.ReadError

        # resizing downwards is non destructive
        'resize result elements-read
        result

    fn read-bytes (self count)
        'read-elements self count (Array u8)

    fn read-string (self count)
        'read-elements self count String

    fn read-all-bytes (self)
        'seek-absolute self 0
        'read-bytes self (countof self)

    fn read-all-string (self)
        'seek-absolute self 0
        'read-string self (countof self)

    fn read-line (self)
        local result : String
        local chunk : String

        chunk-size := 1024

        while (not ('eof? self))
            'resize chunk chunk-size
            ptr := (imply chunk pointer) as voidstar
            bytes-read := fread ptr 1 chunk-size self._handle

            if (bytes-read < chunk-size)
                if (not ('eof? self))
                    raise FileError.ReadError
                else
                    'resize chunk bytes-read
                    return (result .. chunk)

            for i c in (enumerate chunk)
                if (c == "\n")
                    'resize chunk i
                    'seek self (((i + 1) as i64) - chunk-size)
                    return (result .. chunk)
                elseif (c == "\r")
                    local next-byte : i8
                    if (i == (countof chunk))
                        fread (&next-byte as voidstar) 1 1 self._handle
                    else
                        next-byte = chunk @ (i + 1)

                    if (next-byte == "\n")
                        'seek self (((i + 2) as i64) - chunk-size)
                    else
                        'seek self (((i + 1) as i64) - chunk-size)

                    'resize chunk i
                    return (result .. chunk)

            result ..= chunk

        result

    inline lines (self)
        Generator
            inline start ()
                'rewind self
                self
            inline valid? (self)
                not ('eof? self)
            inline current (self)
                'read-line self
            inline next (self)
                self

    fn eof? (self)
        (feof self._handle) as bool

    fn... write (self, data : Array, offset : usize, count : usize)
        ptr := (& (data @ offset)) as voidstar
        element-size := sizeof ((typeof data) . ElementType)
        elements-written := fwrite ptr element-size count
        if (elements-written < count)
            raise FileError.WriteError
    case (self, data : Array)
        this-function self data 0 (countof data)

do
    let FileStream FileMode FileError
    locals;
