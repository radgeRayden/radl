using import ..ext ..config

module-setup settings...
let settings... =
    _
        enable-threading? = threading-available?
        consolidate-events? = va-option consolidate-events? settings... true
        consolidation-interval = va-option consolidation-interval settings... 0.1:f64

sugar-if linux?
    (import .FileWatcherLinux) settings...
elseif windows?
    (import .FileWatcherWin32) settings...
else
    static-error "Unsupported OS"
