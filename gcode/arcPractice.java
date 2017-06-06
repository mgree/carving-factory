

/*
 * Arc practice
 * @author Sonia Grunwald
 * @version June 6th, 2017
 * CNC Carving Factory Greenberg Lab
 *
 * Set the x0, y0, x1, y1, and radius it will draw you an arc
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




public class arcPractice {

public static void main(String[] args) {
        //Show the GUI
        SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                createAndShowGUI();
                        }
                });
}


//Create the GUI
                private static void createAndShowGUI() {
        JFrame f = new JFrame("GCode Results");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new MyPanel());
        f.setPreferredSize(new Dimension(300, 300));
        f.pack();
        f.setVisible(true);
        }

}


//The panel sets up and displays the GUI in the end
class MyPanel extends JPanel {

public MyPanel() {
        setBorder(BorderFactory.createLineBorder(Color.black));
}

public Dimension getPreferredSize() {
        return new Dimension(250,200);
}

protected void paintComponent(Graphics g) {
        //these are inputs
        int r = 50;
        int x0 = 160;
        int y0 = 80;
        int x1 = 120;
        int y1 = 85;
        double d; //distance between points
        double m1;      //the midpoint x
        double m2;      //the midpoint y
        double u;       //the normal
        double v;       //the normal
        double h;       //height (distance to center)
        int e = 1;      //counterclockwise pt 1 to pt2 is 1
                        //clockwise pt 1 to pt 2 is -1
        double a, b;    //the x and y of the center

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        //draw the start and end points
        g2.setStroke(new BasicStroke(3));
        g.drawLine(x0,y0,x0,y0);
        g.drawLine(x1,y1,x1,y1);

        //draw the line between them
        g2.setStroke(new BasicStroke(1));
        g.drawLine(x0,y0,x1,y1);

        //calculate the distance between them
        d = Math.sqrt(Math.pow((x1-x0), 2) + Math.pow((y1-y0), 2));

        //find the midpoint
        m1 = (x0+x1)/2;
        m2 = (y0 + y1)/2;

        //find the normal
        u = (x1-x0)/d;
        v = (y1 - y0)/d;

        //find the height
        h = Math.sqrt(Math.pow(r,2) - (Math.pow(d,2)/4));

        //get the center
        a = m1- e*h*v;
        b = m2 + e*h*u;

        //drawing results
        //points to center
        g.drawLine(x0,y0, (int)Math.round(a),(int)Math.round(b));
        g.drawLine(x1,y1, (int)Math.round(a), (int)Math.round(b));

        //draw center
        g2.setStroke(new BasicStroke(5));
        g.drawLine((int)Math.round(a),(int)Math.round(b),(int)Math.round(a),(int)Math.round(b));


        //find rectangle dimensions
        int rectx =  (int)Math.round(a - r);
        int recty = (int)Math.round(b - r);

        //draw bounding box
        g2.setStroke(new BasicStroke(1));
        g.drawRect(rectx, recty, 2*r, 2*r);

        //calculate angles
        double startAngle = (180/Math.PI * Math.atan2(b-y0, x0-a));
        double endAngle = (180/Math.PI * Math.atan2(b-y1, x1-a));
        double deltaAngle = endAngle - startAngle;

        //draw the arc
        g.drawArc(rectx, recty, 2*r, 2*r, (int)Math.round(startAngle), (int)Math.round(deltaAngle));
}
}
