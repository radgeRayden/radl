using import Array enum hash property struct

struct VersionedIndex
    slot : u32
    generation : u32

    inline __hash (self)
        hash self.slot self.generation

enum ArrayMapError plain
    KeyNotFound

typedef ArrayMap
    @@ memo
    inline __typecall (cls ET)
        let IndexType =
            typedef (.. "ArrayMapIndex<" (tostring ET) ">") : (storageof VersionedIndex)
                inline __hash (self)
                    hash (storagecast self)

                inline __copy (self)
                    bitcast
                        copy (storagecast self)
                        this-type

                inline __== (thisT otherT)
                    static-if (thisT == otherT)
                        inline (self other)
                            (storagecast self) == (storagecast other)

        struct (.. "ArrayMap<" (tostring ET) ">")
            _items : (Array ET)
            _slots : (Array u32)
            _vacant-slots : (Array u32)
            _generation : u32
            _generation-outdated? : bool

            ElementType := ET
            IndexType   := IndexType

            inline __countof (self)
                (countof self._items) - (countof self._vacant-slots)

            inline update-generation (self)
                if self._generation-outdated?
                    self._generation += 1
                    self._generation-outdated? = false

                deref self._generation

            fn add (self item)
                if (empty? self._vacant-slots)
                    slot := (countof self._items) as u32

                    # if this is false, there are too many elements. Return fake key that doesn't point to anything.
                    if (slot < ~0:u32)
                        update-generation self
                        'append self._items item
                        'append self._slots self._generation

                    bitcast
                        VersionedIndex slot self._generation
                        IndexType
                else
                    update-generation self
                    slot := ('pop self._vacant-slots)
                    # overwrites undefined element at slot without dropping
                    assign item (self._items @ slot)
                    self._slots @ slot = self._generation
                    bitcast
                        VersionedIndex slot self._generation
                        IndexType

            unlet update-generation

            fn in? (self id)
                id := storagecast id
                ((countof self._slots) > id.slot) and ((self._slots @ id.slot) == id.generation)

            fn remove (self id)
                if ('in? self id)
                    id := storagecast id

                    self._generation-outdated? = true

                    if (id.slot == (countof self._slots))
                        'pop self._slots
                        'pop self._items
                    else
                        self._slots @ id.slot = ~0:u32
                        'append self._vacant-slots id.slot

                        # remove element and replace it with undefined data
                        'append self._items (dupe (undef ElementType))
                        'swap self._items ((countof self._items) - 1) id.slot
                        'pop self._items

            fn get (self id)
                if ('in? self id)
                    self._items @ ((storagecast id) . slot)
                else
                    raise ArrayMapError.KeyNotFound

            inline __as (cls T)
                static-if (T == Generator)
                    inline (self)
                        local vacant-slot-index = 0:usize
                        'sort self._vacant-slots

                        inline next-occupied-slot (start)
                            loop (idx = start)
                                if ((vacant-slot-index < (countof self._vacant-slots)) and (idx == (self._vacant-slots @ vacant-slot-index)))
                                    vacant-slot-index += 1
                                    idx + 1
                                else
                                    break idx

                        Generator
                            inline () (next-occupied-slot 0:u32)
                            inline (i)
                                i < (countof self._slots)
                            inline (i)
                                _
                                    bitcast (VersionedIndex i (self._slots @ i)) IndexType
                                    self._items @ i
                            inline (i)
                                next-occupied-slot (i + 1)

            inline __drop (self)
                count := countof self._items

                static-if (not (plain? ElementType))
                    # sort vacant slots in descending order
                    'sort self._vacant-slots ((x) -> ~x)
                    local vacant-slot-index = 0:usize

                    for i in (rrange (countof self._items))
                        if (vacant-slot-index < (countof self._vacant-slots) and (i == (self._vacant-slots @ vacant-slot-index)))
                            lose ('pop self._items)
                            vacant-slot-index += 1
                        else
                            'pop self._items

                super-type.__drop self

do
    let ArrayMap ArrayMapError
    local-scope;
