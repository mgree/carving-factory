/*
 * Gcode Reader -- designed to take a GCode file and display a GUI of
 * the expected pattern to be drawn. It also outputs all instructions
 * it processes in an easy to read format in the terminal window
 *
 * @author Sonia Grunwald
 * @version June, 2017
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
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.GridLayout;


public class GCodeReader {

private static final double MAX_X_DIM = 1300;
private static final double MAX_Y_DIM = 725;

private static ArrayList<Line> lines = new ArrayList<Line> ();
private static ArrayList<Arc> arcs = new ArrayList<Arc>();
private static HashMap<Character,Double> parameters = new HashMap<Character,Double>();
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

//Initialize min and max values
private static double maxX = 0.0;
private static double minX = 0.0;
private static double maxY = 0.0;
private static double minY = 0.0;
private static double maxZ = 0.0;
private static double minZ = 0.0;

//Initialize starting point
private static double prevX = 0.0;
private static double prevY = dimY;
private static double prevZ = 0.0;

//Initialize arc centers
private static double prevI = 0.0;
private static double prevJ = 0.0;
private static double prevK = 0.0;
private static double prevR = 1.0;

//Bool to warn if drawing is valid
private static Boolean validDrawing = true;

//Command number
private static int commandLineNum = 0;

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
                //find the scale factor and set the dimensions accordingly
                scaleFactor = findScaleFactor(dimX, dimY);
                pxDimX = (int)(scaleFactor * dimX);
                pxDimY = (int)(scaleFactor * dimY);

                //Parse document
                input = CharStreams.fromFileName(args[0]);
                PrgLexer lexer = new PrgLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                PrgParser parser = new PrgParser(tokens);

                //Set up parameters / tool library
                addAllParams(parameters);
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
                if (!validDrawing) {
                        System.err.println ("This drawing is invalid given the x and y stock lengths. Please reexamine your gcode.");
                } else {
                        System.err.println ("This drawing is valid given the x and y stock lengths.");
                }

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



/* Adds all parameters to the hashmap mapping characters to their default values */
public static void addAllParams(Map<Character,Double> hm){
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


/* Creates a tool library mapping tool numbers to the size of the bit */
public static void makeToolLibrary(Map<Integer,Double> tl){
        tl.put(1, .5);
        tl.put(2, .25);
}


/* Checks if the dimensions can be drawn on the given space */
public static Boolean outOfBounds (double x1, double y1, double x2, double y2){
        x1 *= (scale * scaleFactor);
        y1 *= (scale * scaleFactor);
        x2 *= (scale * scaleFactor);
        y2 *= (scale * scaleFactor);

        if (x1 <= pxDimX && x1 >= 0 &&
            y1 <= pxDimY && y1 >= 0 &&
            x2 <= pxDimX && x2 >= 0 &&
            y2 <= pxDimY && y2 >= 0) {
                return false;
        }

        return true;
}


/* Creates a command from a CommandContext */
public static Command createCommand(PrgParser.CommandContext c){

        Command newCommand = new Command();

        //if it's a line of just arguments, get them and set the arguments
        //This will leave the newCommand with default values which means they
        //won't get executed
        PrgParser.JustArgContext justArg = c.justArg();
        PrgParser.NormalCmdContext normalCmd = c.normalCmd();
        if (justArg != null) {
                List<PrgParser.ArgContext> argList = justArg.arg();
                for (PrgParser.ArgContext a : argList) {
                        Character charCode = Character.toUpperCase(a.paramChar().getText().charAt(0));
                        Double paramValue = (double)toFloat(a.paramArg().floatNum());
                        parameters.put (charCode, paramValue);
                }
        }
        //If it's a normal command, proceed as usual
        else if (normalCmd != null) {
                newCommand.setLineNum(commandLineNum++);

                //get the type and set the type to that character
                //(For unclear reasons, using tokens here made things mor difficult)
                PrgParser.TypeContext type = normalCmd.type();
                newCommand.setType(Character.toUpperCase(type.getText().charAt(0)));

                //get the mode and set it
                newCommand.setMode(toNum(normalCmd.natural()));

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
                s.append (toNum(normalCmd.natural()));

                //For all the arguments, update that parameter in the hashmap
                List<PrgParser.ArgContext> argList = normalCmd.arg();
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


/* Processes the command. This may include adding lines to the list of lines
   to be drawn, adding arcs to the appropriate list, and printing out updates of what
   each command does */
public static void processCommand(Command c){

        char type = c.getType();
        int mode = c.getMode();
        int linenumber = c.getLineNum();

        double X = parameters.get('X');
        double Y = parameters.get('Y');
        double Z = parameters.get('Z');

        //execute something according to the type and mode
        //here write to a file or print an update (added line from (x,y) to (x,y))
        if (type == 'G') {
                switch (mode) {
                case 0:
                        if (outOfBounds(prevX, dimY - prevY, X, dimY - Y)) {
                                System.err.println("Warning: Moving outside stock limits");
                        }
                        System.err.println("Rapid Move: ("+ prevX*scale + ", " + (dimY - prevY)*scale + ", " +
                                           prevZ*scale + "), (" + X*scale + ", " + (dimY - Y)*scale + ", " + Z +
                                           ") at feed rate " + parameters.get('F') );
                        break;
                case 1:
                        if (outOfBounds(prevX, dimY - prevY, X, dimY - Y)) {
                                System.err.println("Warning: Line will not fit on specified stock");
                                validDrawing = false;
                        }
                        lines.add(new Line (prevX, prevY, X, Y, toolLibrary.get((int)Math.round(parameters.get('T')))));
                        System.err.println("Line drawn: ("+ prevX*scale + ", " + (dimY - prevY)*scale + ", " +
                                           prevZ*scale + "), (" + X*scale + ", " + (dimY - Y)*scale + ", " + Z +
                                           ") at feed rate " + parameters.get('F') );
                        break;
                case 2:
                        if (outOfBounds(prevX, dimY - prevY, X, dimY - Y)) {
                                System.err.println("Warning: Arc will not fit on specified stock");
                                validDrawing = false;
                        }
                        Arc newArc;
                        if (isRadiusArc()) {
                                newArc = new Arc(parameters.get('R'), prevX, dimY - prevY, X, dimY - Y, -1, toolLibrary.get((int)Math.round(parameters.get('T'))), linenumber);
                                //if the radius is negative, draw the inverse
                                if (parameters.get('R')  < 0) {
                                        newArc.inverseArc();
                                }
                        } else {
                                newArc = new Arc(parameters.get('I'), parameters.get('J'), prevX, dimY - prevY, X, dimY - Y, -1, toolLibrary.get((int)Math.round(parameters.get('T'))), linenumber);
                        }
                        arcs.add(newArc);
                        System.err.println("Clockwise arc drawn with center (" +  newArc.getCenterX() +
                                           ", " + newArc.getCenterY() + ") and radius " + parameters.get('R') + " from " +
                                           newArc.getStartAngle() + " to " + newArc.getEndAngle());
                        break;
                case 3:
                        if (outOfBounds(prevX, dimY - prevY, X, dimY - Y)) {
                                System.err.println("Warning: Arc will not fit on specified stock");
                                validDrawing = false;
                        }
                        Arc aNewArc;
                        if (isRadiusArc()) {
                                aNewArc = new Arc(parameters.get('R'), prevX, dimY - prevY, X, dimY - Y, 1, toolLibrary.get((int)Math.round(parameters.get('T'))), linenumber);
                                //if the radius is negative, draw the inverse
                                if (parameters.get('R')  < 0) {
                                        aNewArc.inverseArc();
                                }
                        } else {
                                aNewArc = new Arc(parameters.get('I'), parameters.get('J'), prevX, pxDimY - prevY, X, pxDimY - Y, 1, toolLibrary.get((int)Math.round(parameters.get('T'))), linenumber);
                        }
                        arcs.add(aNewArc);

                        System.err.println ("Counterclockwise arc drawn with center (" +  aNewArc.getCenterX() +
                                            ", " + aNewArc.getCenterY() + ") and radius " + parameters.get('R') + " from " +
                                            aNewArc.getStartAngle() + " to " + aNewArc.getEndAngle());
                        break;
                case 4:
                        System.err.println("Dwell for " + parameters.get('P') + "time");
                        break;
                case 10:
                        System.err.println("Coordinate system origin setting");
                        break;
                case 12:
                        System.err.println("Clockwise circular pocket");
                        break;
                case 13:
                        System.err.println("Counterclockwise circular pocket");
                        break;
                case 15:
                        System.err.println("Polar Coordinate moves in G0 and G1");
                        break;
                case 16:
                        System.err.println("Polar Coordinate moves in G0 and G1");
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
                case 28:
                        System.err.println("Return Home");
                        parameters.put ('X', 0.0);
                        parameters.put('Y', 0.0);
                        parameters.put ('Z', 0.0);
                        break;
                case 30:
                        System.err.println("Return Home");
                        parameters.put ('X', 0.0);
                        parameters.put('Y', 0.0);
                        parameters.put ('Z', 0.0);
                        break;
                case 31:
                        System.err.println("Straight probe");
                        break;
                case 40:
                        System.err.println("Cancel cutter radius compensation");
                        break;
                case 41:
                        System.err.println("Start cutter radius compensation left");
                        break;
                case 42:
                        System.err.println("Start cutter radius compensation right");
                        break;
                case 43:
                        System.err.println("Apply tool length offset (plus)");
                        break;
                case 49:
                        System.err.println("Cancel tool length offset");
                        break;
                case 50:
                        System.err.println("Reset all scale factors to 1.0");
                        break;
                case 51:
                        System.err.println("Set axis data input scale factors");
                        break;
                case 52:
                        System.err.println("Temporary coordinate system offsets");
                        break;
                case 53:
                        System.err.println("Move in absolute machine coordinate system");
                        break;
                case 54:
                        System.err.println("Use fixture offset 1");
                        break;
                case 55:
                        System.err.println("Use fixture offset 2");
                        break;
                case 56:
                        System.err.println("Use fixture offset 3");
                        break;
                case 57:
                        System.err.println("Use fixture offset 4");
                        break;
                case 58:
                        System.err.println("Use fixture offset 5");
                        break;
                case 59:
                        System.err.println("Use fixture offset 6 / use general fixture number");
                        break;
                case 61:
                        System.err.println("Exact stop");
                        break;
                case 64:
                        System.err.println("Constant Velocity mode");
                        break;
                case 68:
                        System.err.println("Rotate program coordinate system");
                        break;
                case 69:
                        System.err.println("Rotate program coordinate system");
                        break;
                case 70:
                        System.err.println("Change units to inches");
                        break;
                case 71:
                        System.err.println("Change units to millimeters");
                        break;
                case 73:
                        System.err.println("Canned cycle - peck drilling");
                        break;
                case 80:
                        System.err.println("Cancel motion mode (including canned cycles)");
                        break;
                case 81:
                        System.err.println("Canned cycle - drilling");
                        break;
                case 82:
                        System.err.println("Canned cycle - drilling with dwell");
                        break;
                case 83:
                        System.err.println("Canned cycle - peck drilling");
                        break;
                case 85:
                        System.err.println("Canned cycle - boring");
                        break;
                case 86:
                        System.err.println("Canned cycle - boring");
                        break;
                case 88:
                        System.err.println("Canned cycle - boring");
                        break;
                case 89:
                        System.err.println("Canned cycle - boring");
                        break;
                case 90:
                        System.err.println("Absolute distance mode");
                        break;
                case 91:
                        System.err.println("Incremental distance mode");
                        break;
                case 93:
                        System.err.println("Inverse feed rate mode initiated");
                        break;
                case 94:
                        System.err.println("Feed per minute mode");
                        break;
                case 95:
                        System.err.println("Feed per revolution mode");
                        break;
                case 98:
                        System.err.println("Initial level return after canned cycles");
                        break;
                case 99:
                        System.err.println("R-point level return after canned cycles");
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
                        System.err.println ("Spindle on clockwise");
                        break;
                case 4:
                        System.err.println ("Spindle on counterclockwise");
                        break;
                case 5:
                        System.err.println ("Spindle off");
                        break;
                case 6:
                        System.err.println ("Tool Change to Tool " + (int)Math.round(parameters.get('T')) );
                        break;
                case 7:
                        System.err.println ("Mist coolant on");
                        break;
                case 8:
                        System.err.println ("Dust hood up on/Flood coolant on");
                        break;
                case 9:
                        System.err.println ("Dust hood up off/all coolant off");
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
                case 19:
                        System.err.println ("Spindle Orient");
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
                case 47:
                        System.err.println ("Repeat program from first line");
                        break;
                case 48:
                        System.err.println ("Enable feed/speed overrides");
                        break;
                case 49:
                        System.err.println ("Disable feed/speed overrides");
                        break;
                case 98:
                        System.err.println ("Subprogram call");
                        break;
                case 99:
                        System.err.println ("Return from subprogram/rewind");
                        break;
                case 102:
                        System.err.println ("Rotates rack to new tool position");
                        break;
                default:
                        System.err.println ("Command not processed. Command: " + type + mode);
                }
        }
        //If it was an argument only command it won't be processed
}





/*
 * Private helper functions
 *
 */
/* Creates an int from a NaturalContext to help translate a command context to
 * a command
 */
private static int toNum(PrgParser.NaturalContext nc) {
        List<TerminalNode> dcs = nc.DIGIT();
        StringBuffer digits = new StringBuffer(dcs.size());
        for (TerminalNode d : dcs) {
                digits.append(d.getSymbol().getText());
        }
        return Integer.parseInt(digits.toString());
}

/* Creates a float from a NaturalContext to help translate a command context to
 * a command
 */
private static float toFloat(PrgParser.FloatNumContext fnc) {
        //Save the sign
        String sign = " ";
        if (fnc.sign() !=null ) {
                sign = fnc.sign().getText();
        }
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
        float ans = Float.parseFloat(digits.toString());
        //if it's supposed to be negative, multiply by -1
        if (sign.charAt(0) == ('-')) {
                ans *= -1;
        }
        return ans;
}


/* Finds the appropriate pixels/inch scale factor to scale the GUI window and
 * drawings proportionally.
 * Using the max dimensions (unique to each screen size) and the entered dimensions
 * it allows the window to always appear as large as possible while keeping the
 * tool cuts proportional */
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

/* Indicates whether an arc should be drawn according to the radius parameter (R)
 * or the center parameters (I,J,K).
 * If the I, J, K values haven't been recently updated while R has, assume it's
 * in radius mode
 */
private static Boolean isRadiusArc(){
        return ((prevI ==parameters.get('I')) &&
                (prevJ == parameters.get('J')) &&
                (prevK == parameters.get('K')));
}



/* Creates and shows the GUI window */
private static void createAndShowGUI() {
        JFrame f = new JFrame("GCode Reader");

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new MyPanel (lines, arcs, scaleFactor, scale, pxDimY));
        f.setPreferredSize(new Dimension(pxDimX, pxDimY));
        f.pack();
        f.setVisible(true);
}
}


/**
 *
 * SETTING UP AND DISPLAYING THE GUI
 *
 **/


class MyPanel extends JPanel {
ArrayList<Line> theLines;
ArrayList<Arc> theArcs;
double scale; //This is automatically found for displaying the window correctly
double auxScale; //This is used for debugging so you can easily scale your design up and
                 //down without manually altering all the gcode values
int upToThisPointLines;
int upToThisPointArcs;
int pxDimY;

double arcoffset = 0;
double lineoffsetX = 0;
double lineoffsetY = 0;

public MyPanel(ArrayList<Line> j, ArrayList<Arc> k, double scaleFactor, double auxilliaryScale, int pxdy) {
        setBorder(BorderFactory.createLineBorder(Color.black));
        theLines = j;
        theArcs = k;
        scale = scaleFactor;
        auxScale = auxilliaryScale;
        pxDimY = pxdy;

        //Set up GUI elements
        upToThisPointLines = theLines.size();
        upToThisPointArcs = theArcs.size();
        JSlider upToLines = new JSlider(0, upToThisPointLines, upToThisPointLines);
        JSlider upToArcs = new JSlider(0, upToThisPointArcs, upToThisPointArcs);
        JLabel upToLinesText = new JLabel("Lines: " + Integer.toString(upToThisPointLines));
        JLabel upToArcsText = new JLabel("Arcs: " + Integer.toString(upToThisPointArcs));
        JButton getInfo = new JButton ("Get Current Arc Info");

        //Offsets used for debugging
        // JSlider arcOffset = new JSlider(-200, 200, 0);
        // JSlider lineOffsetX = new JSlider(-200, 200, 0);
        // JSlider lineOffsetY = new JSlider(-200, 200, 0);
        // JLabel arcOffsetText = new JLabel("Arc Offset: " + Double.toString(arcoffset));
        // JLabel lineOffsetTextX = new JLabel("Line X Offset: " + Double.toString(lineoffsetX));
        // JLabel lineOffsetTextY = new JLabel("Line YOffset: " + Double.toString(lineoffsetY));


        upToLines.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                                upToThisPointLines = upToLines.getValue();
                                upToLinesText.setText("Lines: " + Integer.toString(upToThisPointLines));
                                repaint();
                        }
                });
        upToArcs.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                                upToThisPointArcs = upToArcs.getValue();
                                upToArcsText.setText("Arcs: " + Integer.toString(upToThisPointArcs));
                                repaint();
                        }
                });


        // arcOffset.addChangeListener(new ChangeListener() {
        //                 public void stateChanged(ChangeEvent e) {
        //                         arcoffset = arcOffset.getValue();
        //                         arcOffsetText.setText("Arc Offset: " + Double.toString(arcoffset/100));
        //                         repaint();
        //                 }
        //         });
        //
        // lineOffsetX.addChangeListener(new ChangeListener() {
        //                 public void stateChanged(ChangeEvent e) {
        //                         lineoffsetX = lineOffsetX.getValue();
        //                         lineOffsetTextX.setText("Line X Offset: " + Double.toString(lineoffsetX/100));
        //                         repaint();
        //                 }
        //         });
        //
        //
        // lineOffsetY.addChangeListener(new ChangeListener() {
        //                 public void stateChanged(ChangeEvent e) {
        //                         lineoffsetY = lineOffsetY.getValue();
        //                         lineOffsetTextY.setText("Line Y Offset: " + Double.toString(lineoffsetY/100));
        //                         repaint();
        //                 }
        //         });


        getInfo.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                                System.out.println(theArcs.get(upToArcs.getValue()-1).toString());
                        }
                });

        this.add(upToLines);
        this.add(upToLinesText);
        this.add(upToArcs);
        this.add(upToArcsText);
        this.add(getInfo);
        // this.add(arcOffsetText);
        // this.add(arcOffset);
        // this.add(new JLabel("                                                     "));//spacer
        // this.add(lineOffsetTextX);
        // this.add(lineOffsetX);
        // this.add(lineOffsetTextY);
        // this.add(lineOffsetY);
}


public Dimension getPreferredSize() {
        return new Dimension(250,200);
}


protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < upToThisPointLines; i++) {
                Line l = theLines.get(i);
                g2.setStroke(new BasicStroke((float)(scale*l.getLineWidth())));
                g.drawLine((int)Math.round((l.getStartX()-lineoffsetX/100)*scale * auxScale), pxDimY - (int)Math.round((l.getStartY()+lineoffsetY/100)*scale * auxScale),
                           (int)Math.round((l.getEndX()-lineoffsetX/100)*scale* auxScale),  pxDimY - (int)Math.round((l.getEndY()+lineoffsetY/100)*scale * auxScale));


        }
        for (int i = 0; i < upToThisPointArcs; i++) {
                Arc a = theArcs.get(i);
                double[] dInfo = a.getDrawingInfo();
                g2.setStroke(new BasicStroke((float)(scale*a.getLineWidth())));
                g.drawArc((int)Math.round((dInfo[0]-arcoffset/100)*scale * auxScale), (int)Math.round((dInfo[1]-arcoffset/100)*scale * auxScale),
                          (int)Math.round(dInfo[2]*scale * auxScale), (int)Math.round(dInfo[3]*scale * auxScale), (int)Math.round(dInfo[4]),
                          (int)Math.round(dInfo[5]));
        }
}


/*
WEIRD ARC:

Command Number: 164
Rectangle: (24.602671123510007, 2.1778030891279556, 13.102999687194824, 13.102999687194824)
Start Angle: -95.30882470848113
End Angle: -81.5808550187568
Change in angles: 13.727969689724333
Center: (31.15417096710742, 8.729302932725368)
Arc:
Command Number: 165
Rectangle: (-393.8491429314827, -734.2627677000046, 752.7536010742188, 752.7536010742188)
Start Angle: -82.42956218077691
End Angle: -82.07388673633712
Change in angles: 0.3556754444397967
Center: (-17.472342394373285, -357.88596716289527)
Arc:
Command Number: 166
Rectangle: (26.22471445790675, 14.798072712197992, 19.12660026550293, 19.12660026550293)
Start Angle: 98.17282348388902
End Angle: 80.81095834522836
Change in angles: -17.36186513866066
Center: (35.788014590658214, 24.361372844949457)
*/
}
