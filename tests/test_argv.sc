using import testing
using import argv

inline build-argv (args...)
    local argv =
        arrayof rawstring args...
    _ (va-countof args...) (&argv as (@ rawstring))

do
    argc argv := build-argv "-a" "bcdfghi"

    # normal usage: `normalize-script-args (script-launch-args)'
    argc argv := (normalize-script-args "test_argv" argc argv)
    test (argc == 3)
    test ((string (argv @ 0)) == "test_argv")
    test ((string (argv @ 1)) == "-a")
    test ((string (argv @ 2)) == "bcdfghi")

sugar-if false
    arg-parser :=
        define-argv
            subcommand build
                arg input : String
                    positional
                    "-i"
                    "--input"
                arg? output : String
                    positional
                    "-o"
                    "--output"
                    default = "{input}.bin"
                flag display-help? "h" "--help"

            alias (compile = build) # substitutes a command for another
            subcommand run < build # inherits args from build

            default-subcommand = build # if sub is ommitted, we use build

    bottle-args :=
        define-argv
            subcommand demo
                arg name : String
                    positional
                    "--name"
                    default = "hello-bottle"
            subcommand install

            default-subcommand = demo
