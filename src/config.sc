using import .ext
do
    threading-available? := module-exists? sdl
    local-scope;
