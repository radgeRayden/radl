do
    vvv bind element-coerces-to-storage
    do
        inline __rimply (otherT thisT)
            static-if (otherT < pointer 
                        and ((elementof otherT) == (storageof (elementof thisT))))
                inline (incoming) 
                    incoming as thisT

        inline __imply (thisT otherT)
            static-if (otherT < pointer 
                        and ((elementof otherT) == (storageof (elementof thisT))))
                inline (self) 
                    (dupe self) as otherT
            else
                super-type := (superof (unqualified thisT))
                super-type.__imply thisT otherT

        inline __== (thisT otherT)
            static-if (imply? thisT otherT)
                inline (self other)
                    (imply (view self) otherT) == other
        local-scope;

    vvv bind coerces-to-storage
    do
        inline __rimply (otherT thisT)
            static-if (otherT == (storageof thisT))
                inline (incoming)
                    bitcast incoming thisT

        inline __imply (thisT otherT)
            static-if (otherT == (storageof thisT))
                inline (self)
                    bitcast (copy self) otherT

        inline __== (thisT otherT)
            static-if (imply? thisT otherT)
                inline (self other)
                    (imply (view self) other) == other
        local-scope;

    local-scope;
