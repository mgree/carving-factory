/*
 * Gcode Generator -- pops up a GUI window inviting you to draw. Once the window
 * is closed out of, the program writes the gcode for your drawing to a file
 * called outputGCode.txt. This gcode may be read in directly by the GCodeReader.
 *
 * @author Sonia Grunwald
 * @version June 7th, 2017
 * Michael Greenberg Lab
 *
 */



import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.*;
import java.awt.Window;
import java.awt.Window.*;
import java.text.SimpleDateFormat;
import java.lang.Object;
import java.util.*;
import java.io.*;

public class GCodeGenerator {

private static final String FILENAME = "outputGCode.txt";
private static MyFavoritePanel thePanel = new MyFavoritePanel();
private static Boolean closed = false;
private static StringBuilder gcode = new StringBuilder();

public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                createAndShowGUI();
                        }
                });
}

/* Creates and displays the GUI */
private static void createAndShowGUI() {

        JFrame f = new JFrame("GCodeGenerator");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(thePanel);
        f.pack();
        f.setVisible(true);

        //When the window is closed, trigger an event writing all in
        //information to a file
        f.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                                writeFile();
                        }
                });

}

/* Writes all the information in gcode format to a file with appropriate
 * starting and ending sequences */
public static void writeFile(){

        BufferedWriter fileWriter = null;
        Date d = new java.util.Date();
        String startSequence = "(Generated GCode from  " + d + ")\nN0 G0 G49 G40 G17 G80 G50 G90\nN1 T1 M6\n";

        try{
                //Get the lines
                ArrayList<Line> lines = thePanel.getLines();

                //if you've drawn something
                if (lines.size() > 0) {

                        //start the file writer and append the start seq
                        fileWriter = new BufferedWriter(new FileWriter(FILENAME));
                        gcode.append(startSequence);
                        int linenum = 2;

                        //for the first one  we want to G0 to the starting coordinates
                        gcode.append("N" + linenum + " G0 X" + lines.get(0).getStartX() + " Y" +
                                     lines.get(0).getStartY() + " Z0.0\n");
                        linenum++;

                        //For the rest of the lines, detect if it should be a
                        //G0 or a G1. Add it to the gcode
                        for (int i = 1; i < lines.size(); i++) {
                                //if the start isn't the same as the last end
                                //move to the new start
                                if(lines.get(i).getStartX() != lines.get(i-1).getEndX() &&
                                   lines.get(i).getStartY() != lines.get(i-1).getEndY() ) {
                                        gcode.append("N" + linenum + " ");
                                        gcode.append("G0 X" + lines.get(i).getStartX() + " Y" +
                                                     lines.get(i).getStartY() + " Z0.0\n");
                                        linenum = (linenum+1) % 99999; //ensuring we never go over the limit

                                }
                                //no matter what, draw the line and increase the line number
                                gcode.append("N" + linenum + " ");
                                gcode.append(lines.get(i).toGCode());
                                linenum = (linenum+1) % 99999; //ensuring we never go over the limit

                        }

                        //add the ending sequence
                        String endSeq = "N" + linenum + " M05\nN" + linenum++ +
                                        " M02\nN" + linenum++ + " M30\nN" + linenum++ +
                                        " %\n";
                        gcode.append(endSeq);

                        //Write all the information to the file
                        fileWriter.write(gcode.toString());
                }

        } catch (IOException enotherException) {
                System.err.println("Exception: " + enotherException );
        } finally {
                try {
                        if (fileWriter != null) {
                                fileWriter.close();
                        }
                } catch(IOException ex) {
                        ex.printStackTrace();
                }
        }
}

}


/**
 *
 * SETTING UP AND DISPLAYING THE GUI
 *
**/

class MyFavoritePanel extends JPanel {
int oldx, oldy, newx, newy;
ArrayList<Line> lines = new ArrayList<Line> ();

public MyFavoritePanel() {
        setBorder(BorderFactory.createLineBorder(Color.black));

        addMouseListener(new MouseAdapter() {
                        public void mousePressed(MouseEvent e) {
                                oldx = e.getX();
                                oldy = e.getY();
                                newx = oldx;
                                newy = oldy;
                        }
                });

        addMouseMotionListener(new MouseAdapter() {
                        public void mouseDragged(MouseEvent e) {
                                drawing(e.getX(),e.getY());
                        }
                });


}

private void drawing(int x, int y) {

        if ((oldx!=x) || (oldy!=y)) {
                oldx = newx;
                oldy = newy;
                newx=x;
                newy=y;

                Line l = new Line(oldx, oldy, newx, newy, .5);
                lines.add(l);
                repaint();
        }
}

public ArrayList<Line>  getLines (){
        return lines;
}

public Dimension getPreferredSize() {
        return new Dimension(250,200);
}

protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for (Line l : lines) {
                g2.setStroke(new BasicStroke((float)(l.getLineWidth())));
                g.drawLine((int)Math.round(l.getStartX()), (int)Math.round(l.getStartY()),
                           (int)Math.round(l.getEndX()), (int)Math.round(l.getEndY()));

        }
}
}
