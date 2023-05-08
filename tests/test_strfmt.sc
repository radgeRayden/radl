using import String
using import testing

using import .strfmt

let ABC = 123
let CDE = 345
let str = "banana"

# print
#     repr f"ABC is ${ABC}, CDE is ${CDE}, and the sum is ${(+ ABC CDE)}. This other string is ${str}"
#     repr S"ABC is 123, CDE is 345, and the sum is 468. This other string is banana"

test
    ==
        format "hello %d %s %d world!" 1 "banana" 3
        "hello 1 banana 3 world!"

test
    ==
        report
            f"ABC is ${ABC}, CDE is ${CDE}, and the sum is ${(+ ABC CDE)}. This other string is ${str}"
        S"ABC is 123, CDE is 345, and the sum is 468. This other string is banana"

test
    ==
        f"there is nothing to interpolate here, so the string should remain unchanged."
        "there is nothing to interpolate here, so the string should remain unchanged."

test
    ==
        f"${ an expression start token without a corresponding end is ignored"
        "${ an expression start token without a corresponding end is ignored"

test-compiler-error
    f"however ${ if there's an end token at some point, the first start token is considered. ${}"

# let prefix:fAlt = (gen-interpolation-macro "abra" "cadabra")
# run-stage;
# test
#     ==
#         fAlt"this is not a very smart interpolation marker abravarargs...cadabra"
#         "this is not a very smart interpolation marker 1 2 3 4"
#

do
    local a : i32 10
    local b : i32 20

    test
        ==
            interpolate
                "${1} is the ${2} of ${0}, and also ${2}"
                "this"
                10
                do
                    print "this side effect should occur only once"
                    a * b
            "10 is the 200 of this, and also 200"

test
    ==
        va-format " " 1 2 3 4
        "1 2 3 4"

test
    ==
        f""""varargs work just fine if you va-format them... ${va-format " " 1 2 3 4}
        "varargs work just fine if you va-format them... 1 2 3 4\n"

fn join-str (a b)
    f"${a} ${b}"

test
    ==
        join-str "banana" "grape"
        "banana grape"

do
    local a : i32 1
    local b : i32 2
    test
        ==
            f"${a} + ${b} = ${a + b}"
            "1 + 2 = 3"

    a = 4
    b = 8
    test
        ==
            f"${a} + ${b} = ${a + b}"
            "4 + 8 = 12"
