import java.awt.geom.Point2D;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStreams;
import java.lang.System;
import java.util.*;
import java.io.*;
import java.lang.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.event.*;


public class CurveFitting {

static CurvePointsList xpoints;
static CurvePointsList ypoints;

public static void main(String[] args) {

        //Wrong number of arguments
        if (args.length != 1) {
                System.err.println("Error: Please enter one file");
                System.exit(-1);
        }

        CharStream input;
        try {
                //Parse document
                input = CharStreams.fromFileName(args[0]);
                PrgPointsLexer lexer = new PrgPointsLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                PrgPointsParser parser = new PrgPointsParser(tokens);

                PrgPointsParser.DrawingContext ctx = parser.drawing();
                List<PrgPointsParser.PointContext> pointList = ctx.point();

                xpoints = new CurvePointsList ();
                ypoints = new CurvePointsList ();

                for (PrgPointsParser.PointContext p : pointList) {
                        xpoints.add((double)toFloat(p.xCoord().floatNum()));
                        ypoints.add((double)toFloat(p.yCoord().floatNum()));

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

//Create the GUI
private static void createAndShowGUI() {
        JFrame f = new JFrame("Curve Fitting Practice");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new MyPanel(xpoints, ypoints));
        f.setPreferredSize(new Dimension(800, 800));
        f.pack();
        f.setVisible(true);
}



/* Creates a float */
private static float toFloat(PrgPointsParser.FloatNumContext fnc) {
        //Save the sign
        String sign = " ";
        if (fnc.sign() != null ) {
                sign = fnc.sign().getText();
        }
        List<TerminalNode> dcs = fnc.DIGIT(); //first part of num
        StringBuffer digits = new StringBuffer(dcs.size());
        for (TerminalNode d : dcs) {
                digits.append(d.getSymbol().getText());
        }
        PrgPointsParser.DecimalContext decctx = fnc.decimal();
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

}





//The panel sets up and displays the GUI in the end
class MyPanel extends JPanel {

//Set up x and y point lists;
CurvePointsList xpoints, ypoints;

//set up default values that can be altered through the GUI
double scalar = .75;
int calculatedCurveSkips = 1;
int ogPointsSkips = 1;
int steps = 1;
boolean curvePts = false;
boolean ogPts = true;


public MyPanel(CurvePointsList xpts, CurvePointsList ypts) {
        setBorder(BorderFactory.createLineBorder(Color.black));

        xpoints = xpts; //the original points (x)
        ypoints = ypts; //the original points (y)

        //Calculate the curve points
        ArrayList <Point2D.Double> curvePoints = curvePoints(xpoints, ypoints);

        //Set up all GUI elements
        JSlider scaleSlider = new JSlider(1, 200, 75);
        JLabel scaleText = new JLabel("Scale: " + Double.toString(scalar));

        JSlider curveSkipSlider = new JSlider(1, (int)curvePoints.size(), 1);
        JLabel curveSkipText = new JLabel("Showing the curve: Pick 1 out of every " + Integer.toString(calculatedCurveSkips) + " calculated points");

        JSlider OGPointSkipSlider = new JSlider(1, (int)xpoints.size(), 1);
        JLabel OGPointSkipText = new JLabel("Calculating the Curve: Pick 1 out of every " + Integer.toString(ogPointsSkips) + " original points");

        JSlider stepSlider = new JSlider(1, 50, 1);
        JLabel stepText = new JLabel("Step " + Integer.toString(steps));

        JToggleButton ogPtsBtn = new JToggleButton("Show Original Points", true);
        JToggleButton curvePtsBtn = new JToggleButton ("Show Curve Points", false);

        scaleSlider.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                                scalar = scaleSlider.getValue() / 100.0;
                                scaleText.setText("Scale: " + Double.toString(scalar));
                                repaint();
                        }
                });

        curveSkipSlider.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                                calculatedCurveSkips = curveSkipSlider.getValue();
                                curveSkipText.setText("Showing the curve: Pick 1 out of every " + Integer.toString(calculatedCurveSkips) + " calculated points");
                                repaint();
                        }
                });

        OGPointSkipSlider.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                                ogPointsSkips = OGPointSkipSlider.getValue();
                                OGPointSkipText.setText("Calculating the Curve: Pick 1 out of every " + Integer.toString(ogPointsSkips) + " original points");
                                repaint();
                        }
                });

        stepSlider.addChangeListener(new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                                steps = stepSlider.getValue();
                                stepText.setText("Step " + Integer.toString(steps));
                                repaint();
                        }
                });

        ogPtsBtn.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                                ogPts = ogPtsBtn.isSelected();
                                repaint();
                        }
                });

        curvePtsBtn.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                                curvePts = curvePtsBtn.isSelected();
                                repaint();
                        }
                });





        this.add(scaleText);
        this.add(scaleSlider);
        this.add(stepText);
        this.add(stepSlider);
        this.add(OGPointSkipText);
        this.add(OGPointSkipSlider);
        this.add(curveSkipText);
        this.add(curveSkipSlider);
        this.add(new JLabel ("                                    "));
        this.add(ogPtsBtn);
        this.add(curvePtsBtn);

}

public Dimension getPreferredSize() {
        return new Dimension(500, 500);
}


/**
 * calculates the natural cubic spline that interpolates y[0], y[1], ...
 * y[n] The first segment is returned as C[0].a + C[0].b*u + C[0].c*u^2 +
 * C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2], ... C[n-1]
 * @param n @param x @return
 */
public Cubic[] calcNaturalCubic(int n, double[] x) {

        double[] gamma = new double[n + 1];
        double[] delta = new double[n + 1];
        double[] D = new double[n + 1];
        int i;

        gamma[0] = 1.0f / 2.0f;
        for (i = 1; i < n; i++) {
                gamma[i] = 1 / (4 - gamma[i - 1]);
        }
        gamma[n] = 1 / (2 - gamma[n - 1]);

        delta[0] = 3 * (x[1] - x[0]) * gamma[0];
        for (i = 1; i < n; i++) {
                delta[i] = (3 * (x[i + 1] - x[i - 1]) - delta[i - 1]) * gamma[i];
        }
        delta[n] = (3 * (x[n] - x[n - 1]) - delta[n - 1]) * gamma[n];

        D[n] = delta[n];
        for (i = n - 1; i >= 0; i--) {
                D[i] = delta[i] - gamma[i] * D[i + 1];
        }

        Cubic[] C = new Cubic[n];
        for (i = 0; i < n; i++) {
                C[i] = new Cubic(x[i], D[i], 3 * (x[i + 1] - x[i]) - 2 * D[i] - D[i + 1],
                                 2 * (x[i] - x[i + 1]) + D[i] + D[i + 1]);
        }
        return C;
}




/*
 * Calculates the points defining a curve
 */
public ArrayList<Point2D.Double> curvePoints(CurvePointsList xpts, CurvePointsList ypts) {
        int numPts = xpts.size();

        ArrayList<Point2D.Double> curve = new ArrayList<Point2D.Double>();
        if (numPts >= 2) {

                Double[] xpoints = xpts.toArray();
                Double [] ypoints = ypts.toArray();
                Cubic[] X = calcNaturalCubic(numPts - 1, toPrimitive(xpoints));
                Cubic[] Y = calcNaturalCubic(numPts - 1, toPrimitive(ypoints));

                curve.add(0, (new Point2D.Double(X[0].eval(0), Y[0].eval(0))));
                int k = 1;
                for (int i = 0; i < X.length; i++) {
                        for (int j = 1; j <= steps; j++) {
                                double u = j / (double) steps;
                                curve.add(k++, (new Point2D.Double(X[i].eval(u), Y[i].eval(u))));
                        }
                }
        }
        return curve;
}


//Helper function to convert from Double[] to double[]
private double[] toPrimitive(Double[] array) {
        if (array == null || array.length == 0) {
                return null;
        }
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
                result[i] = array[i].doubleValue();
        }
        return result;
}


/*
 * Repaints all elements from the given conditions
 */
protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        double prevX = 0;
        double prevY = 0;

        //Use the iterator to create lists using every nth element
        CurvePointsList.CurvePointsIterator xitr = xpoints.iterator();
        CurvePointsList.CurvePointsIterator yitr = ypoints.iterator();
        CurvePointsList currXPoints = xitr.getNthList(ogPointsSkips);
        CurvePointsList currYPoints = yitr.getNthList(ogPointsSkips);

        //Recalculate the curve points
        ArrayList<Point2D.Double> curvePoints = curvePoints(currXPoints, currYPoints);

        //If you want the original points, draw them
        if (ogPts) {
                g2.setStroke(new BasicStroke(4f));
                for (int i = 0; i < currXPoints.size(); i++) {
                        g.drawLine((int)Math.round(currXPoints.get(i)*scalar), (int)Math.round(currYPoints.get(i)*scalar),
                                   (int)Math.round(currXPoints.get(i)*scalar), (int)Math.round(currYPoints.get(i)*scalar));
                }
        }

        //If you want the curve points, draw them
        if (curvePts) {
                g2.setStroke(new BasicStroke(4f));
                for (int i = 0; i < curvePoints.size(); i += calculatedCurveSkips) {
                        g.drawLine((int)Math.round(curvePoints.get(i).getX()*scalar), (int)Math.round(curvePoints.get(i).getY()*scalar),
                                   (int)Math.round(curvePoints.get(i).getX()*scalar), (int)Math.round(curvePoints.get(i).getY()*scalar));
                }
        }


        //No matter what, draw the curve
        g2.setStroke(new BasicStroke(1f));
        if (curvePoints.size() > 1) {
                prevX = curvePoints.get(0).getX();
                prevY = curvePoints.get(0).getY();
        }
        for (int i = 1; i < curvePoints.size(); i += calculatedCurveSkips) {
                g.drawLine((int)Math.round(prevX*scalar), (int)Math.round(prevY*scalar),
                           (int)Math.round(curvePoints.get(i).getX()*scalar), (int)Math.round(curvePoints.get(i).getY()*scalar));
                prevX = curvePoints.get(i).getX();
                prevY = curvePoints.get(i).getY();
        }

}
}
