using import .ext
do
    linux? := operating-system == 'linux
    windows? := operating-system == 'windows
    threading-available? := module-exists? sdl
    local-scope;
