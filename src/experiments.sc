using import itertools testing

# Incomplete form of λ with automatic named parameter binding. One major issue is that
  parameters are defined in the order that they are seen. I've also not implemented symbol lookup
  in nested lists. One solution to symbol lookup would be to have a hook in the scope lookup, not
  depending on symbol scanning at all.
sugar λ* (body...)
    body... as:= list

    symbol? := (x) -> (('typeof x) == Symbol)
    parameter? := (x) -> ('match? str"^\\$" (x as string))
    inline new-binding? (x)
        try ('@ sugar-scope x) false
        else true

    let argument-list =
        ->> body...
            filter symbol?
            map ((x) -> (x as Symbol))
            filter parameter?
            filter new-binding?
            '()

    let body =
        if ((countof body...) == 1)
            _ (decons body...) ()
        else body...

    qq
        [inline] [argument-list]
            [body]

run-stage;

print ((λ* $x * $y) 5 9)
test-compiler-error
    # FIXME:
    (λ* ($x * $y)) 5 9

()

