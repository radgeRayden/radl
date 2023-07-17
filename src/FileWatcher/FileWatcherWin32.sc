using import Array
using import Buffer
using import hash
using import Map
using import print
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
        # FIXME: kind of a hack. Need to investigate pointer trait.
        if ((storagecast (view self)) != null)
            windows.CloseHandle self
        ()

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

struct WatchedDirectory
    handle : WatchHandle
    overlapped : windows.OVERLAPPED
    path : WideStringView
    files : (Map WideStringView FileWatchCallback)
    notification-buffer : WideString

    inline __typecall (cls path)
        super-type.__typecall cls
            path = path
            overlapped =
                typeinit (hEvent = (windows.CreateEventW null false false null))
            notification-buffer = widestring (1024 * 64)

struct FileWatcher
    watched-directories : (Array WatchedDirectory)
    directory-path-map  : (Map WideStringView usize)

    fn... watch (self, path : String, callback : FileWatchCallback)
        pathW := UTF-8->WideString path
        dir file := split-path (full-path (view pathW))
        if ('empty-string? file)
            raise FileWatchError.NotAFile

        if (not (windows.PathFileExistsW pathW))
            raise FileWatchError.NotFound

        attrs := windows.GetFileAttributesW pathW
        if (attrs & windows.FILE_ATTRIBUTE_DIRECTORY)
            raise FileWatchError.NotAFile

        let watched-directory =
            try 
                idx := 'get self.directory-path-map (view dir)
                wd := self.watched-directories @ idx
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

                'set self.directory-path-map (copy dir) (countof self.watched-directories)
                watched-directory :=
                    'append self.watched-directories 
                        WatchedDirectory (path = (copy dir))

                local result : i32
                bufptr bufsize := 'data watched-directory.notification-buffer
                result =
                    windows.ReadDirectoryChangesW (view dirhandle) bufptr (bufsize as u32) false 
                        |   windows.FILE_NOTIFY_CHANGE_FILE_NAME
                            windows.FILE_NOTIFY_CHANGE_LAST_WRITE    
                            windows.FILE_NOTIFY_CHANGE_SIZE
                        null
                        &watched-directory.overlapped
                        null
                watched-directory.handle = dirhandle
                watched-directory
        
        'set watched-directory.files (copy file) callback
        ()
        
    fn dispatch-events (self)
        for wd in self.watched-directories
            notification-buffer := wd.notification-buffer
            bufptr bufsize := 'data notification-buffer

            local bytes-written : u32
            local result = windows.GetOverlappedResult wd.handle &wd.overlapped &bytes-written false
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
                    dir := wd.path
                    path := 'join WideString dir file
                    cb := 'get wd.files path

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

            # queue again.
            result =
                windows.ReadDirectoryChangesW wd.handle bufptr (bufsize as u32) false 
                    |   windows.FILE_NOTIFY_CHANGE_FILE_NAME
                        windows.FILE_NOTIFY_CHANGE_LAST_WRITE    
                        windows.FILE_NOTIFY_CHANGE_SIZE
                    &bytes-written
                    &wd.overlapped
                    null
            ()

    fn... unwatch (self, path : String)
        pathW := UTF-8->WideString path
        dir file := split-path (full-path (view pathW))
        try
            idx := 'get self.directory-path-map dir
            wd := self.watched-directories @ idx
            'discard wd.files file
            if ((countof wd.files) == 0)
                wd = ('pop self.watched-directories)
                'discard self.directory-path-map dir
        else ()

do
    let FileWatcher
    local-scope;