using import ..foreign include

linux? := operating-system == 'linux
windows? := operating-system == 'windows
run-stage;

sugar-if linux?
    inotify :=
        foreign (include "sys/inotify.h")
            export {extern struct} matching "^inotify_"
            export {define} matching "^(?=IN_)"
            with-constants { IN_MOVE IN_CLOSE }

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
