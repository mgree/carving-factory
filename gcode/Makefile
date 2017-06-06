test-gui : build-gui
	java GCodeReader parser_test4 500 500 5

build-gui : PrgLexer.tokens Line.class Command.class GCodeReader.class

test-parser : build-parser
	java PrgTest parser_test1.txt

build-parser : PrgLexer.tokens Command.class PrgTest.class

PrgLexer.tokens : Prg.g4
	java -jar /usr/local/lib/antlr-4.7-complete.jar $<
	javac Prg*.java

%.class : %.java
	javac $<

clean :
	-rm -f *.class *.tokens
	-rm -f PrgLexer.java PrgParser.java PrgBaseListener.java PrgListener.java
