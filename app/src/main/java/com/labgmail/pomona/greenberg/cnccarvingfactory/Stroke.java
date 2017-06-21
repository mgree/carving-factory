package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Color;
import android.graphics.Path;
import android.util.Log;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Custom pseudo-wrapper on paths, so we can actually save them. (android.os.Path isn't Parcelable!)
 *
 * Created by edinameshietedoho and soniagrunwald on 5/25/17.
 */

@SuppressWarnings("WeakerAccess")
public class Stroke extends Path implements Serializable, Iterable<Anchor>{

    private float sWidth;
    private List<Anchor> points = new LinkedList<>();

    /**
     * Are we a degenerate stroke, i.e., a point?
     *
     * Call isDegenerate to find out. Degenerate strokes should be drawn as a point at their centroid.
     */
    private boolean degenerate = true;

    public Stroke(float sWidth) {
        this.sWidth = sWidth;
    }

    public Path getPath() {
        Path path = new Path();

        for (Anchor p : points) {
            if (path.isEmpty()) {
                path.moveTo(p.x, p.y);
            } else {
                path.lineTo(p.x, p.y);
            }
        }

        return path;
    }

    public void addPoint(float x, float y, float z, long t) {
        points.add(new Anchor(x, y, z, t));
    }

    /**
     * Gets the color of the stroke (based on the first point).
     *
     * TODO deprecate this in favor of drawing each segment as its own (gradient) color
     *
     * @return
     */
    public int getColor() {
        if (!points.isEmpty()) {
            return points.get(0).getColor();
        } else {
            return Color.argb(255, 0, 0, 0);
        }
    }

    public float getStrokeWidth() { return sWidth; }

    public int size() { return points.size(); }

    @Override
    public Iterator<Anchor> iterator() { return points.iterator(); }

    public Anchor centroid() {
        float meanX = 0;
        float meanY = 0;
        float meanZ = 0;
        long meanTime = 0;

        for (Anchor a : points) {
            meanX += a.x;
            meanY += a.y;
            meanZ += a.z;
            meanTime += a.time;
        }

        return new Anchor(meanX / points.size(), meanY / points.size(), meanZ / points.size(), meanTime / points.size());
    }

    public boolean isDegenerate() {
        if (!degenerate) {
            return false;
        }

        Anchor centroid = centroid();

        for (Anchor a : points) {
            double distance = Math.sqrt(Math.pow(a.x - centroid.x, 2) + Math.pow(a.y - centroid.y, 2));
            if (distance >= 2 * sWidth) {
                Log.d("FIT", "non-degenerate distance " + distance + " " + sWidth);
                degenerate = false;
                return false;
            }
        }

        return true;
    }

    public Stroke fitToNatCubic(int everyN, int steps) {
        // select every everyN points from the user's input, split into X and Y components
        List<Double> selectedX = new LinkedList<>();
        List<Double> selectedY = new LinkedList<>();
        List<Double> selectedZ = new LinkedList<>();
        List<Long> selectedTime = new LinkedList<>();

        int i = 0;
        while (i < points.size()) {
            // save the current point
            Anchor a = points.get(i);
            selectedX.add((double) a.x);
            selectedY.add((double) a.y);
            selectedZ.add((double) a.z);
            selectedTime.add(a.time);

            // advance n steps (or stop at the end, keeping the last element)
            if (i + everyN < points.size()) {
                i += everyN;
            } else if (i < points.size() - 1) {
                i = points.size() - 1; // get the last point no matter what
            } else {
                i = points.size(); // stop the loop
            }
        }

        // fit curves
        Cubic[] fittedX = calcNatCubic(selectedX.toArray(new Double[selectedX.size()]));
        Cubic[] fittedY = calcNatCubic(selectedY.toArray(new Double[selectedY.size()]));
        Cubic[] fittedZ = calcNatCubic(selectedZ.toArray(new Double[selectedZ.size()]));

        // construct new stroke from curves
        Stroke fitted = new Stroke(sWidth);
        fitted.addPoint(fittedX[0].eval(0), fittedY[0].eval(0), points.get(0).z, selectedTime.get(0));
        for (i = 0; i < fittedX.length; i += 1) {
            for (int j = 1; j <= steps; j += 1) {
                double u = j / (double) steps;
                fitted.addPoint(fittedX[i].eval(u), fittedY[i].eval(u), fittedZ[i].eval(u), selectedTime.get(i));
            }
        }

        return fitted;
    }

    /**
     * calculates the natural cubic spline that interpolates y[0], y[1], ...
     * y[n] The first segment is returned as C[0].a + C[0].b*u + C[0].c*u^2 +
     * C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2], ... C[n-1]
     *
     * taken from Tim Lambert at
     * https://github.com/deric/curve-fit-demo/blob/master/src/main/java/org/clueminer/curve/fit/splines/NatCubic.java
     */
    private Cubic[] calcNatCubic(Double[] x) {
        int n = x.length - 1;

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
     * Taken from Tim Lambert at
     *  https://github.com/deric/curve-fit-demo/blob/master/src/main/java/org/clueminer/curve/fit/splines/Cubic.java
     */
    private static class Cubic {
        private double a, b, c, d;

        public Cubic(double a, double b, double c, double d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public float eval(double x) {
            return (float) ((((d * x) + c) * x + b) * x + a);
        }
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        for(Anchor a : points) {
            string.append(a.toString());
            string.append("\n");
        }
        return string.toString();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(sWidth);
        out.writeObject(points);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        sWidth = in.readFloat();

        int size = in.readInt();
        try {
            points = (LinkedList<Anchor>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

}