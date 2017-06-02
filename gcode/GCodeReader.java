/*
    Things the parser has trouble with:
        line numbers
        floats that without decimals

    Things to fix in addition
        don't use a switch statement, make it a hashmap (type, value)

    More features
        Think about how to implement arcs, report feedrates, etc
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
import java.lang.Object;
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

    //SET UP DEFAULT VALUES
    //Note: I have no idea what these should be whatsoever.
    private static double A = 0.0; //A-axis of machine
    private static double B = 0.0; //B-axis of machine
    private static double C = 0.0; //C-axis of machine
    private static double D = 0.0; //tool radius compensation number
    private static double F = 1.0; //feedrate
    private static double H = 0.0; //tool length offset index
    private static double I = 0.0; //X-axis offset for arcs
    private static double J = 0.0; //Y-axis offset for arcs
    private static double K = 0.0; //Z-axis offset for arcs
    private static double L = 0.0; //Numer of repetitions in canned cycles, key for G10
    private static double O = 0.0; //Subroutine label number
    private static double P = 0.0; //Dwell time, key for G10
    private static double Q = 0.0; //Feed increment in G83 canned cycle, repetitions of subroutine call
    private static double R = 1.0; //arc radius
    private static double S = 1.0; //spindle speed
    private static double T = 0.0; //tool selection
    //private static double U = 0.0; synonymous with A
    //private static double V = 0.0; synonymous with B
    //private static double W = 0.0; synonymous with C
    private static double X = 0.0; //X-axis of machine
    private static double Y = 0.0; //Y-axis of machine
    private static double Z = 0.0; //Z-axis


    private static double maxX = 0.0;
    private static double minX = 0.0;
    private static double maxY = 0.0;
    private static double minY = 0.0;
    private static double maxZ = 0.0;
    private static double minZ = 0.0;




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

                //Get the program context and create a series of commands from it
                //Process each of these commands in turn
                //Could separate this into two steps easily
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



//Method creates a command from a CommandContext
public static Command createCommand(PrgParser.CommandContext c){

        Command newCommand = new Command();

        //get the type token and set the type to that character
        PrgParser.TypeContext type = c.type();
        Token t = type.getToken(Token.MIN_USER_TOKEN_TYPE,0).getSymbol();
        newCommand.setType(Character.toUpperCase(t.getText().charAt(0)));

        //get the mode and set it
        newCommand.setMode(toNum(c.natural()));

        //For all the arguments, create a new parameter pairing and add it to the list
        List<PrgParser.ArgContext> argList = c.arg();
        for (PrgParser.ArgContext a : argList) {
                Params newParam = new Params(Character.toUpperCase(a.paramChar().getText().charAt(0)),
                                             (double)toFloat(a.paramArg().floatNum()));
                newCommand.addParam(newParam);
        }

        return newCommand;
}


//Processes the command, setting the parameters to the appropriate values
public static void processCommand(Command c){

        ArrayList<Params> theParams = c.getParams();
        char type = c.getType();
        int mode = c.getMode();

        //for lines, save original x and y, then update
        double oldx = X;
        double oldy = Y;
        double oldz = Z;


        //execute something according to the type and mode
        //Only update the parameters if it's a type you know how to process
        //use a switch statement allowing for additional commands to be added later
        //here write to a file or print an update (added line from (x,y) to (x,y))
        //in the future, may paint directly from here. May allow painting of arcs to
        //be somewhat simpler

        if (type == 'G') {
                switch (mode) {
                case 0:
                        updateParameters(theParams);
                        lines.add(new Line (oldx, oldy, X, Y));
                        System.err.println("Line drawn: ("+ oldx + ", " + oldy + ", " +
                                           oldz + "), (" + X + ", " + Y + ", " + Z +
                                           ") at feed rate " + F );
                        oldx = X;
                        oldy = Y;
                        oldz = Z;
                        break;
                case 1:
                        updateParameters(theParams);
                        lines.add(new Line (oldx, oldy, X, Y));
                        System.err.println("Line drawn: ("+ oldx + ", " + oldy + ", " +
                                           oldz + "), (" + X + ", " + Y + ", " + Z +
                                           ") at feed rate " + F );
                        oldx = X;
                        oldy = Y;
                        oldz = Z;
                        break;
                case 2:
                        //Should I update the params here even if I'm not yet drawing arcs?
                        System.err.println("A clockwise arc should be drawn with radius " + R);
                        break;
                case 3:
                        System.err.println ("A counterclockwise arc should be drawn with radius " + R);
                        break;
                case 4:
                        System.err.println("Dwell for " + P + "time");
                        break;

                case 20:
                        System.err.println("Change units to inches");
                        break;

                case 21:
                        System.err.println("Change units to millimeters");
                        break;

                case 30:
                        System.err.println("Return Home");
                        X = 0.0;
                        Y = 0.0;
                        Z = 0.0;
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



//Method updates the parameters according to an arraylist of new parameters
//Hopefully this will get scrapped and replaced with a simple hashmap
private static void updateParameters (ArrayList<Params> theParams) {
        //for all the parameters in the command, set those appropriately
        for (Params p : theParams) {
                char type = p.getType();
                double value = p.getValue();

                switch (type) {

                case 'A':
                        A = value;
                        break;
                case 'B':
                        B = value;
                        break;
                case 'C':
                        C = value;
                        break;
                case 'D':
                        D = value;
                        break;
                case 'F':
                        F = value;
                        break;
                case 'H':
                        H = value;
                        break;
                case 'I':
                        I = value;
                        break;
                case 'J':
                        J = value;
                        break;
                case 'K':
                        K = value;
                        break;
                case 'L':
                        L = value;
                        break;
                case 'O':
                        O = value;
                        break;
                case 'P':
                        P = value;
                        break;
                case 'Q':
                        Q = value;
                        break;
                case 'R':
                        R = value;
                        break;
                case 'S':
                        S = value;
                        break;
                case 'T':
                        T = value;
                        break;
                case 'U':
                        A = value;
                        break;
                case 'V':
                        B = value;
                        break;
                case 'W':
                        C = value;
                        break;
                case 'X':
                        X = value;
                        break;
                case 'Y':
                        Y = value;
                        break;
                case 'Z':
                        Z = value;
                        break;
                default:
                        System.err.println("Parameter type not found. Type: " + type);
                }


                //in the case of x, y, and z
                //keep track of maximum and minimum values
                if(X < minX) {
                        minX = X;
                } else if (X > maxX) {
                        maxX = X;
                }

                if(Y < minY) {
                        minY = Y;
                } else if (Y > maxY) {
                        maxY = Y;
                }

                if(Z < minZ) {
                        minZ = Z;
                } else if (Z > maxZ) {
                        maxZ = Z;
                }


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
        PrgParser.DecimalContext decctx = fnc.decimal();
        List<TerminalNode> fracdcs = decctx.DIGIT();

        StringBuffer digits = new StringBuffer(dcs.size());
        for (TerminalNode d : dcs) {
                digits.append(d.getSymbol().getText());
        }
        digits.append(".");
        for (TerminalNode f : fracdcs) {
                digits.append(f.getSymbol().getText());
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
