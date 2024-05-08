using import Buffer enum slice ..strfmt String struct

module-setup API ContextType

using
    static-if default-import?
        import .posix-files
    else
        API

let ContextType =
    static-if (none? ContextType)
        storageof none
    else ContextType

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

struct FileStream
    _handle  : FileHandle
    _mode    : FileMode
    _path    : String

    inline __typecall (cls path mode)
        handle := open-file path mode
        if (is-handle-invalid? handle)
            log-error f"Could not open file ${path}"
            raise FileError.NotAccessible

        super-type.__typecall cls
            _handle = handle
            _mode = mode

    inline __drop (self)
        close-file self._handle

    fn __countof (self)
        get-file-length self._handle

    fn tell (self)
        get-cursor-position self._handle

    fn seek (self offset)
        set-cursor-position self._handle ((('tell self) as i64) + offset)

    fn seek-absolute (self position)
        set-cursor-position self._handle position

    fn rewind (self)
        set-cursor-position self._handle 0

    fn read-elements (self container)
        ptr count := 'data container
        element-size := sizeof (elementof (typeof ptr))
        expected-bytes := count * element-size

        start-position := 'tell self
        bytes-read := read-bytes self._handle ptr expected-bytes
        elements-read := bytes-read // element-size

        if (bytes-read < expected-bytes)
            if (not ('eof? self))
                log-error f"Error reading file ${self._path}"
                raise FileError.ReadError

            # For larger elements, incomplete reads may occur.
            # We should set the cursor back to the end of the last element read.
            expected-advance := elements-read * element-size
            if (expected-advance < bytes-read)
                'seek-absolute self (start-position + expected-advance)

        lslice container elements-read

    fn read-bytes (self count)
        'read-elements self (heapbuffer u8 count)

    fn read-string (self count)
        local result = String count
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
            chunk := 'read-elements self (view chunk)
            bytes-read := countof chunk
            for i c in (enumerate chunk)
                if (c == "\n")
                    'seek self (((i + 1) as i64) - (bytes-read as i64))
                    'append result (lslice chunk i)
                    return result
                elseif (c == "\r")
                    local next-byte : i8
                    if (i == (countof chunk))
                        'read-elements self ((ViewBuffer (mutable@ i8)) &next-byte 1)
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
        eof? self._handle

    fn write (self data)
        viewing data
        ptr count := 'data data
        element-size := sizeof (elementof (typeof ptr))
        bytes-written := write-bytes self._handle ptr (count * element-size)
        elements-written := bytes-written // element-size
        if (elements-written < count)
            log-error f"Error writing to file ${self._path}"
            raise FileError.WriteError

do
    let FileStream FileMode FileError
    locals;
