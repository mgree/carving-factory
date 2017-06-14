CLASSPATH=antlr-4.7-complete.jar:.

test-gui : build-gui
	java -cp $(CLASSPATH) GCodeReader Heart.txt s8

build-gui : PrgLexer.tokens Line.class Arc.class Command.class GCodeReader.class

test-gen : build-gen
	java -cp $(CLASSPATH) GCodeGenerator

build-gen : PrgLexer.tokens Line.class Arc.class Command.class GCodeGenerator.class

test-parser : build-parser
	java PrgTest parser_test1.txt

build-parser : PrgLexer.tokens Command.class PrgTest.class

PrgLexer.tokens : Prg.g4
	java -jar antlr-4.7-complete.jar $<
	javac Prg*.java

%.class : %.java
	javac -cp $(CLASSPATH) $<

clean :
	-rm -f *.class *.tokens
	-rm -f PrgLexer.java PrgParser.java PrgBaseListener.java PrgListener.java
