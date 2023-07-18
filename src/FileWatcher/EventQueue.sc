using import Array Map String struct
using import .common ..foreign

time :=
    foreign (include "time.h")
        export {extern typedef}
        with-constants {CLOCKS_PER_SEC}

scopes-enable-fn-default-view-parameters;

struct TimestampedEvent plain
    time : time.clock_t
    event : FileEventType

fn time-difference (end start)
    # FIXME: handle wrap around
    ((f64 end) / (f64 time.CLOCKS_PER_SEC)) - ((f64 start) / (f64 time.CLOCKS_PER_SEC))

struct EventQueue
    file-events : (Map String (Array TimestampedEvent))
    callbacks : (Map String FileWatchCallback)

    fn append-event (self path event cb)
        ts-event := TimestampedEvent (time.clock) (copy event)
        try
            events := 'get self.file-events path
            'append events ts-event
            ()
        else
            'set self.file-events (copy path)
                (Array TimestampedEvent) ts-event
            'set self.callbacks (copy path) cb

    fn consolidate-and-dispatch (self interval)
        local any-dispatched? : bool
        now := (time.clock)
        for path events in self.file-events
            let cb =
                try ('get self.callbacks path)
                else (unreachable)

            local modified? : bool
            local processed : usize
            for i ev in (enumerate events)
                # we need to delay for `interval` so events can accumulate
                if (i == 0 and (time-difference now ev.time) < interval)
                    break;

                # consolidate modified events into one
                inline dispatch-event (ev)
                    if (ev.event == FileEventType.Modified)
                        if (not modified?)
                            cb path FileEventType.Modified
                            modified? = true
                    else
                        cb path ev.event

                if (i > 0)
                    prev-ev := events @ (i - 1)
                    # group events by their distance in time. Only when you take `interval` without any events
                    # they will stop being grouped.
                    if ((time-difference ev.time prev-ev.time) < interval)
                        dispatch-event ev
                    else
                        # reset `modified` grouping
                        modified? = false
                        dispatch-event ev
                else
                    dispatch-event ev

                any-dispatched? = true
                processed += 1

            for i in (range processed)
                'remove events 0

        any-dispatched?

    fn forget-file (self path)
        'discard self.file-events path
        'discard self.callbacks path

do
    let EventQueue
    local-scope;
