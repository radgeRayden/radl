using import Array
using import hash
using import Map
using import Rc
using import String
using import struct
import C.errno

using import ..ext
using import .headers
using import .common

type WatchDescriptor < (opaque "InotifyWatchDescriptor") :: i32
    inline __rimply (otherT thisT)
        static-if (otherT == (storageof thisT))
            inline (incoming)
                bitcast incoming thisT

    inline __imply (thisT otherT)
        static-if (otherT == (storageof thisT))
            inline (self)
                bitcast self otherT

    inline __== (thisT otherT)
        static-if (thisT == otherT)
            inline (self other)
                (storagecast (view self)) == (storagecast (view other))

struct WatchedFile
    descriptor : i32
    name : String

    inline __hash (self)
        hash (hash self.descriptor) (hash self.name)

    inline __== (thisT otherT)
        static-if (thisT == otherT)
            inline (self other)
                and (self.descriptor == other.descriptor) (self.name == other.name)

struct FileWatcher
    _handle : i32
    watch-descriptors : (Map String (Rc WatchDescriptor))
    watched-dirs : (Map i32 String)
    file-callbacks : (Map WatchedFile FileWatchCallback)

    inline __typecall (cls)
        handle := (inotify.init1 inotify.IN_NONBLOCK)
        assert (handle != -1)

        super-type.__typecall cls
            _handle = handle

    fn... watch (self, path : String, callback : FileWatchCallback)
        # NOTE: dirname may segfault if given read only memory. Currently String always has a heap backing,
        # but this may not be true in the future for serialized constant Strings.
        dir file := 'from-rawstring String (libgen.dirname (copy path)), 'from-rawstring String (libgen.basename path)
        if (file == "")
            raise FileWatchError.NotAFile

        using inotify filter "^IN_"
        mask := | IN_ONLYDIR IN_CREATE IN_MODIFY IN_DELETE
        let wd =
            try
                copy ('get self.watch-descriptors dir)
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

        cb-key := WatchedFile (view (imply wd WatchDescriptor)) (copy path)
        'set self.file-callbacks cb-key callback
        'set self.watched-dirs (view (imply wd WatchDescriptor)) (copy dir)
        ()

    fn... unwatch (self, path : String)
        local path = copy path
        dir file := 'from-rawstring String (libgen.dirname path), 'from-rawstring String (libgen.basename path)
        if (file == "")
            return;

        let wd =
            try ('get self.watch-descriptors dir)
            else (return)

        do
            cb-key := WatchedFile (copy wd) (copy path)
            if ('in? self.file-callbacks cb-key)
                'discard self.file-callbacks cb-key
            else
                return;

        refT := (Rc WatchDescriptor)
        if ((refT.strong-count wd) == 1)
            inotify.rm_watch self._handle (view (imply wd WatchDescriptor))
            'discard self.watched-dirs wd
            'discard self.watch-descriptors dir

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

                # NOTE: this event handling is not comprehensive. I intend to improve it as I use the library.
                :: dispatch-callback
                if (not event.len)
                    merge dispatch-callback

                try
                    dir file := 'get self.watched-dirs event.wd, 'from-rawstring String (&event.name as rawstring)
                    path := .. dir "/" file
                    cb-key := WatchedFile event.wd (copy path)
                    cb := ('get self.file-callbacks cb-key)

                    vvv bind evtype
                    do (using inotify filter "^IN_")
                        mask := event.mask
                        if (mask & IN_CREATE)
                            FileEventType.Created
                        elseif (mask & IN_DELETE)
                            FileEventType.Deleted
                        elseif (mask & IN_MODIFY)
                            FileEventType.Modified
                        else (merge dispatch-callback)

                    cb path evtype
                else ()
                dispatch-callback ::

                offset + (sizeof inotify.event) + event.len

    inline __drop (self)
        unistd.close self._handle

do
    let FileWatcher
    local-scope;
