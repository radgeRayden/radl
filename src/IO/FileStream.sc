using import Buffer C.stdio enum slice String struct

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
        fseek self._handle (position as i64) SEEK_SET
        ()

    fn rewind (self)
        rewind self._handle
        ()

    fn read-elements (self container)
        start-position := 'tell self
        ptr count := 'data container
        element-size := sizeof (elementof (typeof ptr))
        elements-read := fread (ptr as voidstar) element-size count self._handle
        end-position := 'tell self

        if (elements-read < count)
            if (not ('eof? self))
                raise FileError.ReadError

            # For larger elements, incomplete reads may occur. According to POSIX `fread` returns
            # the number of elements successfully read, so our data won't be corrupted. However we
            # should set the cursor back to the end of the last element read.
            bytes-written := elements-read * element-size
            expected-end := start-position + bytes-written
            if (expected-end < end-position)
                'seek-absolute self expected-end

        slice container 0 elements-read

    fn read-bytes (self count)
        'read-elements self (heapbuffer u8 count)

    fn read-string (self count)
        local result : String count
        'resize result count
        trim ('read-elements self result)

    fn read-all-bytes (self)
        'seek-absolute self 0
        'read-bytes self (countof self)

    fn read-all-string (self)
        'seek-absolute self 0
        'read-string self (countof self)

    fn read-line (self)
        local result : String
        chunk := heapbuffer i8 1024

        while (not ('eof? self))
            ptr chunk-size := 'data chunk
            bytes-read := fread ptr 1 chunk-size self._handle

            if (bytes-read < chunk-size)
                if (not ('eof? self))
                    raise FileError.ReadError

            chunk := (lslice (view chunk) bytes-read)
            for i c in (enumerate chunk)
                if (c == "\n")
                    'seek self (((i + 1) as i64) - (bytes-read as i64))
                    'append result (lslice chunk i)
                    return result
                elseif (c == "\r")
                    local next-byte : i8
                    if (i == (countof chunk))
                        fread (&next-byte as voidstar) 1 1 self._handle
                    else
                        next-byte = chunk @ (i + 1)

                    bytes-read as:= i64
                    if (next-byte == "\n")
                        'seek self (((i + 2) as i64) - bytes-read)
                    else
                        'seek self (((i + 1) as i64) - bytes-read)

                    'append result (lslice chunk i)
                    return result

            'append result chunk

        result

    inline lines (self)
        Generator
            inline start ()
                'rewind self
                view self
            inline valid? (self)
                not ('eof? self)
            inline current (self)
                'read-line self
            inline next (self)
                view self

    fn eof? (self)
        (feof self._handle) as bool

    fn write (self data)
        viewing data
        ptr count := 'data data
        element-size := sizeof (elementof (typeof ptr))
        elements-written := fwrite ptr element-size count self._handle
        if (elements-written < count)
            raise FileError.WriteError

do
    let FileStream FileMode FileError
    locals;
