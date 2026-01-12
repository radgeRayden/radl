using import argv strfmt struct Option String
struct ProgramArguments
    name : String
    age : (Option i32)
    show-age? : bool

    PositionalParameters := '(name)

using import testing

inline validate-args (T args...)
    local args = arrayof rawstring args...
    argc argv := countof args, &args
    argparser := ((ArgumentParser T))
    try ('parse argparser argc argv) true
    except (ex)
        report "Error while parsing arguments:" ex.index ex.kind
        false

test (validate-args ProgramArguments "program" "Maria" "--show-age?")

# EXAMPLE PROGRAM
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
    local args = arrayof rawstring "program" "--name" "Maria" "--show-age?"
    main (countof args) &args

()
