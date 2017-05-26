package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by edinameshietedoho on 5/25/17.
 */

public class Stroke extends Path implements Parcelable {

    private int color;
    private List<PointF> points = new LinkedList<PointF>();

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

    public void addPoint(long time, float x, float y) {
        points.add(new PointF(x, y));
    }

    public int getColor() {
        return color;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeList(points);
        out.writeInt(color);
    }

    public static final Parcelable.Creator<Stroke> CREATOR = new Parcelable.Creator<Stroke>() {
        public Stroke createFromParcel(Parcel in) {
            return new Stroke(in);
        }

        public Stroke[] newArray(int size) {
            return new Stroke[size];
        }
    };

    private Stroke(Parcel in) {
        in.readList(points, null);
        color = in.readInt();
    }

    public int describeContents() {
        return 0;
    }
}
