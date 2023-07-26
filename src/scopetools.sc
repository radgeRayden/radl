spice scope-string-keys (scope)
    using import Array
    scope as:= Scope
    local keys : (Array Value)
    for k v in scope
        'append keys `[(k as Symbol as string)]

    sc_argument_list_map_new ((countof keys) as i32)
        (i) -> (keys @ i)

spice scope-unpack (self)
    using import Array
    self as:= Scope

    local values : (Array Value)
    for k v in self
        'append values v

    sc_argument_list_map_new ((countof values) as i32)
        (i) -> (values @ i)

spice static-join-scopes (...)
    fold (result = (Scope)) for s in ('args ...)
        .. result (s as Scope)

sugar make-scope (...)
    if (empty? (... as list))
        qq
            [do]
                [(Scope)]
    else
        let kpairs =
            if ((countof ...) == 3)
                list ...
            else
                uncomma ...

        vvv bind kpairs
        fold (result = '()) for kpair in ('reverse kpairs)
            cons
                cons `let (kpair as list)
                result

        qq
            [do]
                unquote-splice kpairs
                [locals];

spice scope-key-lookup (scope key f)
    using import hash String
    scope as:= Scope

    sw := (sc_switch_new `(hash (String key)))
    sc_switch_append_default sw `(f none)
    for k v in scope
        k := k as Symbol as string
        sc_switch_append_case sw `[(hash k)] `(f v)
    sw

locals;
