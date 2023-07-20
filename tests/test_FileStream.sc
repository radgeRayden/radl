using import String testing print
using import IO.FileStream

test-str := S"Hello, World!\n"
local result-str : String
local success : bool
try
    f := FileStream module-path FileMode.Read
    for line in ('lines f)
        print line
    'rewind f
    print ('read-all-bytes f)

    f := FileStream "test.txt" FileMode.Write
    'write f test-str
    drop f

    f := FileStream "test.txt" FileMode.Read
    result-str = ('read-all-string f)
    print result-str
    success = true
except (ex)
    print ex

test (result-str == test-str)
test success
