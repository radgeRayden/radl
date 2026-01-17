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

inline enum-bitfield (ET outT values...)
    let values... =
        va-map
            inline (value)
                (imply value ET) as outT
            values...
    | values...

inline typeinit@ (...)
    implies (T)
        static-assert (T < pointer)
        imply (& (local := (elementof T) ...)) T

""""Small inline form with automatic parameter binding. Access the first argument as $
    or numbered parameters $0 to $7. Eg.: 
    ```
    ((λ print $) "hello world")
    ((λ print $0 "is" $1) "everyone" "beautiful")
    ```
sugar λ (body...)
    body... as:= list
    qq
        [inline] ($0 $1 $2 $3 $4 $5 $6 $7)
            $ := $0
            [body...]

do
    let module-exists? enum-bitfield typeinit@ λ
    |> := va-map
    curly-list := typeinit@

    local-scope;
