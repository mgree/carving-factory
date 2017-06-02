test-parser : build-parser
	java PrgTest parser_test1.txt

build-parser : PrgLexer.tokens Params.class Command.class PrgTest.class

test-gui : build-gui
	java GCodeReader SimpleGCode.txt

build-gui : Line.class Params.class Command.class GCodeReader.class

PrgLexer.tokens : Prg.g4
	java -jar /usr/local/lib/antlr-4.7-complete.jar $<
	javac Prg*.java

%.class : %.java
	javac $<

clean :
	-rm -f *.class *.tokens
	-rm -f PrgLexer.java PrgParser.java PrgBaseListener.java PrgListener.java
