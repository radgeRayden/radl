module-setup rectT
using import Array enum struct

struct Rect plain
    x : i32
    y : i32
    w : i32
    h : i32

let rectT =
    static-if default-import? Rect
    else rectT

# Cases for splitting empty space in an atlas.
enum AtlasSplitResult plain
    TooSmall
    JustRight
    Once
    Twice

struct Atlas
    spaces : (Array rectT)

    # Clears a rect atlas and sets it up for packing with a given space size.
    # TL note: I could have made this more generic but I don't think it needs to be.
    fn... clear (self, side-size : i32, ox : i32 = 0, oy : i32 = 0)
        'clear self.spaces
        'emplace-append self.spaces ox oy side-size side-size

    # Packs a rectangle into an atlas if it fits. Returns whether or not it could
    # be packed. Sets `rect`'s `x`/`y` if it fits.
    fn... pack (self, rect : rectT)
        for target-space in ('reverse self.spaces)
            if (rect.w <= target-space.w and rect.h <= target-space.h)
                # maybe a bit too concise.
                space := copy target-space
                target-space = 'pop self.spaces
                result small-split big-split := this-type.split rect space

                inline set-pos ()
                    # I don't think we have ref multi assignment.
                    rect.x = space.x
                    rect.y = space.y

                using AtlasSplitResult

                switch result
                case TooSmall
                    'append self.spaces (copy space)
                case JustRight
                    set-pos;
                    return true
                case Once
                    set-pos;
                    'append self.spaces small-split
                    return true
                case Twice
                    set-pos;
                    'append self.spaces big-split
                    'append self.spaces small-split
                    return true
                default (unreachable)

        false

    # Split a space to fit a rectangle into it, retrieving the size for the smaller
    # and larger splits if they're calculated.
    fn... split (rect : rectT, space : rectT)
        free-w free-h := space.w - rect.w, space.h - rect.h

        # Rect won't fit into space/rect fits perfectly into space
        if (free-w < 0 or free-h < 0)
            return AtlasSplitResult.TooSmall (rectT) (rectT)
        if (free-w == 0 and free-h == 0)
            return AtlasSplitResult.JustRight (rectT) (rectT)


        # Rect fits perfectly in one dimension = create only one split
        if (free-w > 0 and free-h == 0)
            return AtlasSplitResult.Once
                rectT
                    x = space.x + rect.w
                    y = space.y
                    w = space.w - rect.w
                    h = space.h
                (rectT)
        elseif (free-w == 0 and free-h > 0)
            return AtlasSplitResult.Once
                rectT
                    x = space.x
                    y = space.y + rect.h
                    w = space.w
                    h = space.h - rect.h
                (rectT)

        # Otherwise two splits
        _
            AtlasSplitResult.Twice
            if (free-w > free-h)
                small := rectT space.x (space.y + rect.h) rect.w free-h
                big   := rectT (space.x + rect.w) space.y free-w space.h
                _ small big
            else
                small := rectT (space.x + rect.w) space.y free-w rect.h
                big   := rectT space.x (space.y + rect.h) space.w free-h
                _ small big

do
    let Atlas
    let AtlasRect = rectT
    local-scope;
