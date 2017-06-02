package GCodeReader2;



/*
    THINGS IT CAN'T DEAL WITH:
        COMMENTS
        multiple G commands on one line
        Weird start commands (s, t)
        Negative x and y values



   read file and create commands
   analyze
   process commands
    iterate through params setting appropriate values
    According to the mode, execute something



    Eventually)
    Think about how to implement arcs, report feedrates, etc
    Do so
    clean up char[] ?

    1) Make more robust
      allow comments
      ignore things better
      Don't use problem values
    2)
    Once completed:
    add extra command line arguments for drill size and dimensions
    add this into the swing panel

    3)
    Once completed:
    Deal with scaling and errors
      calculate the existing error
      create error threshold var
      from there calculate required scale factor
      compare to dimensions
      output results with scale if able, else report that these
        dimensions can't be used to create an image of high enough fidelity


 */



import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import java.io.*;
import java.lang.Object;
import java.util.*;


public class GCodeReader {
public static ArrayList<Line> lines = new ArrayList<Line> ();
private static final int SCALE = 1;
private static final int MAX_LINE_SIZE = 300;
private static ArrayList<String> unprocessedLines = new ArrayList<String> ();

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

        try{
                //If you successfully give it 1 filename
                if (args.length == 1) {

                        //Open readers and writers
                        BufferedReader fileReader = new BufferedReader (new FileReader (args[0]));

                        //Read from the gcode file and process each line
                        String nextLine = fileReader.readLine();
                        while(nextLine != null && nextLine.length()>0) {
                                nextLine = nextLine.toLowerCase();
                                Command nextCommand = createCommand(nextLine);
                                processCommand (nextCommand);
                                nextLine = fileReader.readLine();
                        }

                        //Store unprocessed lines in a separate file
                        String unprocessedLineString = "";
                        for (String k : unprocessedLines) {
                                unprocessedLineString += "\n";
                                unprocessedLineString += k;

                        }
                        System.err.println(unprocessedLineString);


                        System.err.println ("X Range: (" + minX + ", " + maxX +
                              ") \nY Range: (" + minY + ", " + maxY +
                              ") \nZ Range: (" + minZ + ", " + maxZ +")");
                        //Clean up
                        fileReader.close();

                } else {
                        System.out.println ("Pick one file please!");
                }


        } catch (IOException e) {
                System.out.println ("Exception: " + e);
        }

        //Show the GUI
        SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                createAndShowGUI();
                        }
                });
}



//Method that creates COMMANDS from a String of GCode
public static Command createCommand(String l){
        //create a new command
        Command newCommand = new Command();

        int start = indexNextChar(l);

        //if it has something on the line
        if (start != -1) {

                //Get the type of the command
                Character next = l.charAt(start);

                //If the letter is n (line number), skip to the next command
                if (next == 'n') {
                        start = indexNextChar(l, start+1);
                        next = l.charAt(start);
                }

                if (next == 'g' || next == 'm') {
                        //find and set type
                        newCommand.setType(next);

                        //find and set mode
                        int modeStart = indexNextNum(l, start+1);
                        int modeEnd = indexNextNonNum(l, start+1);
                        int newMode = Integer.parseInt(l.substring(modeStart, modeEnd));
                        newCommand.setMode(newMode);

                        //find and set all parameters
                        int paramType = indexNextChar(l, modeEnd);

                        while (paramType != -1) {
                                int paramValStart = indexNextNum(l, paramType);
                                int paramValEnd = indexNextNonNum(l, paramValStart+1);

                                //This shouldn't happen. It means an error in the gcode
                                if (paramValStart == -1) {
                                        System.err.println("Error: No value associated with parameter");
                                        break;
                                }
                                //Last parameter. In this case go to the end
                                else if (paramValEnd == -1) {
                                        double paramValue = Double.parseDouble(l.substring(paramValStart));
                                        Params newParam = new Params(l.charAt(paramType), (int)paramValue);
                                        newCommand.addParam(newParam);
                                }
                                //Middle parameter. Just get this value.
                                else {
                                        //NOTE: INFO LOST IN DOUBLE->INT CONVERSION
                                        double paramValue = Double.parseDouble(l.substring(paramValStart, paramValEnd));
                                        Params newParam = new Params(l.charAt(paramType), (int)paramValue);
                                        newCommand.addParam(newParam);
                                }

                                //Get next parameter type
                                paramType = indexNextChar(l, paramValEnd);

                        }

                } else {
                        System.err.println("Invalid command type. G or M Code required." + next + " found instead.");
                }

        } else {
                unprocessedLines.add("Line Not Processed: " + l);
        }

        return newCommand;
}



//Method that processes the command, setting the parameters to the appropriate values
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

        if (type == 'g') {
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
                case 20:
                        System.err.println("Change units to inches");
                case 21:
                        System.err.println("Change units to millimeters");
                case 30:
                        System.err.println("Return Home");
                        X = 0.0;
                        Y = 0.0;
                        Z = 0.0;
                case 49:
                        System.err.println("Cancel tool length offset");
                default:
                        System.err.println ("Command not processed. Command: " + type + mode);
                }


        } else if (type == 'm') {
                switch (mode) {
                default:
                        System.err.println ("Command not processed. Command: " + type + mode);
                }
        } else {
                System.err.println ("Error. Type: " + type);
        }



}



//Method updates the parameters according to an arraylist of new parameters
private static void updateParameters (ArrayList<Params> theParams) {
        //for all the parameters in the command, set those appropriately
        for (Params p : theParams) {
                char type = p.getType();
                double value = p.getValue();

                switch (type) {

                case 'a':
                        A = value;
                        break;
                case 'b':
                        B = value;
                        break;
                case 'c':
                        C = value;
                        break;
                case 'd':
                        D = value;
                        break;
                case 'f':
                        F = value;
                        break;
                case 'h':
                        H = value;
                        break;
                case 'i':
                        I = value;
                        break;
                case 'j':
                        J = value;
                        break;
                case 'k':
                        K = value;
                        break;
                case 'l':
                        L = value;
                        break;
                case 'o':
                        O = value;
                        break;
                case 'p':
                        P = value;
                        break;
                case 'q':
                        Q = value;
                        break;
                case 'r':
                        R = value;
                        break;
                case 's':
                        S = value;
                        break;
                case 't':
                        T = value;
                        break;
                case 'u':
                        A = value;
                        break;
                case 'v':
                        B = value;
                        break;
                case 'w':
                        C = value;
                        break;
                case 'x':
                        X = value;
                        break;
                case 'y':
                        Y = value;
                        break;
                case 'z':
                        Z = value;
                        break;
                default:
                        System.err.println("Parameter type not found. Type: " + type);
                }


                //in x, y, and z keep track of maximum and minimum values
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








/*
 * PRIVATE METHODS FOR STRING MANIPULATION
 */

//find first instance of a letter
private static int indexNextChar(String s){
        return indexNextChar(s, 0);
}

//find next instance of a letter starting at specific index
//Note that the strings we are using are already LOWERCASE
//if no characters are found, it returns -1
private static int indexNextChar(String s, int startIndex){
        if (startIndex > -1) {
                char[] tempChars = s.toCharArray();
                for (int i = startIndex; i < tempChars.length; i++) {
                        if (tempChars[i]>96 && tempChars[i]<123) {
                                return i;
                        }
                }
        }

        return -1;
}

//find first instance of a number
private static int indexNextNum(String s){
        return indexNextNum(s, 0);
}

//find next instance of a digit, -, or . starting at a specific index
//if no numbers are found, returns -1
private static int indexNextNum(String s, int startIndex){
        if (startIndex > -1) {

                char[] tempChars = s.toCharArray();
                for (int i = startIndex; i < tempChars.length; i++) {
                        if (tempChars[i]>44 && tempChars[i]<58 && tempChars[i] != '/') {
                                return i;
                        }
                }
        }
        return -1;
}

//find the next instand of a non-number
private static int indexNextNonNum(String s, int startIndex){
        if (startIndex > -1) {

                char[] tempChars = s.toCharArray();
                for (int i = startIndex; i < tempChars.length; i++) {
                        if (tempChars[i]<45 || tempChars[i]>57 || tempChars[i] == '/') {
                                return i;
                        }
                }
        }
        return -1;
}

//Create the GUI
private static void createAndShowGUI() {
        JFrame f = new JFrame("GCode Results");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new MyPanel(lines));
        f.pack();
        f.setVisible(true);
}
}










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
