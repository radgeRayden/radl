using import slice
using import String
using import String+
using import testing

match? start end := scan S"abcde" S"cd"
test match?
test
    (slice S"abcde" start end) == S"cd"
test
    (replace S"banana apple banana" S"banana" S"apple") == S"apple apple apple"

splitted := (split S"apple apple apple" S" ")
test ((countof splitted) == 3)
for s in splitted
    test (s == S"apple")

test
    (common-prefix S"/project/min/max/include.sc" S"/project/blah.sc") == S"/project/"
test
    (common-suffix S"/project/min/max/include.sc" S"/project/blah.sc") == S".sc"

test
    (common-prefix S"blah.sc" S"blah") == S"blah"
test
    (common-prefix S"blah" S"blah.sc") == S"blah"

test
    (common-suffix S"/project/min/max/include.sc" S".sc") == S".sc"
test
    (common-suffix S".sc" S"/project/min/max/include.sc" ) == S".sc"

test (starts-with? S"/home/user" S"/home")
test (not (starts-with? S"/home/user" S"/usr"))
test (ends-with? S"todo.txt" S".txt")
test (not (ends-with? S"todo.txt" S".dat"))
