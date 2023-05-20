fn normalize-script-args (name argc argv)
    new-argv := malloc-array rawstring (argc + 1) # let it leak
    new-argv @ 0 = (&name as rawstring)

    for i in (range argc)
        new-argv @ (i + 1) = (argv @ i)

    _ (argc + 1) new-argv

do
    let normalize-script-args
    locals;
