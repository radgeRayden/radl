using import Cache testing

global c : (Cache i32)
global call-count : i32

fn getv ()
    'get c str"first"
        fn ()
            call-count += 1
            call-count

test ((getv) == 1:i32)
test ((getv) == 1:i32)

'set c str"first" 10:i32
test ((getv) == 10:i32)
test ((getv) == 10:i32)

test (call-count == 1)

'clear c
(getv)
test (call-count == 2)

# any hashable object can be a key
let v =
    'get c 100:u64
        fn (arg)
            arg + 10
        5

test (v == 15)
