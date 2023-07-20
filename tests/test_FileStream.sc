using import Buffer String testing print
using import IO.FileStream

inline FileStream (...)
    try (FileStream ...)
    except (ex)
        print ex
        exit 1

f := FileStream module-path FileMode.Read

try
    for line in ('lines f)
        print line
    'rewind f
    print ('read-all-bytes f)
else ()

test-path := (module-dir .. "/test.txt")
test-str := S"Hello, World!\n"
f := FileStream test-path FileMode.Write

try ('write f test-str)
else ()
drop f

f := FileStream test-path FileMode.Read
let result-str =
    try ('read-all-string f)
    else (exit 1)
test (result-str == test-str)
'rewind f

# the test string is 14 characters long. This means we can fit 3 u32s (12 bytes).
let elements =
    try ('read-elements f (heapbuffer u32 10))
    else (exit 1)

test ((countof elements) == 3)
test (('tell f) == 12)
