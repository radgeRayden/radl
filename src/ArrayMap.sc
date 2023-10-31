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

            ElementType := ET
            IndexType   := IndexType

            inline __countof (self)
                (countof self._items) - (countof self._vacant-slots)

            fn add (self item)
                if (empty? self._vacant-slots)
                    slot := (countof self._items) as u32
                    'append self._items item
                    'append self._slots self._generation
                    bitcast
                        VersionedIndex slot self._generation
                        IndexType
                else
                    slot := ('pop self._vacant-slots)
                    # overwrites undefined element at slot without dropping
                    assign item (self._items @ slot)
                    self._slots @ slot = self._generation
                    bitcast
                        VersionedIndex slot self._generation
                        IndexType

            fn in? (self id)
                id := storagecast id
                ((countof self._slots) > id.slot) and ((self._slots @ id.slot) == id.generation)

            fn remove (self id)
                id := storagecast id

                if ('in? self id)
                    self._generation += 1
                    if (self._generation == ~0:u32)
                        # ?

                    self._slots @ id.slot = ~0:u32
                    'append self._vacant-slots id.slot
                    # 'sort self._vacant-slots

                    # remove element and replace it with undefined data
                    'append self._items (dupe (undef ElementType))
                    'swap self._items ((countof self._items) - 1) id.slot
                    'pop self._items

            fn get (self id)
                if ('in? self id)
                    self._items @ ((storagecast id) . slot)
                else
                    raise ArrayMapError.KeyNotFound

            inline __drop (self)
                count := countof self._items

                # sort in descending order so we can pop the vacant slots without swapping with another vacant slot
                'sort self._vacant-slots
                    (x) -> (- x)

                for slot in self._vacant-slots
                    'swap self._items ((countof self._items) - 1) slot
                    # don't drop undefined element
                    lose ('pop self._items)

                super-type.__drop self

do
    let ArrayMap ArrayMapError
    local-scope;
