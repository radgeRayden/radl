using import Array argv enum Map strfmt struct Option String print ext

# TEST: BASIC MODE
struct ProgramArguments
    name : String
    age : (Option i32)
    show-age? : bool

    PositionalParameters := '(name)

using import testing

inline validate-args (T args)
    argc argv := countof args, &args
    argparser := ((ArgumentParser T))
    try ('parse argparser argc argv) true
    except (ex)
        report "Error while parsing arguments:" ex.index ex.kind
        false

inline args> (args)
    local = arrayof rawstring (|> (λ $ as zarray) (unpack args))

test
    validate-args ProgramArguments 
        args> '( program Maria --show-age? )

# TEST: COMMANDS MODE
do
    enum RunModes plain
        timing-accurate
        fast

    struct CLICommandAssemble
        file : String
        output : String

        ParameterAliases := '[(file filename)]
        PositionalParameters := '(file output)

    struct CLICommandRun
        binary : String
        mode : (Option RunModes)

        PositionalParameters := '(binary)

    enum CLICommands
        # the command parameter name can go in the struct name
        assemble : CLICommandAssemble
        run : CLICommandRun

    vvv test
    validate-args CLICommands
        args> '( program assemble --filename program.asm --output=program.bin )

    vvv test
    validate-args CLICommands
        args> '( program run program.bin --mode=fast )

# Example that tests most features. It's not exactly like the gcc command line,
  and accepts some things that gcc wouldn't.
do
    enum GCCWarningLevel plain

    struct GCCArguments
        input-files : (Array String)
        output : (Option String)
        help : bool
        version : bool
        verbose : bool
        library : (Option (Array String))
        define-macro : (Option (Map String String))
        warning-level : (Option (Array GCCWarningLevel))

        ParameterShortNames := '[
            v verbose,
            l library,
            D define-macro,
            O optimization,
            W warning-level,
        ]

    # test
    #     validate-args GCCArguments \
    #         "fake-gcc" "-O2" "main.c" "stuff.c" "obj/stuff.o" "-o" "game.out" "-Wall" "-Wextra" \
    #         "-DIMPLEMENT_EVERYTHING" "-D"
    ()

# EXAMPLE PROGRAMS
do
    fn main (argc argv)
        local argparser : (ArgumentParser ProgramArguments)
        let args =
            try ('parse argparser argc argv)
            except (ex) 
                print "Error while parsing arguments:" ex.index ex.kind
                exit 1

        local message := copy args.name
        if args.show-age?
            try ('unwrap args.age)
            then (age) (message ..= f" is ${age} years old.")
            else (message ..= " is of unknown age.")
        else
            message ..= " decided not to share their age."
        print message
        0
    args := args> '( program --name Maria --show-age? )
    main (countof args) &args

()
