using import rect-pack
using import itertools

local atlas : Atlas
'clear atlas 1024

for i x y in (enumerate (span (range 2 6) (range 2 6)))
    local rect : AtlasRect 0 0 x y
    packed := 'pack atlas rect
    assert packed
    print "rect" i "packed at (" rect.x "," rect.y ") (" rect.w "," rect.h ")"
