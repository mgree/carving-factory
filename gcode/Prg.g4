grammar Prg;

program : command+ ;

command : type natural arg* returnChar*;

type : 'G' | 'M' | 'g' | 'm' ;

arg : paramChar paramArg ;

paramChar: 'A' | 'B' | 'C' | 'D' | 'F' | 'H' | 'I'
         | 'J' | 'K' | 'L' | 'N' | 'O' | 'P' | 'Q'
         | 'R' | 'S' | 'T' | 'U' | 'V' | 'W' | 'X'
         | 'Y' | 'Z'
         | 'a' | 'b' | 'c' | 'd' | 'f' | 'h' | 'i'
         | 'j' | 'k' | 'l' | 'n' | 'o' | 'p' | 'q'
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
