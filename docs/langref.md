# Comments

```
# Comment until end of line

#* Block comment *#

#+ Nesting #+ Block Comment +# ... continues +#
```

# Literals

```
1 2.4 3E4 12.4E-5       # Double (default)
1d 1.0d 29.99D          # Decimal (BigDecimal, with suffix)

true                    # Booleans
false

null
undefined

"String"                # Quotes don't make a difference
'Also String'

"\r\n\t\b\"\'\`\$"      # Escapes
"\u0020"                # Unicode escape

`Result: ${ value } ${ unit }`     # Template strings

[ 1, 2, 3 ]                # List literal
[ a, b, *rest ]            # List spread operator

{                          # Map literals 
  key1: value1,            # Fixed key
  [ expr + key ]: value2,  # Expression key
  **otherMap               # Map spread operator
}
```

# Expressions

```
-expr             # unary minus
+expr             # unary plus
!expr             # boolean negation
++expr            # pre increment
--expr            # pre decrement

expr++            # post increment 
expr--            # post decrement
expr[index]       # element access
expr.field        # field access
expr?.field       # null-safe field access
expr.method(...)  # method call
funcName(...)     # function call

expr ** expr      # power operator

expr * expr       # multiplication
expr / expr       # division
expr // expr      # integer division
expr % expr       # remainder

expr + expr       # addition
expr - expr       # subtraction

expr..expr        # range, end inclusive
expr..<expr       # range, end exclusive

expr infix expr   # infix function call

expr ?: expr      # elvis operator

expr in expr      # in 
expr !in expr     # not in 
expr is expr      # is 
expr !is expr     # isn't

expr < expr == expr <= expr   # comparisons
expr >= expr > expr == expr   # can be chained
a != b != c != d              # all pairs must be distinct, a != b && a != c && a != d ...
expr <=> expr                 # starship / compareTo operator
expr === expr                 # strict equals
expr !== expr                 # strict not-equals

expr & expr       # conjunction
expr && expr      # short-circuit conjunction

expr | expr       # disjunction    
expr || expr      # short-circuit conjunction

cond ? ifTrue : ifFalse       # ternary operator

lvalue = expr     # assignment    
lvalue += expr    # increase by
lvalue -= expr    # decrease by
...               # and so on for most binary operators

```

# Statements

```
val foo = expr                   # Constant / single assignment
var bar = expr, bubu = expr      # Variable(s)

if (expr) {                      # If statement
  ...
} else {
  ...
}

loopName@                        # Optional loop identifier
while (expr) {                   # While statement
  ...
}

do {                             # Do while statement
  ...
} while (expr)

for (el in list) {               # List iteration
   ...
}
for ((key, value) in map) {      # Map iteration
   
}

break;                           # Loop control
break@loopName;
continue;
continue@loopName;

return expr;
```

# Functions

```
fun funcName(param1, param2 = defaultValue, *posArgs, **kwArgs) {
   ...
}

# posArgs receives all extra parameters without names
# kwArgs receives all extra parameters with names
```
