import C.bindings
from C.bindings.extern let feof

fn git-version ()
    _popen _pclose popen pclose := # hack for windows-mingw
    using import C.stdio
    using import String

    let popen pclose =
        static-if (operating-system == 'windows) (_ _popen _pclose)
        else (_ popen pclose)

    inline try-commands (def cmd...)
        let devnull =
            static-if (operating-system == 'windows) str"NUL"
            else str"/dev/null"
        va-map
            inline "#hidden" (cmd)
                handle := popen (.. "bash -c '" cmd "' 2> " devnull) "r"
                local result : String
                while (not ((feof handle) as bool))
                    local c : i8
                    fread &c 1 1 handle
                    if (c != 0 and c != char"\n")
                        'append result c

                if ((pclose handle) == 0)
                    return (deref result)
                S""
            cmd...
        def

    try-commands S"unknown"
        "git describe --exact-match --tags HEAD"
        "echo git-$(git rev-parse --short HEAD)-$(git rev-parse --abbrev-ref HEAD)"

do
    let git-version
    local-scope;
