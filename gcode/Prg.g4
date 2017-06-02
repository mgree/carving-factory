grammar Prg;

program : command+ ;

command : type natural arg* returnChar*;

type : 'G' | 'M' ;

arg : paramChar paramArg ;

paramChar: 'X' | 'Y' | 'Z' ;

returnChar : '\n' | '\r' ;

paramArg: floatNum ;

floatNum : sign? DIGIT + decimal? ;

decimal : '.' DIGIT*;

sign : '+' | '-' ;

natural : DIGIT+ ;

DIGIT : ('0' .. '9')  ;

WS  :  [ \t\u000C]+ -> skip ;

COMMENT : '(' ~[)] ')' -> skip ;
