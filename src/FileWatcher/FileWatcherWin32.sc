using import Array
using import Buffer
using import Map
using import Rc
using import Slice
using import String
using import struct
using import .headers
using import .common
import ..traits

typedef widechar <<: u16

StackWideCharPointer := 
    static-eval
        'change-storage-class ('mutable (pointer.type widechar)) 'Function

type+ (mutable@ widechar)
    using traits.element-coerces-to-storage
type+ StackWideCharPointer
    using traits.element-coerces-to-storage

typedef WatchHandle <<:: windows.HANDLE
    using traits.element-coerces-to-storage
    inline __drop (self)
        windows.CloseHandle self
        ()

WideString := (HeapBuffer widechar)
WideStringView := (SliceView (mutable@ widechar))
WideStringStack := (Buffer StackWideCharPointer (inline ()))

widestring := (size) -> (heapbuffer widechar size)
widestring-stack := (size) -> (WideStringStack (alloca-array widechar size) size)

type+ WideString
    inline... buffer-length (buf : (mutable@ widechar))
        (windows.wcslen buf) + 1
    case (wstr : WideStringView)
        this-function ('data wstr) ()

    inline empty-string? (self)
        or
            (countof self) == 0
            (('data self) @ 0) == 0

    inline __imply (thisT otherT)
        static-if (otherT < pointer and (elementof otherT) == (storageof thisT.ElementType))
            inline (self)
                ('data self) as otherT
        else
            super-type.__imply thisT otherT

inline chained-mixin (types...)
    static-eval (.. (va-map mixin types...))

type+ WideStringStack
    using (chained-mixin this-type WideString)

type+ WideStringView
    using (chained-mixin this-type WideString)

    inline from-widestring (cls wstr)
        lslice (view wstr) ('buffer-length wstr)

fn UTF-8->WideString (str)
    ptr size := 'data str
    # NOTE: we add 1 to the size because 'data ignores the null terminator as of 2023/07/13
    size := size + 1
    len := windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) null 0

    result := widestring len
    written := 
        windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) result len
    assert (len == written)
    result

fn... WideString->UTF-8 (widestr : WideStringView)
    wptr wsize := 'data widestr
    len := windows.WideCharToMultiByte windows.CP_UTF8 0 wptr (wsize as i32) null 0 null null

    i8buf := heapbuffer i8 len
    written := windows.WideCharToMultiByte windows.CP_UTF8 0 wptr (wsize as i32) ('data i8buf) len null null
    assert (len == written)
    'from-rawstring String ('data i8buf)

fn... full-path (path : WideStringView)
    buf := windows._wfullpath null path windows.MAX_PATH
    # TODO: raise if buf is null
    WideString buf (WideString.buffer-length buf)

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
        lslice lhs-path ('buffer-length lhs-path)
        lslice rhs-path ('buffer-length rhs-path)

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