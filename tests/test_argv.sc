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

do
    fn build (input output)
        print input output
    fn run (input output)
        print input output

    parse-args :=
        define-argv-parser
            subcommand build
                arg input : String {positional, "-i", "--input"}
                arg? output : String {positional, "-o", "--output"}
                    default = "{input}.bin"
                flag display-help? "h" "--help"
                execute
                    build input output
            alias (compile = build) # substitutes a command for another
            subcommand run < build # inherits args from build
                execute
                    run input output
            # optional, default looks like this
            subcommand usage
                execute
                    print usage
                    exit 1
            options
                default-subcommand = build # if no sub is specified, uses this. If ommitted, defaults to showing usage.
                flag-enable-prefix = "-" # optional, default '-'
                flag-disable-prefix = none # no flag disable, optional

    argc argv := build-argv "assembler" "-i" "hello.s" "hello.bin"
    parse-args argc argv
    0

sugar-if false
    bottle-args :=
        define-argv
            subcommand demo
                arg name : String
                    positional
                    "--name"
                    default = "hello-bottle"
            subcommand install

            default-subcommand = demo
