

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
        JFrame f = new JFrame("BSpline Practice");
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


}
}
