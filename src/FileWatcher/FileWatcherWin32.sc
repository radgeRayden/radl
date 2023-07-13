using import Array
using import Buffer
using import Slice
using import String
using import struct
using import .headers

WideString := (typeof (heapbuffer u16 1))

typedef WideStringView
    inline __typematch (cls T)
        T.ElementType == u16
    
    inline __rimply (cls T)
        inline (self) (lslice (view self) (countof self))

    inline from-widestring (wstr)
        lslice (view wstr) ((windows.wcslen ('data wstr) ()) + 1)
    
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

fn... full-pathW (wpath : WideString)
    relative? := windows.PathIsRelativeW ('data wpath) ()
    if relative?
        buf := windows._wfullpath null ('data wpath) windows.MAX_PATH
        WideString buf ((windows.wcslen buf) + 1)
    else
        copy wpath

fn dirname (path)
    pathw := winstr path
    full-path := full-pathW pathw
    windows.PathCchRemoveFileSpec ('data full-path)
    winstr->UTF-8 ('from-widestring WideStringView full-path)

fn basename (path)
    path := winstr path
    filename := heapbuffer u16 windows.MAX_PATH
    extension := heapbuffer u16 windows.MAX_PATH

    filename-ptr filename-size := 'data filename
    extension-ptr extension-size := 'data extension
    windows._wsplitpath_s ('data path) null 0 null 0 filename-ptr filename-size extension-ptr extension-size
    
    filepath := heapbuffer u16 windows.MAX_PATH
    path-ptr path-size := 'data filepath
    windows._wmakepath_s path-ptr path-size null null ('data filename) ('data extension) ()
    winstr->UTF-8 filepath

print (winstr->UTF-8 (winstr S"ẝ𝓸ĺ𝑑è𝖗"))
struct FileWatcher

do
    let FileWatcher
    local-scope;
