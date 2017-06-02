import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStreams;



import java.io.File;
import java.lang.System;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.lang.Object;
import java.io.*;

/*
Finish up making to num, to type, etc
Getting info out of various contexts
When that's possible, we integrate that with the GCodeReader
making the toCommand method create a command from all the different fields

Eventually deal with new lines in the parser file





 Goals and/or Tasks

 Get the program tester to the point that we know if what we're putting in
 is what we want
 Get the tokens out in a reasonable way
 Print them

 Make sure prg.g4 is as robust as it needs to be
 Add shit
 Test everything

 Once it's as good as it needs to be, start integration
 Make a new gcode reader that produces tokens instead of doing it manually
 Make a simulate function that successfully takes tokens and understands them
    enough to draw lines and print things from them



*/

public class PrgTest {

public static void main(String[] args) {
        if (args.length != 1) {
          System.err.println("Error: Please enter one file.");
          System.exit(-1);
        }

        CharStream input;
        try {
          input = CharStreams.fromFileName(args[0]);
          PrgLexer lexer = new PrgLexer(input);
          CommonTokenStream tokens = new CommonTokenStream(lexer);
          PrgParser parser = new PrgParser(tokens);


          PrgParser.ProgramContext ctx = parser.program();
          //System.out.println("Program Info: " + ctx.toInfoString(parser) + "\n");



          List<PrgParser.CommandContext> commandList = ctx.command();
          for (PrgParser.CommandContext c : commandList){
              //System.out.println("Command Info: " + c.toInfoString(parser));

              PrgParser.TypeContext type = c.type();
              //System.out.println("Type Info: " + type.toInfoString(parser));
              Token t = type.getToken(Token.MIN_USER_TOKEN_TYPE,0).getSymbol();
              System.out.println(t.getText());

              System.out.println("Modal number: " + toNum(c.natural()));

              List<PrgParser.ArgContext> argList = c.arg();
              for (PrgParser.ArgContext a : argList) {
                //Token paramCharacter = a.paramChar().getToken(Token.MIN_USER_TOKEN_TYPE,0).getSymbol();

                System.out.println("(" + a.paramChar().getText() + ", " + toFloat(a.paramArg().floatNum()) + ")");
              }


              List<TerminalNode> theTokens = c.getTokens(0);
              for (TerminalNode n : theTokens){
                System.out.println(n.getSymbol().getText());
              }
          }




          //
          // String src = CharStreams.fromFileName(args[0]);
          // PrgLexer lexer = new PrgLexer(src);
          // PrgParser parser = new PrgParser(new CommonTokenStream(lexer));
          // CommonTree tree = (CommonTree)parser.parse().getTree();
          // DOTTreeGenerator gen = new DOTTreeGenerator();
          // StringTemplate st = gen.toDOT(tree);
          // System.out.println(st);





        } catch (IOException e) {
          System.err.println("your file sucks");
          System.exit(-1);
        }
      }

      private static int toNum(PrgParser.NaturalContext nc) {
          List<TerminalNode> dcs = nc.DIGIT();

          StringBuffer digits = new StringBuffer(dcs.size());
          for (TerminalNode d : dcs) {
            digits.append(d.getSymbol().getText());
          }

          return Integer.parseInt(digits.toString());
      }


      private static float toFloat(PrgParser.FloatNumContext fnc) {
          List<TerminalNode> dcs = fnc.DIGIT(); //first part of num
          PrgParser.DecimalContext decctx = fnc.decimal();
          List<TerminalNode> fracdcs = decctx.DIGIT();

          StringBuffer digits = new StringBuffer(dcs.size());
          for (TerminalNode d : dcs) {
            digits.append(d.getSymbol().getText());
          }
          digits.append(".");
          for (TerminalNode f : fracdcs){
            digits.append(f.getSymbol().getText());
          }

          return Float.parseFloat(digits.toString());
      }


}
