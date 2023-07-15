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
            header := include "windows.h"
            shlwapi := include "shlwapi.h"
            pathcch := include "pathcch.h"
            # winnt := include "winnt.h"
            handleapi :=
                include 
                    """"#include <handleapi.h>
                        typeof(INVALID_HANDLE_VALUE) __scopes_INVALID_HANDLE_VALUE () {
                            return INVALID_HANDLE_VALUE;
                        }
            'bind-symbols
                .. header.extern header.define header.typedef shlwapi.extern pathcch.extern
                INVALID_HANDLE_VALUE = (handleapi.extern.__scopes_INVALID_HANDLE_VALUE)
    local-scope;
