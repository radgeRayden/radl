using import ..ext ..config

module-setup settings...
let settings... =
    _
        enable-threading? = threading-available?
        settings...

sugar-if linux?
    (import .FileWatcherLinux) settings...
elseif windows?
    (import .FileWatcherWin32) settings...
else
    static-error "Unsupported OS"
