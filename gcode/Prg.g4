grammar Prg;

program : command+ ;

command : justArg | normalCmd | justLineNum;

justArg : returnChar* linenum? arg+ ;

justLineNum : returnChar* linenum;

normalCmd : returnChar* linenum* type natural arg* returnChar* ;

linenum: 'N' natural | 'n' natural ;

type : 'G' | 'M' | 'g' | 'm' ;

arg : paramChar paramArg ;


paramChar: 'A' | 'B' | 'C' | 'D' | 'F' | 'H' | 'I'
         | 'J' | 'K' | 'L' | 'O' | 'P' | 'Q'
         | 'R' | 'S' | 'T' | 'U' | 'V' | 'W' | 'X'
         | 'Y' | 'Z'
         | 'a' | 'b' | 'c' | 'd' | 'f' | 'h' | 'i'
         | 'j' | 'k' | 'l' | 'o' | 'p' | 'q'
         | 'r' | 's' | 't' | 'u' | 'v' | 'w' | 'x'
         | 'y' | 'z';

returnChar : '\n' | '\r' ;

paramArg: floatNum ;

floatNum : sign? DIGIT + decimal? ;

decimal : '.' DIGIT*;

sign : '+' | '-' ;

natural : DIGIT+ ;

DIGIT : ('0' .. '9')  ;

WS  :  [ \t\u000C]+ -> skip ;

COMMENT : '(' .*? ')' -> skip ;

ENDCHAR : '%' -> skip ;

EQUALS : '=' -> skip ;
