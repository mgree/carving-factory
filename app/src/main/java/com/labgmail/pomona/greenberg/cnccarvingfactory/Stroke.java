package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Path;
import android.graphics.PointF;
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
 * Created by edinameshietedoho on 5/25/17.
 */

@SuppressWarnings("WeakerAccess")
public class Stroke extends Path implements Serializable, Iterable<PointF> {

    private int color;
    private float sWidth;
    private List<PointF> points = new LinkedList<>();

    /**
     * Are we a degenerate stroke, i.e., a point?
     *
     * Call isDegenerate to find out. Degenerate strokes should be drawn as a point at their centroid.
     */
    private boolean degenerate = true;

    public Stroke(int color, float sWidth) {
        this.color = color;
        this.sWidth = sWidth;
    }

    public Path getPath() {
        Path path = new Path();

        for (PointF p : points) {
            if (path.isEmpty()) {
                path.moveTo(p.x, p.y);
            } else {
                path.lineTo(p.x, p.y);
            }
        }

        return path;
    }

    public void addPoint(@SuppressWarnings("UnusedParameters") long time, float x, float y) {
        points.add(new PointF(x, y));
    }

    public int getColor() {
        return color;
    }

    public float getStrokeWidth() { return sWidth; }

    public int size() { return points.size(); }

    public List<PointF> getPoints() { return new LinkedList<>(points); }

    public PointF centroid() {
        float meanX = 0;
        float meanY = 0;

        for (PointF p : points) {
            meanX += p.x;
            meanY += p.y;
        }

        return new PointF(meanX / points.size(), meanY / points.size());
    }

    public boolean isDegenerate() {
        if (!degenerate) {
            return false;
        }

        PointF centroid = centroid();

        for (PointF p : points) {
            double distance = Math.sqrt(Math.pow(p.x - centroid.x, 2) + Math.pow(p.y - centroid.y, 2));
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

        int i = 0;
        while (i < points.size()) {
            // save the current point
            PointF p = points.get(i);
            selectedX.add((double) p.x);
            selectedY.add((double) p.y);

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

        // construct new stroke from curves
        Stroke fitted = new Stroke(color, sWidth);
        fitted.addPoint(0, fittedX[0].eval(0), fittedY[0].eval(0));
        for (i = 0; i < fittedX.length; i += 1) {
            for (int j = 1; j <= steps; j += 1) {
                double u = j / (double) steps;
                fitted.addPoint(0, fittedX[i].eval(u), fittedY[i].eval(u));
            }
        }

        return fitted;
    }

    /**
     * calculates the natural cubic spline that interpolates y[0], y[1], ...
     * y[n] The first segment is returned as C[0].a + C[0].b*u + C[0].c*u^2 +
     * C[0].d*u^3 0<=u <1 the other segments are in C[1], C[2], ... C[n-1]
     *
     * taken from TODO find the source
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

        for(PointF p : points  ) {
            string.append(p.toString());
            string.append("\n");
        }
        return string.toString();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(color);
        out.writeFloat(sWidth);

        out.writeInt(points.size());
        for (PointF p : points) {
            out.writeFloat(p.x);
            out.writeFloat(p.y);
        }

        out.flush();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        color = in.readInt();
        sWidth = in.readFloat();

        int size = in.readInt();
        points = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            float x = in.readFloat();
            float y = in.readFloat();
            points.add(new PointF(x,y));
        }
    }

    @SuppressWarnings({"EmptyMethod", "RedundantThrows", "unused"})
    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

    @Override
    public Iterator<PointF> iterator() {
        return points.iterator();
    }
}
