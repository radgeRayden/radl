""""foreign
    =======

    Motivation: offer a syntax for importing C libraries that takes care of repetitive tasks like
    renaming scopes, 'merging' headers, writing stubs for macros and creating subscopes.

    Mockup of what an import could look like:

    foreign
        # headers are included in the same translation unit, in order.
        include "headerA.h" "headerB.h" "headerC.h"
        # compiler options
        options "-DHEADERA_IMPLEMENTATION"
        with-constants
            SOME_CONSTANT

        # not yet implemented
        with-scope libABC

# using import .core-extensions
using import .strfmt
using import slice
using import String

let constant-wrapper-prefix = "scopes_constant_wrapper__"
let constant-wrapper-regexp = (.. "^(?=" constant-wrapper-prefix ")")

inline filter-scope (scope pattern)
    pattern as:= string
    fold (scope = (Scope)) for k v in scope
        let name = (k as Symbol as string)
        let match? start end = ('match? pattern name)
        if match?
            'bind scope (Symbol (rslice name end)) v
        else
            scope

fn gen-constant-wrapper-fn (macro)
    f""""typeof(${macro}) ${constant-wrapper-prefix}${macro} () {
                return ${macro};
         }

sugar foreign (body...)
    vvv bind c-code namespaces constants extra-symbols
    loop (body c-code namespaces constants extra-symbols = body... S"" '() '() '())
        if (empty? body)
            break c-code namespaces constants extra-symbols
        let at next = (decons body)
        sugar-match (at as list)
        case ('include headers...)
            # we already generated some code, which will cause us problems.
            if (c-code != S"")
                error "only one include clause allowed, and the include clause must come first"

            local includestr : String
            for header in headers...
                header as:= string
                includestr ..= (interpolate "#include \"${0}\"\n" header)
            _ next (String includestr) namespaces constants extra-symbols
        case ('with-constants (curly-list constants...))
            local c-code = c-code

            vvv bind parsed-constants
            fold (constants = constants) for const in constants...
                if (('typeof const) != Symbol)
                    trace-error "while parsing constants" "non symbol constant forbidden"
                const as:= Symbol
                c-code ..= gen-constant-wrapper-fn const

                fn-name := Symbol (f"${constant-wrapper-prefix}${const}" as string)

                cons
                    qq [unlet] [fn-name]
                    cons
                        qq [let] [const] = ([fn-name])
                        constants

            _ next (String c-code) namespaces ('reverse parsed-constants) extra-symbols
        case ('options options...)
            _ next c-code namespaces constants extra-symbols
        case ('export 'from (curly-list namespaces...) 'matching regexp)
            vvv bind exported
            fold (exported = namespaces) for namespace in namespaces...
                if (('typeof namespace) != Symbol)
                    trace-error "while parsing namespaces" "non symbol namespace forbidden"
                namespace as:= Symbol
                name := 'unique Symbol (string ("module-" .. (tostring namespace)))

                cons
                    qq [let] [name] = ([filter-scope] (header . [namespace]) [(regexp as string)])
                    exported

            _ next c-code exported constants extra-symbols
        case ('export 'from (curly-list namespaces...))
            vvv bind exported
            fold (exported = namespaces) for namespace in namespaces...
                if (('typeof namespace) != Symbol)
                    trace-error "while parsing namespaces" "non symbol namespace forbidden"
                namespace as:= Symbol
                name := 'unique Symbol (string ("module-" .. (tostring namespace)))

                cons
                    qq [let] [name] = (header . [namespace])
                    exported

            _ next c-code exported constants extra-symbols
        case ('appending extras...)
            extra-symbols :=
                cons
                    extra-symbols
                    extras...
            _ next c-code namespaces constants ('reverse extra-symbols)
        default
            _ next c-code namespaces constants extra-symbols

    c-code as:= string
    module-sym := 'unique Symbol "scopes-module"
    qq
        [embed]
        [let] [module-sym] =
            [do]
                [let] header =
                    [include] [c-code]
                [..]
                    unquote-splice namespaces
                    [filter-scope] (header . extern) [constant-wrapper-regexp]
        [run-stage];
        [do]
            [using] [module-sym]
            unquote-splice constants
            unquote-splice extra-symbols
            [locals];

do
    let filter-scope foreign
    locals;
