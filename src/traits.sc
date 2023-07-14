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
                    self as otherT
        local-scope;
    local-scope;