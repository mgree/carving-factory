/*
    More features
        Think about how to implement arcs, etc
        Deal with scaling and errors
              calculate the existing error
              create error threshold var
              from there calculate required scale factor
              compare to dimensions
              output results with scale if able, else report that these
                dimensions can't be used to create an image of high enough fidelity
        Color of Z
          gradient lines



   Check these cases:
        multiple G commands on one line
        Weird start commands (s, t)
        Negative x and y values
        u,v,w
        G0 IS A MOVE WITHOUT DRAWING


   Parser still can't handle:
      G91.1
      returns in the middle of lines

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
private static HashMap<Character,Double> parameters = new HashMap<Character,Double>();
private static HashMap<String,Boolean> processable = new HashMap<String,Boolean>();

//This is an additional scale to be used in debugging designs
//This allows the user to manually scale their design up and down
//They will then use this information to alter their gcode
private static int scale = 1;

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

//default stroke width for the drawing pane
//This is updated to appear proportional to the given dimensions
private static int strokeWidth = 1;

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


public static void main(String[] args) {
        //Wrong number of arguments
        if (args.length > 4 || args.length < 1) {
                System.err.println("Error: Please enter one file and two optional dimensions (in inches) and/or an optional scale factor");
                System.exit(-1);
        }
        try{
                //if there is 1 additional arg
                //scale is entered
                if (args.length == 2) {
                        if (args[1].charAt(0) == 's' || args[1].charAt(0) == 'S') {
                                scale = (int)Math.round(Double.parseDouble(args[1].substring(1)));
                        }  else {
                                System.err.println("Enter a scale preceded by an 's' or two dimensions");
                                System.exit(-1);
                        }
                }
                //if there are 2 args
                //only dimensions are inputted
                if (args.length == 3) {
                        dimX = (int)Math.round(Double.parseDouble(args[1]));
                        dimY = (int)Math.round(Double.parseDouble(args[2]));

                }
                //if there are 3 args
                //the dimensions and a scale is entered
                if (args.length == 4) {
                        dimX = (int)Math.round(Double.parseDouble(args[1]));
                        dimY = (int)Math.round(Double.parseDouble(args[2]));

                        if (args[3].charAt(0) == 's' || args[3].charAt(0) == 'S') {
                                scale = (int)Math.round(Double.parseDouble(args[3].substring(1)));
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
                strokeWidth = (int) (toolWidth * scaleFactor); //ok to just cast bc want to round down
                pxDimX = (int)(scaleFactor * dimX);
                pxDimY = (int)(scaleFactor * dimY);


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


//Using the max dimensions (unique to each screen size) and the entered dimensions
//find the scale factor that allows the window to always appear as large as possible
//while keeping the tool cuts proportional
public static double findScaleFactor (int xDim, int yDim){
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

        //For x, y, and z, get existing values for starting point of the line
        prevX = parameters.get('X');
        prevY = parameters.get('Y');
        prevZ = parameters.get('Z');

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
                        //lines.add(new Line (prevX, prevY, X, Y));
                        System.err.println("MOVE: ("+ prevX*scale + ", " + prevY*scale + ", " +
                                           prevZ*scale + "), (" + X*scale + ", " + Y*scale + ", " + Z +
                                           ") at feed rate " + parameters.get('F') );
                        break;
                case 1:
                        lines.add(new Line (prevX, prevY, X, Y));
                        System.err.println("Line drawn: ("+ prevX*scale + ", " + prevY*scale + ", " +
                                           prevZ*scale + "), (" + X*scale + ", " + Y*scale + ", " + Z +
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
                        System.err.println ("Tool Change");
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
        f.add(new MyPanel(lines, scaleFactor, scale, strokeWidth));
        f.setPreferredSize(new Dimension(pxDimX, pxDimY));
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



//The panel sets up and displays the GUI in the end
class MyPanel extends JPanel {
ArrayList<Line> theLines;
double scale; //This is automatically found for displaying the window correctly
int auxScale; //This is used for debugging so you can easily scale your design up and
              //down without manually altering all the gcode values
int strokeWidth;


public MyPanel(ArrayList<Line> j, double scaleFactor, int auxilliaryScale, int stroke) {
        setBorder(BorderFactory.createLineBorder(Color.black));
        theLines = j;
        scale = scaleFactor;
        auxScale = auxilliaryScale;
        strokeWidth = stroke;
}

public Dimension getPreferredSize() {
        return new Dimension(250,200);
}

protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(strokeWidth));

        for (Line l : theLines) {
                g.drawLine((int)Math.round(l.getStartX()*scale * auxScale), (int)Math.round(l.getStartY()*scale * auxScale),
                           (int)Math.round(l.getEndX()*scale * auxScale), (int)Math.round(l.getEndY()*scale * auxScale));
        }

        //draws a counterclockwise arc starting at pos x side
        //rect top left x, rect top left y, rect width, rect height,
        // starting degree (0 is positive x side), num degrees on arc
        //("length" of arc in degrees) going counterclockwise, arc type
        // g2.draw(new Arc2D.Double(40, 40, 50, 50, 180, 135, Arc2D.OPEN));
        // g.drawLine(40,40,40,40);

        /*start and end points, its radius or center point, a direction, and a plane.
        Direction is determined by G02, clockwise, and G03, counterclockwise, when viewed from the plane’s positive direction
        (If XY plane is selected look down so that the X axis positive direction is pointing to the right, and the Y axis
        positive direction is pointing forward)
        The start point is the current position of the machine.
        Specify the end point with X, Y, and Z.
          The values input for the end point will depend on the current G90/G91 (abs/inc) setting of the machine.
          Only the two points in the current plane are required for an arc. Adding in the third point will create a helical interpolation.
        Next is to specify the radius or the center point of the arc, only one or the other, not both.
          Radius:
          To specify the radius, use R and input the actual radius of the desired arc
          When an arc is created knowing only start and end points and a radius there are two possible solutions,
          an arc with a sweep less than 180° and one with sweep greater than 180°. The sign of the radius value,
          positive or negative, determines which arc will be cut, see figure 3. A positive value for R cuts an arc
          with sweep less than 180°. A negative value for R cuts an arc with sweep greater than 180°.
          Center:
          A more accurate and reliable way to define an arc is by specifying the center point, this is done with arguments I, J, and K
          The center point must be defined in the current plane. I, J, and K correspond to X, Y, Z respectively; the current plane
          selection will determine which two are used. XY plane (G17) would use I and J for example.
          Mach has two settings for how I, J, and K should be specified, absolute and incremental.
          This setting can be changed by G code, G90.1 and G91.1, or in the general tab in the Mach configuration.
          This setting is independent of the G90/G91 setting. If arc center mode is set to incremental then I, J, K
          are the distance and direction from the start point to the center point of the arc.
          If arc center mode is set to absolute then I, J, K are the absolute position of the arc center point
          in the current user coordinate system.

        */

        // for (Arc a : theArcs){
        //g2.draw(new Arc2D.Double(x, y,rectwidth, rectheight, 90, 135, Arc2D.OPEN));
        //
        // }

}
}
