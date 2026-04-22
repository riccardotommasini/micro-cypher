grammar CypherSubset;

query
    : MATCH pattern (WHERE expression)? RETURN returnItems EOF
    ;

pattern
    : (variable EQ)? nodePattern patternChain*
    ;

patternChain
    : relationshipPattern nodePattern
    ;

nodePattern
    : LPAREN variable (COLON variable)? RPAREN
    ;

relationshipPattern
    : LEFT_ARROW relationshipDetail? DASH
    | DASH relationshipDetail? RIGHT_ARROW
    ;

relationshipDetail
    : LBRACK variable? (COLON variable)? rangeLiteral? RBRACK
    ;

rangeLiteral
    : STAR integer DOTDOT integer
    ;

returnItems
    : returnItem (COMMA returnItem)*
    ;

returnItem
    : expression
    ;

expression
    : primary (comparator primary)?
    ;

primary
    : propertyAccess
    | variable
    | stringLiteral
    | integer
    ;

propertyAccess
    : variable DOT variable
    ;

comparator
    : EQ
    | NEQ
    ;

variable
    : SYMBOLIC_NAME
    ;

stringLiteral
    : STRING
    ;

integer
    : INTEGER
    ;

MATCH : M A T C H;
WHERE : W H E R E;
RETURN : R E T U R N;

LEFT_ARROW : LT DASH;
RIGHT_ARROW : DASH GT;
EQ : '=';
NEQ : LT GT;
LPAREN : '(';
RPAREN : ')';
LBRACK : '[';
RBRACK : ']';
COLON : ':';
COMMA : ',';
DOT : '.';
DOTDOT : '..';
STAR : '*';
DASH : '-';
LT : '<';
GT : '>';

STRING
    : '\'' ( ~['\\] | '\\' . )* '\''
    ;

INTEGER
    : DIGIT+
    ;

SYMBOLIC_NAME
    : LETTER (LETTER | DIGIT | '_')*
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

fragment LETTER : [a-zA-Z];
fragment DIGIT : [0-9];
fragment A : [aA];
fragment C : [cC];
fragment E : [eE];
fragment H : [hH];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment R : [rR];
fragment T : [tT];
fragment U : [uU];
fragment W : [wW];
