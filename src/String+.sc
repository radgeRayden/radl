using import Array slice String
import C.string
stbsp := import stb.sprintf

scopes-enable-fn-default-view-parameters;

fn... scan (input, term)
    viewing *...
    inputptr termptr := 'data input, 'data term
    result := C.string.strstr inputptr termptr
    if (result != null)
        offset := (ptrtoint result usize) - (ptrtoint inputptr usize)
        _ true (copy offset) (copy (offset + (countof term)))
    else
        _ false 0:usize 0:usize

fn... replace (input : (Slice String), term : String, replacement : String)
    viewing *...
    local result : String
    loop (next match? start end = input (scan input term))
        if (not match?)
            'append result (trim next)
            break result
        else
            'append result (lslice next start)
            'append result (copy replacement)

            next := rslice (view next) end
            _ next (scan next term)

fn... split (input : (Slice String), separator)
    viewing *...
    local results : (Array String)
    loop (next match? start end = input (scan input separator))
        if (not match?)
            'append results (trim next)
            break results
        else
            'append results (trim (lslice next start))
            next := rslice next end
            _ next (scan next separator)

fn... common-prefix (a, b)
    viewing *...
    smallest := min (countof a) (countof b)
    for idx in (range smallest)
        if ((a @ idx) != (b @ idx))
            return (trim (lslice a idx))
    trim (lslice a smallest)

fn... common-suffix (a, b)
    viewing *...
    smallest := min (countof a) (countof b)
    for offset in (range smallest)
        idxa idxb := (countof a) - offset - 1, (countof b) - offset - 1
        if ((a @ idxa) != (b @ idxb))
            return (trim (rslice a (idxa + 1)))
    trim (rslice a ((countof a) - smallest))

fn... starts-with? (str, prefix)
    viewing *...
    (common-prefix str prefix) == prefix

fn... ends-with? (str, suffix)
    viewing *...
    (common-suffix str suffix) == suffix

fn... ASCII-tolower (str)
    viewing str

    delta := char"a" - char"A"
    local result : String
    for c in str
        if (c >= char"A" and c <= char"Z")
            'append result (c + delta)
        else
            'append result c
    result

fn... ASCII-toupper (str)
    viewing str

    delta := char"a" - char"A"
    local result : String
    for c in str
        if (c >= char"a" and c <= char"z")
            'append result (c - delta)
        else
            'append result c
    result

do
    let scan replace split common-prefix common-suffix starts-with? ends-with? ASCII-tolower ASCII-toupper
    local-scope;
