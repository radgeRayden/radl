using import hash Map

typedef CacheKey <: hash
    inline __hash (self)
        imply self hash

    inline __rimply (otherT thisT)
        static-if (otherT == hash)
            inline (incoming)
                bitcast incoming thisT
        else
            inline (incoming)
                let k =
                    static-if (constant? incoming)
                        static-eval (hash incoming)
                    else
                        hash incoming

                imply k thisT

    inline __imply (thisT otherT)
        static-if (otherT == hash)
            inline (self)
                bitcast self hash

typedef Cache
    @@ memo
    inline __typecall (cls T)
        MapType := (Map CacheKey T)

        typedef (.. "Cache<" (tostring T) ">") <:: MapType
            inline get (self k f args...)
                try (copy (MapType.get self (imply (view k) CacheKey)))
                else
                    new-value := f args...
                    'set self (imply (view k) CacheKey) (copy new-value)
                    new-value

do
    let Cache
    local-scope;
