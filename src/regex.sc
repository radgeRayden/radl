using import Array enum Option String struct slice
import remimu

enum RegexError plain
    InvalidPattern
    OutOfMemory

struct CaptureInfo
    position : (Array i64)
    length : (Array i64)

struct MatchInfo
    start : usize
    length : usize
    captures : (Array String)

struct RegexPattern
    tokens : (Array remimu.RegexToken)
    capture-info : CaptureInfo
    expected-captures : u16

    MinimumTokenStorage := 16

    inline __typecall (cls pattern)
        local tokens : (Array remimu.RegexToken)
        'resize tokens MinimumTokenStorage
        loop ()
            ptr count := 'data tokens
            local count = (i16 count)
            result := remimu.regex_parse pattern ptr &count 0
            switch result
            case -1
                raise RegexError.InvalidPattern
            case -2
                new-count := (countof tokens) * 2
                if ((i16 new-count) < 0)
                    raise RegexError.InvalidPattern
                'resize tokens (new-count * 2)
            default
                break;

        let expected-captures =
            fold (count = 0:u16) for token in tokens
                if (token.kind == remimu.REMIMU_KIND_OPEN)
                    count + 1
                else count

        local capture-info : CaptureInfo
        'resize capture-info.position expected-captures
        'resize capture-info.length expected-captures

        super-type.__typecall cls
            tokens = tokens
            capture-info = capture-info
            expected-captures = expected-captures

    fn match (self input)
        result := remimu.regex_match ('data self.tokens) input 0 self.expected-captures ('data self.capture-info.position) ('data self.capture-info.length) ()
        switch result
        case -1
            ((Option MatchInfo))
        case -2
            raise RegexError.OutOfMemory
        case -3
            raise RegexError.InvalidPattern
        default
            capture-info := self.capture-info
            local match-info = MatchInfo 0 (usize result)
            for i in (range self.expected-captures)
                start len := capture-info.position @ i, capture-info.length @ i
                'append match-info.captures
                    trim (slice (view input) (usize start) (usize (start + len)))
            (Option MatchInfo) match-info

fn match-one-shot (pattern input)
    pattern := (RegexPattern pattern)
    'match pattern input

do
    let RegexError RegexPattern match-one-shot
    local-scope;
