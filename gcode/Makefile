CLASSPATH=antlr-4.7-complete.jar:.

test-curves : build-curves
	java -cp $(CLASSPATH) CurveFitting points_simplified.txt

build-curves: PrgPointsLexer.tokens CurveFitting.class CurvePointsList.class

test-gui : build-gui
	java -cp $(CLASSPATH) GCodeReader Heart.txt s8

build-gui : PrgLexer.tokens Line.class Arc.class Command.class GCodeReader.class

test-gen : build-gen
	java -cp $(CLASSPATH) GCodeGenerator

build-gen : PrgLexer.tokens Line.class Arc.class Command.class GCodeGenerator.class

PrgLexer.tokens : Prg.g4
	java -jar antlr-4.7-complete.jar $<
	javac Prg*.java

PrgPointsLexer.tokens : PrgPoints.g4
		java -jar antlr-4.7-complete.jar $<
		javac PrgPoints*.java

%.class : %.java
	javac -cp $(CLASSPATH) $<

clean :
	-rm -f *.class *.tokens
	-rm -f PrgLexer.java PrgParser.java PrgBaseListener.java PrgListener.java
	-rm -f PrgPointsLexer.java PrgPointsParser.java PrgPointsBaseListener.java PrgPointsListener.java
