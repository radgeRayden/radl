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

FileWatchCallback := (@ (function (void (viewof String 1) FileEventType)))

struct WatchedFile
    path : String
    descriptor : (Rc i32)
    callback : FileWatchCallback

struct FileWatcher
    handle : i32
    watched-dirs : (Map String (Rc i32))
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
        mask := | IN_ONLYDIR IN_CREATE
        let wd =
            'getdefault self.watched-dirs dir
                inotify.add_watch self.handle dir inotify.IN_MOVE

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

        ref := Rc.wrap wd
        'set self.watched-files (copy path)
            WatchedFile (copy path) ('upgrade (copy ref)) callback
        'set self.watched-dirs (copy path) (copy ref)
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
            inotify.rm_watch self.handle wd
            'discard self.watched-dirs dir

    fn poll (self)

    inline __drop (self)
        unistd.close self.handle

do
    let FileWatcher
    local-scope;
