using import Array
using import Buffer
using import Slice
using import String
using import struct
using import .headers
using import .common
import ..traits

inline stackbuffer (T size)
    Buffer.wrap (alloca-array T size) size (inline ())

typedef widechar <<: u16
WideString := (HeapBuffer widechar)
WideStringView := (SliceView (mutable@ widechar))
widestring := (size) -> (heapbuffer widechar size)
widestring-stack := (size) -> (stackbuffer widechar size)

type+ (mutable@ widechar)
    using traits.element-coerces-to-storage

type+ WideString
    inline... buffer-length (buf : (mutable@ widechar))
        (windows.wcslen buf) + 1
    case (wstr : WideStringView)
        this-function ('data wstr) ()

type+ WideStringView
    inline from-widestring (cls wstr)
        lslice (view wstr) ('buffer-length wstr)

fn winstr (str)
    ptr size := 'data str
    # NOTE: we add 1 to the size because 'data ignores the null terminator as of 2023/07/13
    size := size + 1
    len := windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) null 0

    result := widestring len
    written := 
        windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) ('data result) len
    assert (len == written)
    result

fn... winstr->UTF-8 (widestr : WideStringView)
    wptr wsize := 'data widestr
    len := windows.WideCharToMultiByte windows.CP_UTF8 0 wptr (wsize as i32) null 0 null null

    i8buf := heapbuffer i8 len
    written := windows.WideCharToMultiByte windows.CP_UTF8 0 wptr (wsize as i32) ('data i8buf) len null null
    assert (len == written)
    'from-rawstring String ('data i8buf)

fn... full-path (path : WideStringView)
    buf := windows._wfullpath null ('data path) windows.MAX_PATH
    # TODO: raise if buf is null
    WideString buf (WideString.buffer-length buf)

fn... split-path (path : WideStringView)
    path := full-path path

    drive := stackbuffer u16 4
    dir := stackbuffer u16 windows.MAX_PATH
    file := stackbuffer u16 windows.MAX_PATH
    ext := stackbuffer u16 windows.MAX_PATH

    drive-ptr drive-size := 'data drive
    dir-ptr dir-size     := 'data dir
    file-ptr file-size   := 'data file
    ext-ptr ext-size     := 'data ext
    windows._wsplitpath_s ('data path) \
        drive-ptr drive-size dir-ptr dir-size file-ptr file-size ext-ptr ext-size
    
    lhs-path := widestring windows.MAX_PATH
    lhs-path-ptr lhs-path-size := 'data lhs-path
    windows._wmakepath_s lhs-path-ptr lhs-path-size ('data drive) ('data dir) null null

    rhs-path := widestring windows.MAX_PATH
    rhs-path-ptr rhs-path-size := 'data rhs-path
    windows._wmakepath_s rhs-path-ptr rhs-path-size null null ('data file) ('data ext) ()
    
    _ 
        lslice lhs-path ('buffer-length lhs-path)
        lslice rhs-path ('buffer-length rhs-path)

print (winstr->UTF-8 (winstr S"ẝ𝓸ĺ𝑑è𝖗"))
path := winstr S"./blah/bluh"
print (va-map winstr->UTF-8 (split-path path))

struct FileWatcher
    fn... watch (self, path : String, callback : FileWatchCallback)
    fn... unwatch (self, path : String)
    fn dispatch-events (self)
    inline __drop (self)

do
    let FileWatcher
    local-scope;