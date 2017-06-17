

/*
 * BSpline practice
 * @author Sonia Grunwald
 * @version June 16th, 2017
 * CNC Carving Factory Greenberg Lab
 *
 * Set the inputs and a BSpline will be generated
 */


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




public class BSplinePractice {

static BSpline bspline;
static Point2D.Double[] pointsToDraw;

public static void main(String[] args) {

        bspline = new BSpline();
        double[] xpoints = {10.0, 20.0, 50.0, 120.0};
        double[] ypoints = {30.0, 10.0, 40.0, 10.0};

        //I have no idea what numPts and steps specifically refer to
        pointsToDraw = bspline.curvePoints(xpoints, ypoints, 4, 4);


        for (Point2D.Double p : pointsToDraw) {
                if (p!=null) {
                        System.out.println(p.toString());
                } else {
                        System.out.println("P is real null");
                }
        }

        //Show the GUI
        SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                createAndShowGUI();
                        }
                });
}


//Create the GUI
private static void createAndShowGUI() {
        JFrame f = new JFrame("BSpline Practice");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new MyPanel(pointsToDraw));
        f.setPreferredSize(new Dimension(300, 300));
        f.pack();
        f.setVisible(true);
}

}


//The panel sets up and displays the GUI in the end
class MyPanel extends JPanel {

Point2D.Double[] pointsToDraw;

public MyPanel(Point2D.Double[] pointsToDraw) {
        setBorder(BorderFactory.createLineBorder(Color.black));
        this.pointsToDraw = pointsToDraw;
}

public Dimension getPreferredSize() {
        return new Dimension(250,200);
}

protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        Double prevX = -99.0;
        Double prevY = -99.0;
        if (pointsToDraw.length > 0) {
                prevX = pointsToDraw[0].getX();
                prevY = pointsToDraw[0].getY();
                g2.setStroke(new BasicStroke(2f));
                g.drawLine((int)Math.round(prevX), (int)Math.round(prevY), (int)Math.round(prevX),
                           (int)Math.round(prevY));
        }
        for (int i = 1; i < pointsToDraw.length; i++) {
                g.drawLine((int)Math.round(prevX), (int)Math.round(prevY), (int)Math.round(pointsToDraw[i].getX()),
                           (int)Math.round(pointsToDraw[i].getY()));

                prevX = pointsToDraw[i].getX();
                prevY = pointsToDraw[i].getY();
        }








}
}
