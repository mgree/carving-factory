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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Window;
import java.awt.Window.*;


import java.lang.Object;
import java.util.*;
import java.io.*;

//doesn't understand fast moves
//set starting sequence


public class GCodeGenerator {

private static StringBuilder gcode = new StringBuilder();
private static final String FILENAME = "outputGCode.txt";
private static MyFavoritePanel thePanel = new MyFavoritePanel();
private static Boolean closed = false;


public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                createAndShowGUI();
                        }
                });






}

private static void createAndShowGUI() {

        JFrame f = new JFrame("GCodeGenerator");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(thePanel);
        f.pack();
        f.setVisible(true);

        f.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                          BufferedWriter fileWriter = null;
                          try{
                                  fileWriter = new BufferedWriter(new FileWriter(FILENAME));

                                  ArrayList<Line> lines = thePanel.getLines();
                                  for (Line l : lines) {
                                          gcode.append(l.toGCode());
                                  }
                                  fileWriter.write(gcode.toString());
                          } catch (IOException enotherException) {
                                  System.err.println("Exception: " + enotherException );
                          } finally {
                                  try {
                                          if (fileWriter != null) {
                                                  fileWriter.close();
                                          }
                                  } catch( IOException ex) {
                                          ex.printStackTrace();
                                  }
                          }
                        }

                });




}
}

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
