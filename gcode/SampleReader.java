import java.lang.Object;
import java.util.*;
import java.io.*;
//import java.string;

/*
 * @author Sonia Grunwald
 * @version 1.0
 *
 * Note: This gcode reader expects there to be no comments with g0 or g1 in them,
 * no additional letter codes on the same line as a g0/g1 command, the x to follow
 * directly after a command, and there to be x, y, and z coordinates
 */
public class SampleReader {

private static ArrayList<Line> lines = new ArrayList<Line> ();
private static ArrayList<String> unprocessedLines = new ArrayList<String> ();
private static double currX = 0.0;
private static double currY = 0.0;
private static final String FILENAME = "GCodeReader/lines.txt";
private static final String BAD_LINE_FILENAME = "GCodeReader/unprocessedLines.txt";

//private static BufferedWriter fileWriter2;

public static void main(String[] args) {

        try{



                if (args.length == 1) {
                        BufferedReader fileReader = new BufferedReader (new FileReader (args[0]));
                        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(FILENAME));
                        BufferedWriter fileWriter2 = new BufferedWriter(new FileWriter(BAD_LINE_FILENAME));

                        String nextLine = fileReader.readLine();


                        while(nextLine != null && nextLine.length()>0) {
                                nextLine = nextLine.toLowerCase();
                                processLine(nextLine);
                                nextLine = fileReader.readLine();
                        }

                        String outputLines = "";
                        for (Line l : lines) {

                                outputLines += l.toString();
                                outputLines += "\n";

                        }
                        fileWriter.write(outputLines);



                        String unprocessedLineString = "";
                        for (String k : unprocessedLines) {

                                unprocessedLineString += k;
                                unprocessedLineString += "\n";

                        }
                        fileWriter2.write(unprocessedLineString);


                        fileReader.close();
                        fileWriter.close();
                        fileWriter2.close();

                } else {
                        System.out.println ("Pick one file please!");
                }


        } catch (IOException e) {
                System.out.println ("Exception: " + e);
        }

}

public static void processLine(String l){




        int start = l.indexOf('g');

        //if it has an instance of a G command
        if (start != -1) {
                Character next = l.charAt(start+1);

                //if you're just drawing a line
                if (next == '0' || next == '1') {
                        String newX, newY, newZ;
                        double xVal, yVal, zVal;
                        int indexX = l.indexOf('x');
                        int indexY = l.indexOf('y');
                        int indexZ = l.indexOf('z');

                        if (indexX != -1 && indexY != -1 && indexZ != -1) {
                                newX = l.substring(indexX + 1, indexY);
                                newY = l.substring(indexY + 1, indexZ);
                                newZ = l.substring(indexZ + 1);

                                xVal = Double.parseDouble(newX);
                                yVal = Double.parseDouble(newY);
                                zVal = Double.parseDouble(newZ);


                                lines.add(new Line(currX, currY, xVal, yVal));
                                currX = xVal;
                                currY = yVal;


                        } else {
                                unprocessedLines.add("Line Not Processed: " + l);
                        }

                }
                //If it's any other command, just print it out
                else {
                  unprocessedLines.add("Line Not Processed: " + l);
                }




                // switch (next) {
                //
                //     case 0:
                //       //if the
                //       break;
                // }

        } else {
          unprocessedLines.add("Line Not Processed: " + l);
        }


}












}
