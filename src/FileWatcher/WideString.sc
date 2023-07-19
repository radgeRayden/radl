using import Buffer
using import slice
using import String
using import .headers
import ..traits

typedef widechar <<: u16
type+ (mutable@ widechar)
    using traits.element-coerces-to-storage

StackWideCharPointer := 
    static-eval
        'change-storage-class ('mutable (pointer.type widechar)) 'Function
type+ StackWideCharPointer
    using traits.element-coerces-to-storage

WideString := (HeapBuffer widechar)
WideStringView := (Slice WideString)
WideStringStack := (Buffer StackWideCharPointer (inline ()))
widestring := (size) -> (heapbuffer widechar size)
widestring-stack := (size) -> (WideStringStack (alloca-array widechar size) size)

fn... UTF-8->WideString (str : String)
    ptr size := 'data str
    len := windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) null 0

    result := stringbuffer widechar len
    written := 
        windows.MultiByteToWideChar windows.CP_UTF8 0 ptr (size as i32) result len
    assert (len == written)
    result

fn... WideString->UTF-8 (widestr : WideStringView)
    wptr wsize := 'data widestr
    len := windows.WideCharToMultiByte windows.CP_UTF8 0 wptr (wsize as i32) null 0 null null

    i8buf := stringbuffer i8 (('zero-terminated? widestr) (len - 1) len)
    written := windows.WideCharToMultiByte windows.CP_UTF8 0 wptr (wsize as i32) ('data i8buf) len null null
    assert (len == written)
    'from-rawstring String ('data i8buf)

inline WideString+ (T)
    type+ T
        inline... string-length (buf : (mutable@ widechar))
            (windows.wcslen buf) + 1
        case (wstr : WideStringView)
            this-function ('data wstr) ()

        inline strlen (self)
            windows.wcslen self

        inline empty-string? (self)
            or
                (countof self) == 0
                (('data self) @ 0) == 0

        inline zero-terminated? (self)
            last-idx := (countof self) - 1
            and
                (countof self) > 0
                (self @ last-idx) == 0

        inline __imply (thisT otherT)
            let ET =
                static-if (thisT.Type < Buffer)
                    thisT.Type.ElementType
                else
                    thisT.ElementType

            static-if (otherT < pointer 
                        and ((elementof otherT) == (storageof ET)))
                inline (self)
                    ('data self) as otherT
            elseif (otherT == String)
                inline (self)
                    WideString->UTF-8 self
            else
                super-type.__imply thisT otherT

        inline __rimply (otherT thisT)
            let ET =
                static-if (thisT.Type < Buffer)
                    thisT.Type.ElementType
                else
                    thisT.ElementType

            static-if (otherT < pointer 
                        and ((elementof otherT) == (storageof ET)))
                inline (incoming)
                    thisT incoming (string-length incoming)
            else
                super-type.__rimply otherT thisT

        inline __copy (self)
            result := widestring (countof self)
            buffercopy result (view self)
            result

        inline join (cls a b)
            result := stringbuffer widechar ((countof a) + (countof b))
            buffercopy result a
            buffercopy (rslice (view result) (countof a)) b
            result

        inline __printer (self print)
            print (WideString->UTF-8 (view self))

va-map WideString+
    _ WideString WideStringView WideStringStack

type+ WideString
    inline from-widestring (cls src)
        dst := widestring ('string-length (view src))
        buffercopy dst (view src)
        dst

type+ WideStringView
    inline from-widestring (cls wstr)
        lslice (view wstr) ('string-length wstr)

do
    let UTF-8->WideString
        WideString->UTF-8
        widechar
        WideString
        WideStringView
        WideStringStack
        widestring
        widestring-stack
    local-scope;