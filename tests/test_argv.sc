using import testing
using import argv
using import String

inline build-argv (args...)
    local argv =
        arrayof rawstring args...
    _ (va-countof args...) (&argv as (@ rawstring))

spice scope-key-lookup (scope key f)
    scope as:= Scope

    sw := (sc_switch_new `(hash key))
    sc_switch_append_default sw `(f none)
    for k v in scope
        k := k as Symbol as string
        sc_switch_append_case sw `[(hash k)] `(f v)
    sw

run-stage;

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
        print (.. input output)
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

    fn parse-args (argc argv)
        using import scopetools
        curly-list := make-scope

        ctx := {
            subcommands = {
                build = {
                    name = "build",
                    positionals = '(input output),
                    args = {
                        input = {
                            type = String,
                            optional? = false,
                            name0 = "-i",
                            name1 = "--input"
                        },
                        output = {
                            type = String,
                            optional? = true,
                            name0 = "-o",
                            name1 = "--output"
                        }
                    },
                    flags = {
                        display-help? = {
                            short = "h",
                            long = "--help"
                        }
                    },
                    execute =
                        (inline (input output)
                            (print "blah")
                            (build input output))
                },
            },
            aliases = {
                compile = "build"
            },
            options = {
                default-subcommand = "build",
                flag-enable-prefix = "-",
                flag-disable-prefix = none
            }
        }

        fn exit-with-usage (code)
            print "Usage: correctly."
            exit code

        inline parse-execute (subcommand argc argv includes-name?)
            print subcommand.name
            false

        assert (argc > 0) "invalid argc"
        defsub := (getattr ctx.subcommands (Symbol ctx.options.default-subcommand))
        if (argc == 1)
            min-argc :=
                va-lfold 0
                    inline (__ next result)
                        static-if (not next.optional?)
                            result + 1
                        else
                            result
                    scope-unpack defsub.args

            static-if (min-argc == 0)
                defsub.execute;
            else
                exit-with-usage 1
        else
            arg1 := (argv @ 1)
            success? :=
                scope-key-lookup ctx.subcommands arg1
                    inline (subcommand)
                        static-if (none? subcommand)
                            false
                        else
                            parse-execute subcommand (argc - 1) &arg1 true

            if (not success?)
                parse-execute defsub (argc - 2) &arg1 false

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
