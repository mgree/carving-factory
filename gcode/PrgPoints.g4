grammar PrgPoints;

drawing : point+ ;

point : 'PointF' '(' xCoord ',' yCoord ')';

xCoord : floatNum ;

yCoord : floatNum ;

floatNum : sign? DIGIT+ decimal? ;

decimal : '.' DIGIT*;

sign : '+' | '-' ;

DIGIT : ('0' .. '9')  ;

WS  :  [ \r\n\t\u000C]+ -> skip ;
