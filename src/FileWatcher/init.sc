linux? := operating-system == 'linux
windows? := operating-system == 'windows
run-stage;

do
    let FileWatcher =
        sugar-if linux?
            import .FileWatcherLinux
        elseif windows?
            import .FileWatcherWin32
    local-scope;
