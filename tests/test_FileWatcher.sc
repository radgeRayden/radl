using import String

vvv using
(import FileWatcher) (consolidate-events? = true)

global fw : FileWatcher
try
    'watch fw (String (module-dir .. "/test.txt"))
        fn (...)
            print ...
except (ex)
    print ex

loop (event-count = 0)
    if (event-count == 10)
        break;

    if ('dispatch-events fw)
        print "event" event-count
        event-count + 1
    else
        event-count

'unwatch fw (String (module-dir .. "/test.txt"))
