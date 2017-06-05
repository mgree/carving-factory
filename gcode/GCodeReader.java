/*
    More features
        Think about how to implement arcs, etc
        add extra command line arguments for drill size and dimensions
        add this into the swing panel
        Deal with scaling and errors
          calculate the existing error
          create error threshold var
          from there calculate required scale factor
          compare to dimensions
          output results with scale if able, else report that these
            dimensions can't be used to create an image of high enough fidelity


   Check these cases:
        multiple G commands on one line
        Weird start commands (s, t)
        Negative x and y values
 */

/*
 * Gcode Reader -- designed to take a GCode file and display a GUI of
 * the expected pattern to be drawn. It also outputs all instructions
 * it processes in an easy to read format in the terminal window
 *
 * @author Sonia Grunwald
 * @version June 5th, 2017
 * Michael Greenberg Lab
 *
 */

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStreams;

import java.lang.System;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.io.*;
import java.lang.*;
import java.util.*;

import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;




public class GCodeReader {
public static ArrayList<Line> lines = new ArrayList<Line> ();
private static final int SCALE = 1;
private static final int MAX_LINE_SIZE = 300;
private static HashMap<Character,Double> parameters = new HashMap<Character,Double>();
private static HashMap<String,Boolean> processable = new HashMap<String,Boolean>();


private static double maxX = 0.0;
private static double minX = 0.0;
private static double maxY = 0.0;
private static double minY = 0.0;
private static double maxZ = 0.0;
private static double minZ = 0.0;
private static double prevX = 0.0;
private static double prevY = 0.0;
private static double prevZ = 0.0;



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

                //Set up parameters / processable commands
                addAllParams(parameters);
                addAllProcessable(processable);

                //Get the program context and create a series of commands from it
                //Process each of these commands in turn
                PrgParser.ProgramContext ctx = parser.program();
                List<PrgParser.CommandContext> commandList = ctx.command();
                for (PrgParser.CommandContext c : commandList) {
                        Command nextCommand = createCommand(c);
                        processCommand (nextCommand);
                }

                //Print the X, Y, and Z ranges
                System.err.println ("X Range: (" + minX + ", " + maxX +
                                    ") \nY Range: (" + minY + ", " + maxY +
                                    ") \nZ Range: (" + minZ + ", " + maxZ +")");

        } catch (IOException e) {
                System.err.println("IOException: " + e);
                System.exit(-1);
        }

        //Show the GUI
        SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                createAndShowGUI();
                        }
                });
}


//Add all parameters to the hashmap with their default values
public static void addAllParams(HashMap<Character,Double> hm){
        //SET UP DEFAULT VALUES
        //Note: I have no idea what these should be whatsoever.
        hm.put('A', 0.0); //A-axis of machine
        hm.put('B', 0.0); //B-axis of machine
        hm.put('C', 0.0); //C-axis of machine
        hm.put('D', 0.0); //tool radius compensation number
        hm.put('F', 1.0); //feedrate
        hm.put('H', 0.0); //tool length offset index
        hm.put('I', 0.0); //X-axis offset for arcs
        hm.put('J', 0.0); //Y-axis offset for arcs
        hm.put('K', 0.0); //Z-axis offset for arcs
        hm.put('L', 0.0); //Numer of repetitions in canned cycles, key for G10
        hm.put('O', 0.0); //Subroutine label number
        hm.put('P', 0.0); //Dwell time, key for G10
        hm.put('Q', 0.0); //Feed increment in G83 canned cycle, repetitions of subroutine call
        hm.put('R', 1.0); //arc radius
        hm.put('S', 1.0); //spindle speed
        hm.put('T', 0.0); //tool selection
        hm.put('X', 0.0); //X-axis of machine
        hm.put('Y', 0.0); //Y-axis of machine
        hm.put('Z', 0.0); //Z-axis of machine
}

//Add all processable commands to the hashmap for easy access
//In the future, all commands will be processable so this will be
//unnecessary
public static void addAllProcessable(HashMap<String,Boolean> hm){
        hm.put("G0", true);
        hm.put("G1", true);
}



//Method creates a command from a CommandContext
public static Command createCommand(PrgParser.CommandContext c){

        Command newCommand = new Command();

        //get the type and set the type to that character
        //(For unclear reasons, using tokens here made things mor difficult)
        PrgParser.TypeContext type = c.type();
        newCommand.setType(Character.toUpperCase(type.getText().charAt(0)));

        //get the mode and set it
        newCommand.setMode(toNum(c.natural()));

        prevX = parameters.get('X');
        prevY = parameters.get('Y');
        prevZ = parameters.get('Z');

        //Get TypeMode string
        StringBuilder s = new StringBuilder();
        s.append (Character.toUpperCase(type.getText().charAt(0)));
        s.append (toNum(c.natural()));

        //ONLY PROCESS A COMMAND (change params, etc) IF IT IS KNOWN
        if (processable.containsKey(s.toString())) {
                //For all the arguments, update that parameter in the hashmap
                List<PrgParser.ArgContext> argList = c.arg();
                for (PrgParser.ArgContext a : argList) {

                        Character charCode = Character.toUpperCase(a.paramChar().getText().charAt(0));
                        Double paramValue = (double)toFloat(a.paramArg().floatNum());

                        parameters.put (charCode, paramValue);

                        //in the case of x, y, and z
                        //keep track of maximum and minimum values
                        if(charCode == 'X') {
                                if (paramValue < minX) {
                                        minX = paramValue;
                                } else if (paramValue > maxX) {
                                        maxX = paramValue;
                                }
                        }

                        if(charCode == 'Y') {
                                if (paramValue < minY) {
                                        minY = paramValue;
                                } else if (paramValue > maxY) {
                                        maxY = paramValue;
                                }
                        }

                        if(charCode == 'Z') {
                                if (paramValue < minZ) {
                                        minZ = paramValue;
                                } else if (paramValue > maxZ) {
                                        maxZ = paramValue;
                                }
                        }
                }
        }

        return newCommand;
}


//Processes the command
//(adds lines to the list of lines to be drawn and prints out updates of what
//each command does)
public static void processCommand(Command c){

        char type = c.getType();
        int mode = c.getMode();

        double X = parameters.get('X');
        double Y = parameters.get('Y');
        double Z = parameters.get('Z');

        //execute something according to the type and mode
        //here write to a file or print an update (added line from (x,y) to (x,y))
        if (type == 'G') {
                switch (mode) {
                case 0:
                        lines.add(new Line (prevX, prevY, X, Y));
                        System.err.println("Line drawn: ("+ prevX + ", " + prevY + ", " +
                                           prevZ + "), (" + X + ", " + Y + ", " + Z +
                                           ") at feed rate " + parameters.get('F') );
                        break;
                case 1:
                        lines.add(new Line (prevX, prevY, X, Y));
                        System.err.println("Line drawn: ("+ prevX + ", " + prevY + ", " +
                                           prevZ + "), (" + X + ", " + Y + ", " + Z +
                                           ") at feed rate " + parameters.get('F') );
                        break;
                case 2:
                        System.err.println("A clockwise arc should be drawn with radius "
                                           + parameters.get('R'));
                        break;
                case 3:
                        System.err.println ("A counterclockwise arc should be drawn with radius "
                                            + parameters.get('R'));
                        break;
                case 4:
                        System.err.println("Dwell for " + parameters.get('P') + "time");
                        break;

                case 20:
                        System.err.println("Change units to inches");
                        break;

                case 21:
                        System.err.println("Change units to millimeters");
                        break;

                case 30:
                        System.err.println("Return Home");
                        parameters.put ('X', 0.0);
                        parameters.put('Y', 0.0);
                        parameters.put ('Z', 0.0);
                        break;

                case 49:
                        System.err.println("Cancel tool length offset");
                        break;

                default:
                        System.err.println ("Command not processed. Command: " + type + mode);
                }


        } else if (type == 'M') {
                switch (mode) {
                default:
                        System.err.println ("Command not processed. Command: " + type + mode);
                }
        } else {
                System.err.println ("Error. Type: " + type);
        }
}

//Create the GUI
private static void createAndShowGUI() {
        JFrame f = new JFrame("GCode Results");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new MyPanel(lines));
        f.pack();
        f.setVisible(true);
}

/*
 * Private helpers for the command context to command transformation
 */
//Creates an int from a NaturalContext
private static int toNum(PrgParser.NaturalContext nc) {
        List<TerminalNode> dcs = nc.DIGIT();

        StringBuffer digits = new StringBuffer(dcs.size());
        for (TerminalNode d : dcs) {
                digits.append(d.getSymbol().getText());
        }

        return Integer.parseInt(digits.toString());
}

//Creates a float from a FloatNumContext
private static float toFloat(PrgParser.FloatNumContext fnc) {

        List<TerminalNode> dcs = fnc.DIGIT(); //first part of num

        StringBuffer digits = new StringBuffer(dcs.size());
        for (TerminalNode d : dcs) {
                digits.append(d.getSymbol().getText());
        }

        PrgParser.DecimalContext decctx = fnc.decimal();
        if (decctx != null) {
                List<TerminalNode> fracdcs = decctx.DIGIT(); //second part of num
                digits.append(".");
                for (TerminalNode f : fracdcs) {
                        digits.append(f.getSymbol().getText());
                }
        }
        return Float.parseFloat(digits.toString());
}


}




//The panel sets up adn displays the GUI in the end
class MyPanel extends JPanel {
ArrayList<Line> theLines;

public MyPanel(ArrayList<Line> j) {
        setBorder(BorderFactory.createLineBorder(Color.black));
        theLines = j;
}

public Dimension getPreferredSize() {
        return new Dimension(250,200);
}

protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (Line l : theLines) {
                g.drawLine((int)l.getStartX(), (int)l.getStartY(), (int)l.getEndX(), (int)l.getEndY());
        }

}
}
