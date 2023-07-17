using import Array
using import Buffer
using import hash
using import Map
using import Rc
using import slice
using import String
using import struct
using import .common
using import .headers
using import .WideString
import ..traits

typedef WatchHandle <<:: windows.HANDLE
    using traits.element-coerces-to-storage
    inline __drop (self)
        windows.CloseHandle self
        ()

struct WatchedFile
    handle : windows.HANDLE
    name : WideString

    inline __hash (self)
        hash (hash self.handle) (hash self.name)

    inline __== (thisT otherT)
        static-if (thisT == otherT)
            inline (self other)
                and (self.handle == other.handle) (self.name == other.name)

fn... full-path (path : WideStringView)
    buf := windows._wfullpath null path windows.MAX_PATH
    # TODO: raise if buf is null
    WideString buf (WideString.string-length buf)

fn... split-path (path : WideStringView)
    drive := widestring-stack 4
    dir := widestring-stack windows.MAX_PATH
    file := widestring-stack windows.MAX_PATH
    ext := widestring-stack windows.MAX_PATH

    drive-ptr drive-size := 'data drive
    dir-ptr dir-size     := 'data dir
    file-ptr file-size   := 'data file
    ext-ptr ext-size     := 'data ext
    windows._wsplitpath_s path \
        drive-ptr drive-size dir-ptr dir-size file-ptr file-size ext-ptr ext-size
    
    lhs-path := widestring windows.MAX_PATH
    lhs-path-ptr lhs-path-size := 'data lhs-path
    windows._wmakepath_s lhs-path-ptr lhs-path-size drive dir null null

    rhs-path := widestring windows.MAX_PATH
    rhs-path-ptr rhs-path-size := 'data rhs-path
    windows._wmakepath_s rhs-path-ptr rhs-path-size null null file ext
    
    _ 
        lslice lhs-path ('strlen lhs-path)
        lslice rhs-path ('strlen rhs-path)

global woverlapped : windows.OVERLAPPED
struct FileWatcher
    watch-handles : (Map WideStringView (Rc WatchHandle))
    watched-dirs : (Map windows.HANDLE WideStringView)
    file-callbacks : (Map WatchedFile FileWatchCallback)
    notification-buffer : WideString

    inline __typecall (cls)
        super-type.__typecall cls
            notification-buffer = widestring (1024 * 64)

    fn... watch (self, path : String, callback : FileWatchCallback)
        pathW := UTF-8->WideString path
        dir file := split-path (view pathW)
        if ('empty-string? file)
            raise FileWatchError.NotAFile

        if (not (windows.PathFileExistsW pathW))
            raise FileWatchError.NotFound

        attrs := windows.GetFileAttributesW pathW
        if (attrs & windows.FILE_ATTRIBUTE_DIRECTORY)
            raise FileWatchError.NotAFile

        let watch-handle =
            try (copy ('get self.watch-handles (view dir)))
            else
                dirhandle :=
                    windows.CreateFileW dir 0x80000000 # GENERIC_READ
                        | windows.FILE_SHARE_READ windows.FILE_SHARE_WRITE windows.FILE_SHARE_DELETE
                        null
                        windows.OPEN_EXISTING
                        windows.FILE_FLAG_BACKUP_SEMANTICS | windows.FILE_FLAG_OVERLAPPED
                        null
                if (dirhandle == windows.INVALID_HANDLE_VALUE) 
                    raise FileWatchError.AccessDenied

                woverlapped.hEvent = windows.CreateEventW null false false null 
                local result : i32
                bufptr bufsize := 'data self.notification-buffer
                result =
                    windows.ReadDirectoryChangesW dirhandle bufptr (bufsize as u32) false 
                        |   windows.FILE_NOTIFY_CHANGE_FILE_NAME
                            windows.FILE_NOTIFY_CHANGE_LAST_WRITE    
                            windows.FILE_NOTIFY_CHANGE_SIZE
                        null
                        &woverlapped
                        null

                watch-handle := Rc.wrap (imply dirhandle WatchHandle)
                'set self.watch-handles (copy dir) (copy watch-handle)
                watch-handle
        
        'set self.file-callbacks
            WatchedFile (imply watch-handle WatchHandle) (copy pathW)
            callback
        'set self.watched-dirs (imply watch-handle WatchHandle) (copy dir)

        ()
        
    fn... unwatch (self, path : String)

    fn dispatch-events (self)
        for __ dirhandle in self.watch-handles
            notification-buffer := self.notification-buffer
            bufptr bufsize := 'data notification-buffer
            local bytes-written : u32
            
            local result = windows.GetOverlappedResult dirhandle &woverlapped &bytes-written false
            if (not result)
                assert ((windows.GetLastError) == windows.ERROR_IO_INCOMPLETE)
                return;
            
            loop (offset = 0:u32)
                if (offset >= bytes-written)
                    break;
                
                event := bitcast (& (bufptr @ offset)) (@ windows.FILE_NOTIFY_INFORMATION)
                :: dispatch-callback
                try
                    filename-size := (copy event.FileNameLength) // 2
                    file := stringbuffer widechar filename-size
                    buffercopy file (viewbuffer (&event.FileName as (mutable@ widechar)) filename-size)
                    dir := 'get self.watched-dirs dirhandle
                    path := 
                        'join WideString dir file

                    cb :=
                        'get self.file-callbacks
                            WatchedFile (imply dirhandle WatchHandle) (copy path)

                    let event-type =
                        switch event.Action
                        case windows.FILE_ACTION_ADDED
                            FileEventType.Created
                        case windows.FILE_ACTION_REMOVED
                            FileEventType.Deleted
                        case windows.FILE_ACTION_MODIFIED
                            FileEventType.Modified
                        default
                            FileEventType.Modified

                    cb (WideString->UTF-8 path) event-type
                else ()
                dispatch-callback ::

                if (not event.NextEntryOffset)
                    break;

                offset + event.NextEntryOffset

            result =
                windows.ReadDirectoryChangesW dirhandle bufptr (bufsize as u32) false 
                    |   windows.FILE_NOTIFY_CHANGE_FILE_NAME
                        windows.FILE_NOTIFY_CHANGE_LAST_WRITE    
                        windows.FILE_NOTIFY_CHANGE_SIZE
                    null
                    &woverlapped
                    null
            print result
            ()

    inline __drop (self)

global fw : FileWatcher
try
    'watch fw (String (module-dir .. "\\test.txt"))
        fn (...)
            print ...
except (ex) (print ex)

while true
    windows.Sleep 2500
    print "dispatching"
    'dispatch-events fw

do
    let FileWatcher
    local-scope;