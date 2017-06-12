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
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Custom pseudo-wrapper on paths, so we can actually save them. (android.os.Path isn't Parcelable!)
 *
 * Created by edinameshietedoho on 5/25/17.
 */

public class Stroke extends Path implements Serializable, Iterable<PointF> {

    // TODO: Colors!

    private int color;
    private List<PointF> points = new LinkedList<>();

    public Stroke(int color) {
        this.color = color;
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

    @Override
    public String toString() {
        return points.toString();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(color);

        out.writeInt(points.size());
        for (PointF p : points) {
            out.writeFloat(p.x);
            out.writeFloat(p.y);
        }

        out.flush();
    }

    private void readObject(ObjectInputStream in) throws IOException {
        color = in.readInt();

        int size = in.readInt();
        points = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            float x = in.readFloat();
            float y = in.readFloat();
            points.add(new PointF(x,y));
        }
    }

    private void readObjectNoData() throws ObjectStreamException { }

    public static final long serialVersionUID = 42L;

    @Override
    public Iterator<PointF> iterator() {
        return points.iterator();
    }
}
