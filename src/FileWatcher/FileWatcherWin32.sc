using import Array
using import Buffer
using import Slice
using import String
using import struct
using import .headers
using import .common

WideString := (typeof (heapbuffer u16 1))

typedef WideStringView

inline... zero-terminated-lengthW (buf : (mutable@ u16))
    (windows.wcslen buf) + 1
case (wstr : WideStringView)
    this-function ('data wstr) ()
    
typedef+ WideStringView
    inline __typematch (cls T)
        T.ElementType == u16
    
    inline __rimply (cls T)
        inline (self) 
            static-if ((typeof self) == T)
                self
            else
                lslice (view self) (countof self)

    inline from-widestring (cls wstr)
        lslice (view wstr) (zero-terminated-lengthW wstr)

fn winstr (str)
    ptr size := 'data str
    # NOTE: we add 1 to the size because 'data ignores the null terminator as of 2023/07/13
    size := size + 1
    len := windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) null 0

    result := heapbuffer u16 len
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
    WideString buf (zero-terminated-lengthW buf)

fn... split-path (path : WideStringView)
    path := full-path path

    drive := heapbuffer u16 windows.MAX_PATH
    dir := heapbuffer u16 windows.MAX_PATH
    file := heapbuffer u16 windows.MAX_PATH
    ext := heapbuffer u16 windows.MAX_PATH

    drive-ptr drive-size := 'data drive
    dir-ptr dir-size     := 'data dir
    file-ptr file-size   := 'data file
    ext-ptr ext-size     := 'data ext
    windows._wsplitpath_s ('data path) \
        drive-ptr drive-size dir-ptr dir-size file-ptr file-size ext-ptr ext-size
    
    lhs-path := heapbuffer u16 windows.MAX_PATH
    lhs-path-ptr lhs-path-size := 'data lhs-path
    windows._wmakepath_s lhs-path-ptr lhs-path-size ('data drive) ('data dir) null null
    rhs-path := heapbuffer u16 windows.MAX_PATH
    rhs-path-ptr rhs-path-size := 'data rhs-path
    windows._wmakepath_s rhs-path-ptr rhs-path-size null null ('data file) ('data ext) ()
    
    _ 
        lslice lhs-path (zero-terminated-lengthW lhs-path)
        lslice rhs-path (zero-terminated-lengthW rhs-path)

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