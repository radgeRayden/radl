spice module-exists? (base-dir name env)
    try
        find-module-path (base-dir as string) (name as string) (env as Scope)
        `true
    else
        `false

sugar module-exists? (name)
    base-dir := '@ sugar-scope 'module-path
    env := '@ sugar-scope '__env
    qq
        [module-exists?] [base-dir] [(name as Symbol as string)] [env]

sugar stop-expansion-if (condition)
    let condition =
        sugar-match condition
        case (condition)
            (sc_expand condition '() sugar-scope) as bool
        case ('not condition)
            not
                (sc_expand condition '() sugar-scope) as bool
        default
            error "incorrect syntax"

    if condition
        _ '() '()
    else
        _ '() next-expr

do
    let module-exists?
        stop-expansion-if
    local-scope;
