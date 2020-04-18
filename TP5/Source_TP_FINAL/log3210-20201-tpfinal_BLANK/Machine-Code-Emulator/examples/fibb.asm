// unsigned int fib(unsigned int n){
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
//    if (n <= 0)
//      return 0;
//    while (i > 0){
//      if (i % 2 == 1){
//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;
//      }
//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;
//    }
//    return a + b;
//  }

PRINT "Please enter the number of the fibonacci suite to compute:"
INPUT n

//    if (n <= 0)
//      return 0;
LD R0, n
BGTZ R0, validInput
PRINT #0
BR end

validInput:
//    unsigned int i = n - 1, a = 1, b = 0, c = 0, d = 1, t;
DEC R0
ST i, R0
ST a, #1
ST b, #0
ST c, #0
ST d, #1

//    while (i > 0){
beginWhile:
LD R0, i
BLETZ R0, printResult

//      if (i % 2 == 1){
MOD R0, R0, #2
DEC R0
BNETZ R0, afterIf

CLEAR

//        t = d*(b + a) + c*b;
//        a = d*b + c*a;
//        b = t;

// TODO:: PUT THE BLOCK 1 HERE !
LD @b, b
// Life_IN  : [@i]
// Life_OUT : [@b, @i]
// Next_IN  : 
// Next_OUT : @b:[2, 6, 9]

LD @a, a
// Life_IN  : [@b, @i]
// Life_OUT : [@a, @b, @i]
// Next_IN  : @b:[2, 6, 9]
// Next_OUT : @a:[2, 10], @b:[2, 6, 9]

ADD @t0, @b, @a
// Life_IN  : [@a, @b, @i]
// Life_OUT : [@a, @b, @i, @t0]
// Next_IN  : @a:[2, 10], @b:[2, 6, 9]
// Next_OUT : @a:[10], @b:[6, 9], @t0:[4]

LD @d, d
// Life_IN  : [@a, @b, @i, @t0]
// Life_OUT : [@a, @b, @d, @i, @t0]
// Next_IN  : @a:[10], @b:[6, 9], @t0:[4]
// Next_OUT : @a:[10], @b:[6, 9], @d:[4, 9], @t0:[4]

MUL @t1, @d, @t0
// Life_IN  : [@a, @b, @d, @i, @t0]
// Life_OUT : [@a, @b, @d, @i, @t1]
// Next_IN  : @a:[10], @b:[6, 9], @d:[4, 9], @t0:[4]
// Next_OUT : @a:[10], @b:[6, 9], @d:[9], @t1:[7]

LD @c, c
// Life_IN  : [@a, @b, @d, @i, @t1]
// Life_OUT : [@a, @b, @c, @d, @i, @t1]
// Next_IN  : @a:[10], @b:[6, 9], @d:[9], @t1:[7]
// Next_OUT : @a:[10], @b:[6, 9], @c:[6, 10], @d:[9], @t1:[7]

MUL @t2, @c, @b
// Life_IN  : [@a, @b, @c, @d, @i, @t1]
// Life_OUT : [@a, @b, @c, @d, @i, @t1, @t2]
// Next_IN  : @a:[10], @b:[6, 9], @c:[6, 10], @d:[9], @t1:[7]
// Next_OUT : @a:[10], @b:[9], @c:[10], @d:[9], @t1:[7], @t2:[7]

ADD @t3, @t1, @t2
// Life_IN  : [@a, @b, @c, @d, @i, @t1, @t2]
// Life_OUT : [@a, @b, @c, @d, @i, @t3]
// Next_IN  : @a:[10], @b:[9], @c:[10], @d:[9], @t1:[7], @t2:[7]
// Next_OUT : @a:[10], @b:[9], @c:[10], @d:[9], @t3:[8]

ADD @t, #0, @t3
// Life_IN  : [@a, @b, @c, @d, @i, @t3]
// Life_OUT : [@a, @b, @c, @d, @i, @t]
// Next_IN  : @a:[10], @b:[9], @c:[10], @d:[9], @t3:[8]
// Next_OUT : @a:[10], @b:[9], @c:[10], @d:[9], @t:[13]

MUL @t4, @d, @b
// Life_IN  : [@a, @b, @c, @d, @i, @t]
// Life_OUT : [@a, @c, @d, @i, @t, @t4]
// Next_IN  : @a:[10], @b:[9], @c:[10], @d:[9], @t:[13]
// Next_OUT : @a:[10], @c:[10], @t:[13], @t4:[11]

MUL @t5, @c, @a
// Life_IN  : [@a, @c, @d, @i, @t, @t4]
// Life_OUT : [@c, @d, @i, @t, @t4, @t5]
// Next_IN  : @a:[10], @c:[10], @t:[13], @t4:[11]
// Next_OUT : @t:[13], @t4:[11], @t5:[11]

ADD @t6, @t4, @t5
// Life_IN  : [@c, @d, @i, @t, @t4, @t5]
// Life_OUT : [@c, @d, @i, @t, @t6]
// Next_IN  : @t:[13], @t4:[11], @t5:[11]
// Next_OUT : @t:[13], @t6:[12]

ADD @a, #0, @t6
// Life_IN  : [@c, @d, @i, @t, @t6]
// Life_OUT : [@a, @c, @d, @i, @t]
// Next_IN  : @t:[13], @t6:[12]
// Next_OUT : @a:[14], @t:[13]

ADD @b, #0, @t
// Life_IN  : [@a, @c, @d, @i, @t]
// Life_OUT : [@a, @b, @c, @d, @i]
// Next_IN  : @a:[14], @t:[13]
// Next_OUT : @a:[14], @b:[15]

ST a, @a
// Life_IN  : [@a, @b, @c, @d, @i]
// Life_OUT : [@b, @c, @d, @i]
// Next_IN  : @a:[14], @b:[15]
// Next_OUT : @b:[15]

ST b, @b
// Life_IN  : [@b, @c, @d, @i]
// Life_OUT : [@c, @d, @i]
// Next_IN  : @b:[15]
// Next_OUT : 

// TODO:: END THE BLOCK 1 HERE ABOVE !

CLEAR

afterIf:
CLEAR

//      t = d*(2*c + d);
//      c = c*c + d*d;
//      d = t;
//      i = i / 2;

// TODO:: PUT THE BLOCK 2 HERE !
LD @c, c
// Life_IN  : [@a, @b]
// Life_OUT : [@a, @b, @c]
// Next_IN  : 
// Next_OUT : @c:[1, 6]

MUL @t0, #2, @c
// Life_IN  : [@a, @b, @c]
// Life_OUT : [@a, @b, @c, @t0]
// Next_IN  : @c:[1, 6]
// Next_OUT : @c:[6], @t0:[3]

LD @d, d
// Life_IN  : [@a, @b, @c, @t0]
// Life_OUT : [@a, @b, @c, @d, @t0]
// Next_IN  : @c:[6], @t0:[3]
// Next_OUT : @c:[6], @d:[3, 4, 7], @t0:[3]

ADD @t1, @t0, @d
// Life_IN  : [@a, @b, @c, @d, @t0]
// Life_OUT : [@a, @b, @c, @d, @t1]
// Next_IN  : @c:[6], @d:[3, 4, 7], @t0:[3]
// Next_OUT : @c:[6], @d:[4, 7], @t1:[4]

MUL @t2, @d, @t1
// Life_IN  : [@a, @b, @c, @d, @t1]
// Life_OUT : [@a, @b, @c, @d, @t2]
// Next_IN  : @c:[6], @d:[4, 7], @t1:[4]
// Next_OUT : @c:[6], @d:[7], @t2:[5]

ADD @t, #0, @t2
// Life_IN  : [@a, @b, @c, @d, @t2]
// Life_OUT : [@a, @b, @c, @d, @t]
// Next_IN  : @c:[6], @d:[7], @t2:[5]
// Next_OUT : @c:[6], @d:[7], @t:[10]

MUL @t3, @c, @c
// Life_IN  : [@a, @b, @c, @d, @t]
// Life_OUT : [@a, @b, @d, @t, @t3]
// Next_IN  : @c:[6], @d:[7], @t:[10]
// Next_OUT : @d:[7], @t:[10], @t3:[8]

MUL @t4, @d, @d
// Life_IN  : [@a, @b, @d, @t, @t3]
// Life_OUT : [@a, @b, @t, @t3, @t4]
// Next_IN  : @d:[7], @t:[10], @t3:[8]
// Next_OUT : @t:[10], @t3:[8], @t4:[8]

ADD @t5, @t3, @t4
// Life_IN  : [@a, @b, @t, @t3, @t4]
// Life_OUT : [@a, @b, @t, @t5]
// Next_IN  : @t:[10], @t3:[8], @t4:[8]
// Next_OUT : @t:[10], @t5:[9]

ADD @c, #0, @t5
// Life_IN  : [@a, @b, @t, @t5]
// Life_OUT : [@a, @b, @c, @t]
// Next_IN  : @t:[10], @t5:[9]
// Next_OUT : @c:[14], @t:[10]

ADD @d, #0, @t
// Life_IN  : [@a, @b, @c, @t]
// Life_OUT : [@a, @b, @c, @d]
// Next_IN  : @c:[14], @t:[10]
// Next_OUT : @c:[14], @d:[15]

LD @i, i
// Life_IN  : [@a, @b, @c, @d]
// Life_OUT : [@a, @b, @c, @d, @i]
// Next_IN  : @c:[14], @d:[15]
// Next_OUT : @c:[14], @d:[15], @i:[12]

DIV @t6, @i, #2
// Life_IN  : [@a, @b, @c, @d, @i]
// Life_OUT : [@a, @b, @c, @d, @t6]
// Next_IN  : @c:[14], @d:[15], @i:[12]
// Next_OUT : @c:[14], @d:[15], @t6:[13]

ADD @i, #0, @t6
// Life_IN  : [@a, @b, @c, @d, @t6]
// Life_OUT : [@a, @b, @c, @d, @i]
// Next_IN  : @c:[14], @d:[15], @t6:[13]
// Next_OUT : @c:[14], @d:[15], @i:[16]

ST c, @c
// Life_IN  : [@c, @d, @i]
// Life_OUT : [@d, @i]
// Next_IN  : @c:[14], @d:[15], @i:[16]
// Next_OUT : @d:[15], @i:[16]

ST d, @d
// Life_IN  : [@d, @i]
// Life_OUT : [@i]
// Next_IN  : @d:[15], @i:[16]
// Next_OUT : @i:[16]

ST i, @i
// Life_IN  : [@i]
// Life_OUT : []
// Next_IN  : @i:[16]
// Next_OUT : 

// TODO:: END THE BLOCK 2 HERE ABOVE!




// TODO:: This instruction is just a placeholder to let the code end, remove the code below!
//LD R0, i
//DEC R0
//ST i, R0
// TODO:: Remove the placeholder above of this line!

CLEAR
BR beginWhile

//    return a + b;
printResult:
LD R0, a
LD R1, b
ADD R0, R0, R1
PRINT R0

end:
PRINT "END"