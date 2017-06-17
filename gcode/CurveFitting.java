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


public class CurveFitting {
static int steps = 2;
static double scalar = .75;
static Point2D.Double[] theCurvePoints;
static double[] xpoints;
static double[] ypoints;

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

                xpoints = new double[pointList.size()];
                ypoints = new double[pointList.size()];

                int i = 0;
                for (PrgPointsParser.PointContext p : pointList) {
                        double x = (double)toFloat(p.xCoord().floatNum());
                        double y = (double)toFloat(p.yCoord().floatNum());
                        xpoints[i] = x;
                        ypoints[i] = y;
                        i++;
                }



                theCurvePoints = curvePoints(xpoints, ypoints);



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
        f.add(new MyPanel(theCurvePoints, xpoints, ypoints, scalar));
        f.setPreferredSize(new Dimension(800, 800));
        f.pack();
        f.setVisible(true);
}



/* Creates a float from a NaturalContext to help translate a command context to
 * a command
 */
private static float toFloat(PrgPointsParser.FloatNumContext fnc) {
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


/**
 * calculates the natural cubic spline that interpolates y[0], y[1], ...
 * y[n] The first segment is returned as C[0].a + C[0].b*u + C[0].c*u^2 +
 * C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2], ... C[n-1]
 * @param n @param x @return
 */
public static Cubic[] calcNaturalCubic(int n, double[] x) {
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

        /* now compute the coefficients of the cubics */
        Cubic[] C = new Cubic[n];
        for (i = 0; i < n; i++) {
                C[i] = new Cubic((float) x[i], D[i], 3 * (x[i + 1] - x[i]) - 2 * D[i] - D[i + 1],
                                 2 * (x[i] - x[i + 1]) + D[i] + D[i + 1]);
        }
        return C;
}


public static Point2D.Double[] curvePoints(double[] xpoints, double[] ypoints) {
        int numPts = xpoints.length;
        Point2D.Double[] curve;
        if (numPts >= 2) {
                Cubic[] X = calcNaturalCubic(numPts - 1, xpoints);
                Cubic[] Y = calcNaturalCubic(numPts - 1, ypoints);

                curve = new Point2D.Double[X.length * steps + 1];
                curve[0] = new Point2D.Double(X[0].eval(0), Y[0].eval(0));
                int k = 1;
                for (int i = 0; i < X.length; i++) {
                        for (int j = 1; j <= steps; j++) {
                                double u = j / (double) steps;
                                curve[k++] = new Point2D.Double(X[i].eval(u), Y[i].eval(u));
                        }
                }
        } else {
                return new Point2D.Double[0];
        }
        return curve;
}

}




//The panel sets up and displays the GUI in the end
class MyPanel extends JPanel {

Point2D.Double[] curvePoints;
double[] xpoints, ypoints;
double scalar;

public MyPanel(Point2D.Double[] theCurvePoints, double[] xpts, double[] ypts, double scalar) {
        setBorder(BorderFactory.createLineBorder(Color.black));
        curvePoints = theCurvePoints;
        xpoints = xpts;
        ypoints = ypts;
        this.scalar = scalar;
}

public Dimension getPreferredSize() {
        return new Dimension(500, 500);
}

protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        double prevX = 0;
        double prevY = 0;

        //Draw the OG points
        for (int i = 0; i < xpoints.length; i++) {
                g2.setStroke(new BasicStroke(4f));
                g.drawLine((int)Math.round(xpoints[i]*scalar), (int)Math.round(ypoints[i]*scalar),
                           (int)Math.round(xpoints[i]*scalar), (int)Math.round(ypoints[i]*scalar));
        }


        g2.setStroke(new BasicStroke(1f));

        //draw the curve
        if (curvePoints.length > 1) {
                prevX = curvePoints[0].getX();
                prevY = curvePoints[0].getY();
        }
        for (int i = 1; i < curvePoints.length; i++) {
                g.drawLine((int)Math.round(prevX*scalar), (int)Math.round(prevY*scalar),
                           (int)Math.round(curvePoints[i].getX()*scalar), (int)Math.round(curvePoints[i].getY()*scalar));
                prevX = curvePoints[i].getX();
                prevY = curvePoints[i].getY();
        }


}
}
