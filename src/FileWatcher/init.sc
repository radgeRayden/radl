linux? := operating-system == 'linux
windows? := operating-system == 'windows
run-stage;

sugar-if linux?
    import .FileWatcherLinux
elseif windows?
    import .FileWatcherWin32
