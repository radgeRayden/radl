using import Array
using import String
using import struct
using import .headers

WideString := (Array u16)

fn winstr (str)
    ptr size := 'data str
    len := windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) null 0
    
    local result : WideString (capacity = ((len + 1) as usize))
    'resize result ((len + 1) as usize)
    u16buf := (imply result pointer) as (mutable@ u16)

    windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) u16buf len
    result

fn... winstr->UTF-8 (ptr : (mutable@ u16), size : usize)
    len := windows.WideCharToMultiByte windows.CP_UTF8 0 ptr (size as i32) null 0 null null

    local result : String (capacity = ((len + 1) as usize))
    'resize result ((len + 1) as usize)
    i8buf := (imply result pointer) as (mutable@ i8)

    windows.WideCharToMultiByte windows.CP_UTF8 0 ptr (size as i32) i8buf len null null
    result
case (data : WideString)
    this-function ('data data)

fn... full-pathW (path : String)
    wpath := winstr path
    relative? := windows.PathIsRelativeW wpath
    if relative?
        buf := windows._wfullpath null wpath windows.MAX_PATH
        result := 'wrap WideString buf ((windows.wcslen buf) + 1)
        free buf
        result
    else
        wpath

fn dirnameW (pathw)
    local pathw = copy pathw
    windows.PathCchRemoveFileSpec ('data pathw)
    pathw

fn basename (path)
    local path = winstr path

    local filename : WideString (capacity = windows.MAX_PATH)
    'resize filename windows.MAX_PATH
    local extension : WideString (capacity = windows.MAX_PATH)
    'resize extension windows.MAX_PATH

    filename-ptr filename-size := 'data filename
    extension-ptr extension-size := 'data extension
    windows._wsplitpath_s path null 0 null 0 filename-ptr filename-size extension-ptr extension-size
    
    path-ptr path-size := 'data path
    windows._wmakepath_s path-ptr path-size null null filename extension
    winstr->UTF-8 path

struct FileWatcher

do
    let FileWatcher
    local-scope;
