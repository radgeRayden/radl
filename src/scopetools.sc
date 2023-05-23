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

        qq
            'define-symbols [(Scope)]
                unquote-splice kpairs

locals;
