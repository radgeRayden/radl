using import Array enum Map slice struct String radl.regex itertools Option radl.strfmt
from (import radl.String+) let starts-with? ASCII-tolower
from (import C.stdlib) let strtoll strtod
from (import UTF-8) let decoder char32

enum Argument
    Key : String
    Pair : String String
    Flag : i32
    Value : String

enum ArgumentParsingErrorKind plain
    IncompleteArgument
    ExpectedArgument
    UnexpectedArgument
    UnrecognizedFlag
    UnrecognizedParameter
    AlreadyProcessed
    MissingMandatoryParameter

    # data conversion errors
    UnsupportedIntegerType
    MalformedArgument
    UnrecognizedEnumValue

struct ArgumentParsingError
    index : i32
    kind : ArgumentParsingErrorKind
    name : (Option String)

# DESIGN NOTES
# every parameter resolves to a full name canonical named parameter. Positional, long and
  short named flags are linked to a corresponding field in the context struct, and their shape
  in the argument list works as a shortcut on how to set this property (eg. flags setting a
  parameter to `true` without an explicit value).
# rules for defining the parameter struct
# 1. named parameters are defined as fields in the struct. Numeric types, strings and 
    booleans are allowed. Named parameters are prefixed with `--` in the command line.
# 2. parameters defined as booleans are flags and may take no arguments (implying true). Only the 
    form `--flag=value` takes an argument. An argument following a flag not in this format will be
    interpreted as a parameter name or positional argument.
# 3. A scope called `ParameterShortNames` defines short single character names that alias full
    name parameters. In the case of flags, those can be combined to activate multiple flags at once.
    Combined flags never take arguments. Short names are prefixed with `-` in the command line.
# 4. A scope called `ParameterAliases` defines alternative names for existing named parameters.
# 5. Positional parameters must have a full name equivalent. The field `PositionalParameters` defines
    a symbol list. The position of the symbol in the list corresponds to its position in the arguments
    list. The symbol corresponds to the parameter full name (field in the struct).
# 6. If a field is defined as an Option, that is an optional parameter. It won't cause an error if
    it isn't found in the argument stream.

spice Symbol->String (sym)
    `[(sym as Symbol as string)]

spice collect-enum-fields (ET)
    using import Array .String+

    ET as:= type
    local args : (Array Symbol)

    # Scopes CEnum or C enum?
    try
        for arg in ('args ('@ ET '__fields__))
            'append args (('@ (arg as type) 'Name) as Symbol)
    else
        for k v in ('symbols ET)
            if (not (starts-with? (String (k as string)) "_"))
                'append args k

    sc_argument_list_map_new (i32 (countof args))
        inline (i)
            arg := args @ i
            `arg
run-stage;

@@ memo
inline collect-enum-fields (ET)
    collect-enum-fields ET

inline match-string-enum (ET value)
    using import hash .String+ switcher print
    tolower := ASCII-tolower

    call
        switcher sw
            va-map
                inline (k)
                    case (static-eval (hash (tolower (k as string))))
                        imply k ET
                collect-enum-fields ET
            default
                raise ArgumentParsingErrorKind.UnrecognizedEnumValue
        hash (tolower value)

spice convert-argument (value T)
    T as:= type

    if (T == bool)
        spice-quote
            match (ASCII-tolower value)
            case (or "true" "on" "yes" "1")
                true
            case (or "false" "off" "no" "0")
                false
            default
                raise ArgumentParsingErrorKind.MalformedArgument
    elseif (T < integer)
        if (('bitcount T) > 64) 
            error
                .. "Cannot convert incoming argument to very wide integer " (tostring T) "."
        spice-quote
            ptr count := 'data value
            local endptr : (mutable rawstring)
            result := strtoll ptr &endptr 0
            if (@endptr != 0)
                raise ArgumentParsingErrorKind.MalformedArgument
            result
    elseif (T < real)
        if (('bitcount T) > 64) 
            error
                .. "Cannot convert incoming argument to very wide real " (tostring T) "."
        spice-quote
            ptr count := 'data value
            local endptr : (mutable rawstring)
            result := strtod ptr &endptr
            if (@endptr != 0)
                raise ArgumentParsingErrorKind.MalformedArgument
            result
    elseif (T < CEnum)
        spice-quote
            match-string-enum T value
    elseif (T == String)
        spice-quote
            imply (copy value) String
    else
        error (.. "Could not convert incoming argument to struct field of type " (tostring T))

spice destructure-list (data)
    push-traceback data 'ProveExpression
    data as:= list
    if (empty? data)
        return (sc_argument_list_new 0 null)
    head rest := decons data
    if (head == 'square-list or head == 'curly-list)
        head rest := decons data
        `(unpack [(uncomma rest)])
    else
        `(unpack [(uncomma data)])

fn has-field? (T field)
    try (sc_prove `(getattr (nullof T) field)) true
    else false

spice check-alias (ctxT field msg-fragment)
    field as:= Symbol
    msg-fragment as:= string

    if (not (has-field? ctxT field))
        trace-error "while generating parameter map"
            f"parameter map contains ${msg-fragment} \"${field}\"" as string
    `()

run-stage;

@@ memo
inline ParameterMap (sourceT)
    struct (.. "ParameterMap<" (tostring sourceT) ">")
        ContextType := sourceT
        let ParameterFunction = 
            @ (raises
                (function void (viewof String) (mutable& (viewof ContextType)))
                ArgumentParsingErrorKind)

        struct NamedParameter
            name : String
            execute : ParameterFunction
            mandatory? : bool
            flag? : bool
            done? : bool

        named-parameters : (Map String NamedParameter)
        short-names : (Map i32 String)
        parameter-aliases : (Map String String)
        positional-parameters : (Array String)

        fn define-parameters (self)
            va-map
                inline (fT)
                    k T := keyof fT.Type, unqualified fT.Type
                    option? := T < Option
                    let flag? =
                        static-if option? (T.Type == bool)
                        else (T == bool)
                    name := String (Symbol->String k)
                    'set self.named-parameters (copy name)
                        typeinit
                            name = (copy name)
                            execute = 
                                fn "argv-handler" (value ctx) 
                                    let T =
                                        static-if option?
                                            T.Type
                                        else T
                                    raising ArgumentParsingErrorKind
                                    (getattr ctx k) = (convert-argument (view value) T) as T
                            mandatory? = not option?
                            flag? = flag?
                ContextType.__fields__

        inline map-over-metadata (metadata mapf)
            let tuples... =
                destructure-list
                    # if the list is not defined, do nothing
                    static-try (getattr ContextType metadata)
                    else '()
            va-map 
                inline (t)
                    static-if ((typeof t) == list)
                        mapf (unpack t)
                    else (mapf t)
                tuples...

        fn define-short-names (self)
            map-over-metadata 'ParameterShortNames
                inline (k v)
                    check-alias ContextType v "short name of undefined parameter"
                    let short-name long-name =
                        char32 (static-eval (k as Symbol as string))
                        Symbol->String v
                    'set self.short-names short-name (copy (String long-name))

        fn define-aliases (self)
            map-over-metadata 'ParameterAliases
                inline (...)
                    original aliases... := ...
                    check-alias ContextType original "alias for undefined parameter"
                    va-map
                        inline (alias)
                            'set self.parameter-aliases 
                                copy (String (Symbol->String alias))
                                copy (String (Symbol->String original))
                        aliases...

        fn define-positional-parameters (self)
            map-over-metadata 'PositionalParameters
                inline (param)
                    check-alias ContextType param "undefined positional parameter"
                    'append self.positional-parameters (copy (String (Symbol->String param)))

        inline __typecall (cls)
            local self := super-type.__typecall cls
            'define-parameters self
            'define-short-names self
            'define-aliases self
            'define-positional-parameters self
            self

        unlet map-over-metadata

typedef ArgumentParser
    inline __typecall (cls)
        bitcast none cls

    fn... parse (self, argc, argv : (@ rawstring))
        selfT := typeof self
        local ctx : selfT.ContextType
        local parameters : (ParameterMap selfT.ContextType)
        local arguments : (Array Argument)

        # canonicalize argument list
        local pair-pattern := try! (RegexPattern "^--(.+?)=(.+)$")
        for i in (range 1 argc)
            arg := 'from-rawstring String (argv @ i)
            try ('unwrap ('match pair-pattern arg))
            then (info)
                k v := info.captures @ 1, info.captures @ 2
                'append arguments (Argument.Pair (copy k) (copy v))
                continue;
            else ()

            if (starts-with? arg "--")
                'append arguments (Argument.Key (trim (rslice arg 2)))
                continue;

            if (starts-with? arg "-")
                flags := rslice arg 1
                ->> flags UTF-8.decoder
                    map ((c) -> ('append arguments (Argument.Flag c)))
                continue;

            'append arguments (Argument.Value arg)

        inline error (i kind)
            static-if ((typeof kind) == Symbol)
                raise (ArgumentParsingError i (getattr ArgumentParsingErrorKind kind))
            else
                raise (ArgumentParsingError i kind)

        inline get-arg (i)
            if (i >= (countof arguments))
                error i 'ExpectedArgument
            else
                arguments @ i

        inline get-flag (short-name)
            'get parameters.named-parameters ('get parameters.short-names short-name)

        loop (i = 0)
            if (i >= (countof arguments))
                break;

            inline process (param v i)
                if param.done?
                    error i 'AlreadyProcessed
                else (param.done? = true)
                try (param.execute v ctx)
                except (ex) (error i ex)

            dispatch (arguments @ i)
            case Key (k)
                # could be a named parameter or a long flag
                # check for aliases first, then fallback to indexing by name directly
                try ('get parameters.named-parameters ('getdefault parameters.parameter-aliases k k))
                then (param)
                    if param.flag?
                        process param "true" i
                        repeat (i + 1)

                    next := i + 1
                    value := get-arg next
                    if (('literal value) == Argument.Value.Literal)
                        inner := 'unsafe-extract-payload value String
                        process param inner i
                    else
                        error next 'IncompleteArgument
                    repeat (i + 2)
                else (error i 'UnrecognizedParameter)

            case Pair (k v)
                try ('get parameters.named-parameters k)
                then (param)
                    process param v i
                else (error i 'UnrecognizedParameter)
                i + 1
            case Flag (f)
                try (get-flag f)
                then (flag) (process flag "true" i)
                else (error i 'UnrecognizedFlag)
                i + 1
            case Value (v)
                if (empty? parameters.positional-parameters)
                    error i 'UnexpectedArgument
                name := 'remove parameters.positional-parameters 0
                try ('get parameters.named-parameters name)
                then (param)
                    process param v i
                    i + 1
                else (assert false)
            default
                assert false

        for k v in parameters.named-parameters
            if ((not v.done?) and v.mandatory?)
                raise
                    ArgumentParsingError
                        kind = 'MissingMandatoryParameter
                        name = (copy v.name)

        ctx

inline ArgumentParser (T)
    typedef (.. "ArgumentParser<" (tostring T) ">") < ArgumentParser : (storageof Nothing)
        ContextType := T
        ParameterMapType := (ParameterMap T)

do
    let ArgumentParser
    local-scope;
