package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Full-screen drawing view.
 *
 * Created by edinameshietedoho on 5/25/17.
 */

public class DrawingView extends View {
    private Stroke curStroke;
    private List<Stroke> strokes = new LinkedList<>();
    private final Paint brush = new Paint();
    private FragmentManager manager;
    private String alphas;
    private int aC;

    public DrawingView(Context ctx) {
        super(ctx);

        initializeBrush();
    }

    public DrawingView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        initializeBrush();
    }

    private void initializeBrush() {
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeWidth(5);
        brush.setARGB(255, 0, 0, 0);
        brush.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int curColor = brush.getColor();

        for (Stroke s : strokes) {
            brush.setColor(s.getColor());
            canvas.drawPath(s.getPath(), brush);
        }

        brush.setColor(curColor);

        if (curStroke != null) {
            canvas.drawPath(curStroke.getPath(), brush);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("UI", event.toString());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                curStroke = new Stroke(brush.getColor());
                addMotionEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                addMotionEvent(event);
                break;
            case MotionEvent.ACTION_UP:
                addMotionEvent(event);
                Log.d("TOUCH", curStroke.toString());
                strokes.add(curStroke);
                curStroke = null;
                break;
            default:
                break;
        }

        return true;
    }


    public void clear() {
        curStroke = null;
        strokes.clear();
        invalidate();
    }

    public void undo() {
        if (strokes.size() > 0) {
            strokes.remove(strokes.size() - 1);
            invalidate();
        }
    }

    public void setAlpha(int alpha) {
        brush.setAlpha(alpha);
        // ??? Do anything about the current stroke?
    }

    private void addMotionEvent(MotionEvent event) {
        for (int h = 0; h < event.getHistorySize(); h += 1) {
            curStroke.addPoint(event.getHistoricalEventTime(h),
                    event.getHistoricalX(h),
                    event.getHistoricalY(h));
        }

        curStroke.addPoint(event.getEventTime(),
                event.getX(),
                event.getY());

        invalidate();
    }

    public void saveState(OutputStream state) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(state);

        out.writeInt(brush.getColor());
        out.writeObject(strokes);
        out.flush();
    }

    public void loadState(InputStream state) throws IOException {
        ObjectInputStream in = new ObjectInputStream(state);

        brush.setColor(in.readInt());
        try {
            strokes = (LinkedList) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

}