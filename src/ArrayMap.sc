using import Array enum hash struct

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
            _generation : u32
            _vacant-slot : u32

            ElementType := ET
            IndexType   := IndexType

            fn add (self item)
                slot := copy self._vacant-slot
                assert (slot <= (countof self._items))

                if (slot == (countof self._items))
                    self._vacant-slot += 1
                    'append self._items item
                    'append self._slots self._generation
                    bitcast
                        VersionedIndex slot self._generation
                        IndexType
                else
                    # overwrites undefined element at slot without dropping
                    assign item (self._items @ slot)
                    self._vacant-slot = (u32 (countof self._items))
                    self._slots @ slot = self._generation
                    bitcast
                        VersionedIndex slot self._generation
                        IndexType

            fn remove (self id)
                id := storagecast id
                if (((countof self._slots) > id.slot) and ((self._slots @ id.slot) == id.generation))
                    self._generation += 1
                    self._slots @ id.slot = self._generation
                    self._vacant-slot = id.slot
                    'append self._items (dupe (undef ElementType))
                    'swap self._items ((countof self._items) - 1) self._vacant-slot
                    'pop self._items

            fn get (self id)
                id := storagecast id
                if (((countof self._slots) > id.slot) and ((self._slots @ id.slot) == id.generation))
                    self._items @ id.slot
                else
                    raise ArrayMapError.KeyNotFound

            inline __drop (self)
                count := countof self._items
                if (self._vacant-slot < (countof self._items))
                    'swap self._items ((countof self._items) - 1) self._vacant-slot
                    # don't drop undefined element
                    lose ('pop self._items)

                super-type.__drop self

do
    let ArrayMap ArrayMapError
    local-scope;
