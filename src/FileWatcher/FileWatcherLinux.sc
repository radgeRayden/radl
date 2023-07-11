using import Array
using import enum
using import Map
using import Rc
using import String
using import struct
import C.errno

using import ..ext
using import .headers

enum FileEventType plain
    Modified

enum FileWatchError plain
    NotFound
    AccessDenied
    NotAFile
    NotWatching

FileWatchCallback := (@ (function void (viewof String 1) FileEventType))
type WatchDescriptor < (opaque "InotifyWatchDescriptor") :: i32
    inline __rimply (otherT thisT)
        static-if (otherT == (storageof thisT))
            inline (incoming)
                bitcast incoming thisT

struct WatchedFile
    path : String
    descriptor : (Rc WatchDescriptor)
    callback : FileWatchCallback

struct FileWatcher
    _handle : i32
    watched-dirs : (Map String (Rc WatchDescriptor))
    watched-files : (Map String WatchedFile)

    inline __typecall (cls)
        handle := (inotify.init1 inotify.IN_NONBLOCK)
        assert (handle != -1)

        super-type.__typecall cls
            _handle = handle

    fn... watch (self, path : String, callback : FileWatchCallback)
        # NOTE: dirname may segfault if given read only memory. Currently String always has a heap backing,
        # but this may not be true in the future for serialized constant Strings.
        local path = copy path
        dir file := 'from-rawstring String (libgen.dirname path), 'from-rawstring String (libgen.basename path)
        if (file == "")
            raise FileWatchError.NotAFile

        using inotify filter "^IN_"
        mask := | IN_ONLYDIR IN_CREATE IN_MODIFY
        let wd =
            try
                copy ('get self.watched-dirs dir)
            else
                wd := inotify.add_watch self._handle dir mask
                if (wd == -1)
                    using C.errno
                    switch (errno)
                    case EACCES
                        raise FileWatchError.AccessDenied
                    case ENOENT
                        raise FileWatchError.NotFound
                    case ENOTDIR
                        raise FileWatchError.NotFound
                    default
                        using import ..strfmt
                        assert false f"Unhandled error while trying to watch file: ${path}"
                Rc.wrap (imply wd WatchDescriptor)


        'set self.watched-files (copy path)
            WatchedFile (copy path) (copy wd) callback
        'set self.watched-dirs (copy path) (copy wd)
        ()

    fn... unwatch (self, path : String)
        local path = copy path
        dir file := 'from-rawstring String (libgen.dirname path), 'from-rawstring String (libgen.basename path)
        if (file == "")
            raise FileWatchError.NotAFile

        let wd =
            try ('get self.watched-dirs dir)
            else (raise FileWatchError.NotWatching)

        if (('strong-count wd) == 1)
            inotify.rm_watch self._handle wd
            'discard self.watched-dirs dir

    fn dispatch-events (self)
        local buf : (array i8 4096)
        loop ()
            data-size := unistd.read self._handle &buf (sizeof buf)
            if (data-size == -1)
                using import C.errno
                err := (errno)
                assert (err == EAGAIN or err == EWOULDBLOCK)
                break;

            loop (offset = 0:i64)
                if (offset >= data-size)
                    break;

                event := bitcast (& (buf @ offset)) (@ inotify.event)
                # TODO: dispatch callback
                offset + (sizeof inotify.event) + event.len

    inline __drop (self)
        unistd.close self._handle

do
    let FileWatcher
    local-scope;
