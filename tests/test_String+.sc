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
