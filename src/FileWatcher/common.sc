using import enum
using import String

do
    enum FileEventType plain
        Modified
        Deleted
        Created

    enum FileWatchError plain
        NotFound
        AccessDenied
        NotAFile
        NotWatching

    FileWatchCallback := (@ (function void (viewof String 1) FileEventType))

    local-scope;