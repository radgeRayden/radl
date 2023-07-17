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
    windows :=
        do
            windows := include "windows.h"
            shlwapi := include "shlwapi.h"
            pathcch := include "pathcch.h"
            winerror := 
                foreign 
                    include "windows.h" "winerror.h"
                    with-constants {ERROR_SUCCESS ERROR_IO_INCOMPLETE}
            handleapi :=
                foreign (include "handleapi.h")
                    with-constants {INVALID_HANDLE_VALUE}
            ntstatus :=
                foreign 
                    include "windows.h" "ntstatus.h"
                    with-constants {STATUS_PENDING}
            .. windows.extern windows.define windows.typedef shlwapi.extern pathcch.extern \
                winerror handleapi ntstatus
    local-scope;
