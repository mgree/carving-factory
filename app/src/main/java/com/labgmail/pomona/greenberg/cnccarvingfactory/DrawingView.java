package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

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

public class DrawingView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Stroke curStroke;
    private List<Stroke> strokes = new LinkedList<>();
    private final Paint brush = new Paint();
    private float stockLength = -1;
    private float stockWidth = -1;
    private float cutoffRight = -1;
    private float cutoffBottom = -1;

    public DrawingView(Context ctx) { this(ctx, null); }

    public DrawingView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        cutoffRight = getWidth();
        cutoffBottom = getHeight();
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

        float height = canvas.getHeight();
        float width = canvas.getWidth();

        float wPPI = stockWidth > 0 ? width / stockWidth  : 1;
        float lPPI = stockLength > 0 ? height / stockLength : 1;

        float scale = Math.min(wPPI, lPPI);

        cutoffBottom = scale * stockLength;
        cutoffRight = scale * stockWidth;

        Log.d("DIM",String.format("canvas %fx%f, stock %fx%f, wScale %f lScale %f, scaled to %f %f",
                width, height,
                stockWidth, stockLength,
                wPPI, lPPI,
                cutoffRight, cutoffBottom));

        brush.setARGB(255,0,0,0);
        brush.setStyle(Paint.Style.FILL);
        Log.d("DIM",String.format("drawing rectangle at %f %f %f %f", cutoffRight, 0f, width, height));

        // RIGHT
        canvas.drawRect(cutoffRight, 0, width, height, brush);
        // BOTTOM
        canvas.drawRect(0, cutoffBottom, width, height, brush);

        //Multiply by the scale factor and the value the user gives and then open a window with those dimensions

        brush.setStyle(Paint.Style.STROKE);
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
                Log.d("TOUCH", String.format("cutoff %f/%f stroke %s", cutoffRight, cutoffBottom, curStroke.toString()));
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

    // ??? Is a redo button possible?
//    public void redo() {
    // get the state of the list before the last stroke was deleted than revert to the old state
    // way we're doing it now decreases the size of the array, doesn't really save anything.
//    }

    public void setAlpha(int alpha) {
        brush.setAlpha(alpha);
        // ??? Do anything about the current stroke?
    }

    private void addMotionEvent(MotionEvent event) {
        for (int h = 0; h < event.getHistorySize(); h += 1) {
            addPoint(event.getHistoricalEventTime(h),
                     event.getHistoricalX(h),
                     event.getHistoricalY(h));
        }

        addPoint(event.getEventTime(),
                 event.getX(),
                 event.getY());

        postInvalidate();
    }

    private void addPoint(long time, float x, float y) {
        curStroke.addPoint(time, Math.min(x, cutoffRight), Math.min(y, cutoffBottom));
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("DIM", "updated key " + key);

        if (key.equals(DisplaySettingsActivity.KEY_LENGTH)) {
            stockLength = sharedPreferences.getInt(key, 0);
        } else if (key.equals(DisplaySettingsActivity.KEY_WIDTH)) {
            stockWidth = sharedPreferences.getInt(key, 0);
        } /* TODO DEPTH */

        postInvalidate();
    }

    public void initializeStockDimensions(SharedPreferences prefs) {
        stockLength = prefs.getInt(DisplaySettingsActivity.KEY_LENGTH, -1);
        stockWidth = prefs.getInt(DisplaySettingsActivity.KEY_WIDTH, -1);
        // TODO DEPTH
    }
}