package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by edinameshietedoho on 5/25/17.
 */

public class Stroke extends Path {

    private Paint pickedPaint;
    private List<PointF> points = new LinkedList<PointF>();

    public Stroke(Paint paints) {
        this.pickedPaint = paints;
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

    public Paint getPaint() {
        return pickedPaint;
    }
}
