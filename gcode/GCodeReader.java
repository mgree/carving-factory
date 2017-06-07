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

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.*;



public class GCodeReader {

private static final int MAX_X_DIM = 1300;
private static final int MAX_Y_DIM = 725;

private static ArrayList<Line> lines = new ArrayList<Line> ();
private static ArrayList<Arc> arcs = new ArrayList<Arc>();
private static HashMap<Character,Double> parameters = new HashMap<Character,Double>();
private static HashMap<String,Boolean> processable = new HashMap<String,Boolean>();
private static HashMap<Integer,Double> toolLibrary = new HashMap<Integer,Double>();
    //maps the tool number to its bit size

//This is an additional scale to be used in debugging designs
//This allows the user to manually scale their design up and down
//They will then use this information to alter their gcode
private static double scale = 1.0;

//This is the automatically detected scale factor used to display
//the lines proportionally according to the given dimensions
private static double scaleFactor = 1.0;

//ACTUAL DIMENSIONS (px)
private static int pxDimX;
private static int pxDimY;

//default window is a 2' X 4' window
//THESE ARE THE INPUTTED DIMENSIONS (inches)
private static int dimX = 48;
private static int dimY = 24;

//Set up the default tool width
private static double toolWidth = .5;

// //default stroke width for the drawing pane
// //This is updated to appear proportional to the given dimensions
// private static double strokeWidth = .5;

//Initialize min and max values
private static double maxX = 0.0;
private static double minX = 0.0;
private static double maxY = 0.0;
private static double minY = 0.0;
private static double maxZ = 0.0;
private static double minZ = 0.0;

//Initialize starting point
private static double prevX = 0.0;
private static double prevY = 0.0;
private static double prevZ = 0.0;

//Initialize arc centers
private static double prevI = 0.0;
private static double prevJ = 0.0;
private static double prevK = 0.0;
private static double prevR = 1.0;

public static void main(String[] args) {
        //Wrong number of arguments
        if (args.length > 4 || args.length < 1) {
                System.err.println("Error: Please enter one file and two optional dimensions (in inches) and/or an optional scale factor");
                System.exit(-1);
        }
        try{
                //if there is 1 additional arg then a scale has been entered
                if (args.length == 2) {
                        if (args[1].charAt(0) == 's' || args[1].charAt(0) == 'S') {
                                scale = Double.parseDouble(args[1].substring(1));
                        }  else {
                                System.err.println("Enter a scale preceded by an 's' or two dimensions");
                                System.exit(-1);
                        }
                }
                //if there are 2 args then only dimensions have been inputted
                if (args.length == 3) {
                        dimX = (int)Math.round(Double.parseDouble(args[1]));
                        dimY = (int)Math.round(Double.parseDouble(args[2]));
                }
                //if there are 3 args then both dimensions and a scale have been entered
                if (args.length == 4) {
                        dimX = (int)Math.round(Double.parseDouble(args[1]));
                        dimY = (int)Math.round(Double.parseDouble(args[2]));

                        if (args[3].charAt(0) == 's' || args[3].charAt(0) == 'S') {
                                scale = Double.parseDouble(args[3].substring(1));
                        } else {
                                System.err.println("Enter a scale preceded by an 's'");
                                System.exit(-1);
                        }
                }

        }  catch (NumberFormatException e) {
                System.err.println("A number was entered incorrectly");
                System.exit(-1);
        }

        CharStream input;
        try {
                scaleFactor = findScaleFactor(dimX, dimY);
                // strokeWidth = toolWidth;
                pxDimX = (int)(scaleFactor * dimX);
                pxDimY = (int)(scaleFactor * dimY);

                //Parse document
                input = CharStreams.fromFileName(args[0]);
                PrgLexer lexer = new PrgLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                PrgParser parser = new PrgParser(tokens);

                //Set up parameters / processable commands / tool library
                addAllParams(parameters);
                addAllProcessable(processable);
                makeToolLibrary(toolLibrary);

                //Get the program context and create a series of commands from it
                //Process each of these commands in turn
                PrgParser.ProgramContext ctx = parser.program();
                List<PrgParser.CommandContext> commandList = ctx.command();
                for (PrgParser.CommandContext c : commandList) {
                        Command nextCommand = createCommand(c);
                        processCommand (nextCommand);
                }

                //Print the X, Y, and Z ranges
                System.err.println ("X Range: (" + minX*scale + ", " + maxX*scale +
                                    ") \nY Range: (" + minY*scale + ", " + maxY*scale +
                                    ") \nZ Range: (" + minZ*scale + ", " + maxZ*scale +")");

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
        hm.put('T', 1.0); //tool selection
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
        hm.put("G2", true);
        hm.put("G3", true);
        hm.put("M6", true);
}

public static void makeToolLibrary(HashMap<Integer,Double> tl){
        tl.put(1, .5);
        tl.put(2, .25);
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

        //For x, y, and z, get existing values for starting point of the line
        prevX = parameters.get('X');
        prevY = parameters.get('Y');
        prevZ = parameters.get('Z');

        prevI = parameters.get('I');
        prevJ = parameters.get('J');
        prevK = parameters.get('K');
        prevR = parameters.get('R');

        //Get TypeMode string
        StringBuilder s = new StringBuilder();
        s.append (Character.toUpperCase(type.getText().charAt(0)));
        s.append (toNum(c.natural()));

        //ONLY IF A COMMAND IS KNOWN update params and min/maxes
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
                        System.err.println("MOVE: ("+ prevX*scale + ", " + prevY*scale + ", " +
                                           prevZ*scale + "), (" + X*scale + ", " + Y*scale + ", " + Z +
                                           ") at feed rate " + parameters.get('F') );
                        break;
                case 1:
                        lines.add(new Line (prevX, prevY, X, Y, toolLibrary.get((int)Math.round(parameters.get('T')))));
                        System.err.println("Line drawn: ("+ prevX*scale + ", " + prevY*scale + ", " +
                                           prevZ*scale + "), (" + X*scale + ", " + Y*scale + ", " + Z +
                                           ") at feed rate " + parameters.get('F') );
                        break;
                case 2:
                        //DOES THE -1 ACTUALLY MATTER AT ALL??
                        Arc newArc;
                        if (isRadiusArc()) {
                                newArc = new Arc(parameters.get('R'), prevX, prevY, X, Y, -1, toolLibrary.get((int)Math.round(parameters.get('T'))));
                                //if the radius is negative, draw the inverse
                                if (parameters.get('R')  < 0) {
                                        newArc.inverseArc();
                                }
                        } else {
                                newArc = new Arc(parameters.get('I'), parameters.get('J'), prevX, prevY, X, Y, -1, toolLibrary.get((int)Math.round(parameters.get('T'))));
                        }
                        arcs.add(newArc);
                        System.err.println("Clockwise arc drawn with center (" +  newArc.getCenterX() +
                                           ", " + newArc.getCenterY() + ") and radius " + parameters.get('R') + " from " +
                                           newArc.getStartAngle() + " to " + newArc.getEndAngle());
                        break;
                case 3:
                        Arc aNewArc;
                        if (isRadiusArc()) {
                                aNewArc = new Arc(parameters.get('R'), prevX, prevY, X, Y, 1, toolLibrary.get((int)Math.round(parameters.get('T'))));
                                //if the radius is negative, draw the inverse
                                if (parameters.get('R')  < 0) {
                                        aNewArc.inverseArc();
                                }
                        } else {
                                aNewArc = new Arc(parameters.get('I'), parameters.get('J'), prevX, prevY, X, Y, 1, toolLibrary.get((int)Math.round(parameters.get('T'))));
                        }
                        arcs.add(aNewArc);

                        System.err.println ("Counterclockwise arc drawn with center (" +  aNewArc.getCenterX() +
                                            ", " + aNewArc.getCenterY() + ") and radius " + parameters.get('R') + " from " +
                                            aNewArc.getStartAngle() + " to " + aNewArc.getEndAngle());
                        break;
                case 4:
                        System.err.println("Dwell for " + parameters.get('P') + "time");
                        break;
                case 17:
                        System.err.println("XY plane selected");
                        break;
                case 18:
                        System.err.println("ZX plane selected");
                        break;
                case 19:
                        System.err.println("YZ plane selected");
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

                case 90:
                        System.err.println("Absolute distance mode");
                        break;

                case 91:
                        System.err.println("Incremental distance mode");
                        break;

                default:
                        System.err.println ("Command not processed. Command: " + type + mode);
                }

        } else if (type == 'M') {
                switch (mode) {

                case 0:
                        System.err.println ("Programmed stop");
                        break;

                case 1:
                        System.err.println ("Optional stop");
                        break;

                case 2:
                        System.err.println ("End of main program");
                        break;

                case 3:
                        System.err.println ("Spindle on");
                        break;

                case 5:
                        System.err.println ("Spindle off");
                        break;

                case 6:
                        System.err.println ("Tool Change to tool " + (int)Math.round(parameters.get('T')) );
                        break;
                case 8:
                        System.err.println ("Dust hood up on");
                        break;

                case 9:
                        System.err.println ("Dust hood up off");
                        break;

                case 10:
                        System.err.println ("Open grip on");
                        break;
                case 11:
                        System.err.println ("Open grip off with check for tool in spindle");
                        break;

                case 12:
                        System.err.println ("Open grip off with no check for tool in spindle");
                        break;

                case 20:
                        System.err.println ("Turns on mister output");
                        break;
                case 21:
                        System.err.println ("Turns off mister output");
                        break;

                case 30:
                        System.err.println ("Program end and rewind");
                        break;

                case 102:
                        System.err.println ("Rotates rack to new tool position");
                        break;

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
        f.add(new MyPanel(lines, arcs, scaleFactor, scale));
        f.setPreferredSize(new Dimension(pxDimX, pxDimY));
        f.pack();
        f.setVisible(true);
}



/*
 * Private helper functions
 *
 */
//For the command context to command transformation
//Creates an int from a NaturalContext
private static int toNum(PrgParser.NaturalContext nc) {
        List<TerminalNode> dcs = nc.DIGIT();

        StringBuffer digits = new StringBuffer(dcs.size());
        for (TerminalNode d : dcs) {
                digits.append(d.getSymbol().getText());
        }

        return Integer.parseInt(digits.toString());
}

//For the command context to command transformation
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


//Using the max dimensions (unique to each screen size) and the entered dimensions
//find the scale factor that allows the window to always appear as large as possible
//while keeping the tool cuts proportional
private static double findScaleFactor (int xDim, int yDim){
        double SF;
        double xSF = MAX_X_DIM / xDim;
        double ySF = MAX_Y_DIM / yDim;
        if (xSF < ySF) {
                SF = xSF;
        } else{
                SF = ySF;
        }
        return SF;
}


//tells you if the arc should be designated by the radius (R) or the center (I,J)
//if the I, J, K values haven't been recently updated then assume its in radius mode
//we need to check this is a valid assumption
private static Boolean isRadiusArc(){
        return (prevI == parameters.get('I') && prevJ == parameters.get('J') &&
                prevK == parameters.get('K') && prevR != parameters.get('R'));
}

}



//The panel sets up and displays the GUI in the end
class MyPanel extends JPanel {
ArrayList<Line> theLines;
ArrayList<Arc> theArcs;
double scale; //This is automatically found for displaying the window correctly
double auxScale; //This is used for debugging so you can easily scale your design up and
              //down without manually altering all the gcode values


public MyPanel(ArrayList<Line> j, ArrayList<Arc> k, double scaleFactor, double auxilliaryScale) {
        setBorder(BorderFactory.createLineBorder(Color.black));
        theLines = j;
        theArcs = k;
        scale = scaleFactor;
        auxScale = auxilliaryScale;
}

public Dimension getPreferredSize() {
        return new Dimension(250,200);
}

protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        for (Line l : theLines) {
                g2.setStroke(new BasicStroke((float)(scale*l.getLineWidth())));
                g.drawLine((int)Math.round(l.getStartX()*scale * auxScale), (int)Math.round(l.getStartY()*scale * auxScale),
                           (int)Math.round(l.getEndX()*scale * auxScale), (int)Math.round(l.getEndY()*scale * auxScale));
        }

        for (Arc a : theArcs) {
                double[] dInfo = a.getDrawingInfo();
                g2.setStroke(new BasicStroke((float)(scale*a.getLineWidth())));
                g.drawArc((int)Math.round(dInfo[0]), (int)Math.round(dInfo[1]),
                          (int)Math.round(dInfo[2]), (int)Math.round(dInfo[3]), (int)Math.round(dInfo[4]),
                          (int)Math.round(dInfo[5]));
        }


        /*
           //Should probably fix this but also meh
           "This setting can be changed by G code, G90.1 and G91.1, or in the general tab in the Mach configuration.
           This setting is independent of the G90/G91 setting. If arc center mode is set to incremental then I, J, K
           are the distance and direction from the start point to the center point of the arc.
           If arc center mode is set to absolute then I, J, K are the absolute position of the arc center point
           in the current user coordinate system."

         */


}
}
