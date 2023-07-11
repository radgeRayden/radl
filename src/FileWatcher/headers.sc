using import ..foreign

linux? := operating-system == 'linux
windows? := operating-system == 'windows
run-stage;

sugar-if linux?
    inotify :=
        do
            header := include "sys/inotify.h"
            ..
                filter-scope header.extern "^inotify_"
                filter-scope header.struct "^inotify_"
                filter-scope header.define "^(?=IN_)"
                do
                    IN_MOVE := header.define.IN_MOVED_FROM | header.define.IN_MOVED_TO
                    IN_CLOSE := header.define.IN_CLOSE_WRITE | header.define.IN_CLOSE_NOWRITE
                    local-scope;

    unistd := (include "unistd.h") . extern
    libgen :=
        do
            dirname := . (include "libgen.h") extern dirname
            basename := extern 'basename (function rawstring rawstring)
            local-scope;

    local-scope;
elseif windows?
    local-scope;
