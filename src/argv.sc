using import struct
using import Map
using import String
import .random

global rng : random.RNG (ptrtoint (storagecast type) u64)
fn gensym ()
    Symbol (.. "#" (tostring (rng)))

sugar set-scope (scope)
    scope env := sc_expand scope '() sugar-scope
    _ '() next-expr (scope as Scope)

spice static-join-scopes (...)
    fold (result = (Scope)) for s in ('args ...)
        .. result (s as Scope)

parent-scope-sym := (gensym)
context-sym := (gensym)
run-stage;

sugar sugar-context (scope parent body...)
    scope env := sc_expand scope '() sugar-scope
    parent env := sc_expand parent '() sugar-scope
    scope as:= Scope
    parent as:= Scope
    qq
        [do]
            [set-scope] [(parent .. scope)]
            [let] [parent-scope-sym] = [parent]
            unquote-splice body...

run-stage;

sugar arg (name ': T (curly-list args...) ...)
    print name T (uncomma args...)
    qq
        'define [context-sym]

sugar arg? (name ': T (curly-list args...) ...)
    print name T (uncomma args...)
    qq
        [(Scope)]

sugar flag (name short long)
    qq
        [(Scope)]

sugar execute (body...)
    qq
        do
            [set-scope] [parent-scope-sym]
            [(Scope)]
locals;

run-stage;

sugar subcommand (name args...)
    vvv bind subscope
    do
        let arg arg? flag execute
        indirect-let context-sym = (Scope)
        locals;

    result-scope :=
    qq
        [sugar-context] [subscope] [parent-scope-sym]
            unquote-splice args...

sugar alias ((alias '= subcommand))

sugar options (...)
    print ...

run-stage;

#inline parse-args (argc argv context)
    assert (argc > 0) "invalid argc"
    if (argc == 1)
        defsub := context.options.default-subcommand
        static-if (not (none? defsub))
            if (defsub.argc == 0)
                defsub.execute;
            else
                context.show-usage;
        else
            context.show-usage;
    else
        argc := argc - 1
        arg1 := (argv @ 1)
        try
            subcommand := 'get context.subcommand-map arg1
            if (not ('process subcommand argc (& (argv @ 1))))
                context.show-usage;
        else
            defsub := context.options.default-subcommand
            static-if (not (none? defsub))
                if (not ('process defsub argc (& (argv @ 1))))
                    context.show-usage;
            else
                context.show-usage;

sugar define-argv-parser (body...)
    vvv bind context
    do
        subcommands := (Scope)
        # options := (Scope)
        locals;

    vvv bind subscope
    do
        let subcommand alias options
        indirect-let context-sym = context
        locals;

    qq
        # [sugar-context] [subscope] [sugar-scope]
        #     fn (argc argv)
        #         [static-join-scopes]
        #             unquote-splice body...
        #         [parse-args] argc argv [context-sym]
        ()

fn normalize-script-args (name argc argv)
    new-argv := malloc-array rawstring (argc + 1) # let it leak
    new-argv @ 0 = (&name as rawstring)

    for i in (range argc)
        new-argv @ (i + 1) = (argv @ i)
    _ (argc + 1) new-argv

do
    let normalize-script-args define-argv-parser
    locals;
