using import ArrayMap
using import testing

local am : (ArrayMap One)
e0 e1 e2 e3 e4 e5 e6 e7 e8 e9 :=
    va-map
        inline (i)
            'add am (One i)
        va-range 10

using import print

test ('in? am e5)
'remove am e5
'remove am e7
'remove am e2
'remove am e2
'add am (One 25)

for id element in am
    'check element

test (not ('in? am e5))
test ('in? am e1)

'remove am e0
'remove am e9

for id element in am
    'check element

test ((countof am) == 6)

am = ((ArrayMap One))
One.test-refcount-balanced;
