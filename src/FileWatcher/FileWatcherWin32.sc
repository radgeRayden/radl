using import Array
using import Buffer
using import Map
using import Rc
using import Slice
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
        lslice lhs-path ('string-length lhs-path)
        lslice rhs-path ('string-length rhs-path)

struct FileWatcher
    watch-handles : (Map WideString (Rc WatchHandle))

    fn... watch (self, path : String, callback : FileWatchCallback)
        pathW := UTF-8->WideString path
        dir file := split-path pathW
        if ('empty-string? file)
            raise FileWatchError.NotAFile

        if (not (windows.PathFileExistsW pathW))
            raise FileWatchError.NotFound

        attrs := windows.GetFileAttributesW pathW
        if (attrs & windows.FILE_ATTRIBUTE_DIRECTORY)
            raise FileWatchError.NotAFile

        if ('in? self.watch-handles dir)
            return;

        dirhandle :=
            windows.CreateFileW dir 0x80000000 # GENERIC_READ
                | windows.FILE_SHARE_READ windows.FILE_SHARE_WRITE windows.FILE_SHARE_DELETE
                null
                windows.OPEN_EXISTING
                windows.FILE_FLAG_BACKUP_SEMANTICS
                null
        if (dirhandle == windows.INVALID_HANDLE_VALUE) 
            raise FileWatchError.AccessDenied

        'set self.watch-handles dir dirhandle
    fn... unwatch (self, path : String)
    fn dispatch-events (self)
    inline __drop (self)

do
    let FileWatcher
    local-scope;