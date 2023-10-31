using import ArrayMap
using import testing

local am : (ArrayMap One)
e0 e1 e2 e3 e4 e5 e6 e7 e8 e9 e0 :=
    va-map
        inline (i)
            'add am (One i)
        va-range 10

using import print

test ('in? am e5)
'remove am e5
'remove am e7
'remove am e2

test (not ('in? am e5))
test ('in? am e1)

am = ((ArrayMap One))
