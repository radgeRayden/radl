using import Array Map String struct
using import .common ..config ..foreign

let StringT =
    sugar-if windows?
        using import .WideString
        WideStringView
    else
        String

time :=
    foreign (include "time.h")
        export {extern typedef struct}
        export {define} matching "^(?=CLOCK_)"

scopes-enable-fn-default-view-parameters;

struct TimestampedEvent plain
    time : f64
    event : FileEventType

fn to-seconds (t)
    (f64 t.tv_sec) + ((f64 t.tv_nsec) / 1000000000:f64)

fn get-time ()
    local ts : time.timespec
    time.clock_gettime time.CLOCK_MONOTONIC &ts
    to-seconds ts

struct EventQueue
    file-events : (Map StringT (Array TimestampedEvent))
    callbacks : (Map StringT FileWatchCallback)

    fn append-event (self path event cb)
        ts-event := TimestampedEvent (get-time) (copy event)
        try
            events := 'get self.file-events path
            'append events ts-event
            ()
        else
            'set self.file-events (copy path)
                (Array TimestampedEvent) ts-event
            'set self.callbacks (copy path) (dupe cb)

    fn consolidate-and-dispatch (self interval)
        local any-dispatched? : bool
        now := (get-time)
        for path events in self.file-events
            let cb =
                try ('get self.callbacks path)
                else (unreachable)

            local modified? : bool
            local processed : usize
            for i ev in (enumerate events)
                # we need to delay for `interval` so events can accumulate
                if (i == 0 and (now - ev.time) < interval)
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
                    if ((ev.time - prev-ev.time) < interval)
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
