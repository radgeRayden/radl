using import Array
using import String
import C.string
stbsp := import stb.sprintf

fn scan (input term)
    inputptr termptr := imply input pointer, imply term pointer
    result := C.string.strstr inputptr termptr
    if (result != null)
        offset := (ptrtoint result usize) - (ptrtoint inputptr usize)
        _ true (copy offset) (copy (offset + (countof term)))
    else
        _ false 0:usize 0:usize

fn replace (input term replacement)
    local result : String
    loop (next match? start end = input (scan input term))
        if (not match?)
            'append result next
            break result
        else
            'append result (lslice next start)
            'append result (copy replacement)

            next := rslice next end
            _ next (scan next term)

fn split (input separator)
    local results : (Array String)
    loop (next match? start end = input (scan input separator))
        if (not match?)
            'append results next
            break results
        else
            'append results (lslice next start)
            next := rslice next end
            _ next (scan next separator)

fn common-prefix (a b)
    for idx in (range (min (countof a) (countof b)))
        if ((a @ idx) != (b @ idx))
            return (lslice a idx)
    copy a

fn common-suffix (a b)
    for offset in (range (min (countof a) (countof b)))
        idxa idxb := (countof a) - offset - 1, (countof b) - offset - 1
        if ((a @ idxa) != (b @ idxb))
            return (rslice a (idxa + 1))
    copy a

do
    let scan replace split common-prefix common-suffix
    local-scope;
