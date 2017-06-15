package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Path;
import android.graphics.PointF;

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

    // TODO: Colors!

    private int color;
    private float sWidth;
    private List<PointF> points = new LinkedList<>();

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
