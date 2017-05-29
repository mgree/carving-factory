package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

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
    private int lStroke;

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
        Log.d("UI",event.toString());

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
        strokes.remove(strokes.size() - 1);
        invalidate();
    }

    private void addMotionEvent(MotionEvent event) {
        for (int h = 0; h < event.getHistorySize(); h+= 1) {
            curStroke.addPoint(event.getHistoricalEventTime(h),
                    event.getHistoricalX(h),
                    event.getHistoricalY(h));
        }

        curStroke.addPoint(event.getEventTime(),
                event.getX(),
                event.getY());

        invalidate();
    }

    // TODO: we don't actually need all of this mess---we should implement onPause and onResume in DrawingActivity

    @Override
    public Parcelable onSaveInstanceState() {
        Log.d("SAVE", String.format("saving with %d strokes", strokes.size()));
        return new DrawingSavedState(super.onSaveInstanceState(),
                strokes,
                brush.getColor());
    }

    @Override
    public void onRestoreInstanceState(Parcelable rawState) {
        super.onRestoreInstanceState(rawState);

        DrawingSavedState state = (DrawingSavedState) rawState;
        this.strokes = state.strokes;
        brush.setColor(state.color);
        Log.d("SAVE", String.format("restoring with %d strokes", strokes.size()));
    }

    public static class DrawingSavedState extends View.BaseSavedState {
        private final List<Stroke> strokes;
        private final int color;

        public DrawingSavedState(Parcelable superState, List<Stroke> strokes, int color) {
            super(superState);
            this.strokes = strokes;
            this.color = color;
        }

        private DrawingSavedState(Parcel in) {
            super(in);
            strokes = new LinkedList<>();
            in.readList(strokes, null);
            color = in.readInt();
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeList(strokes);
            out.writeInt(color);
        }

        public static Parcelable.Creator<DrawingSavedState> CREATOR = new Parcelable.Creator<DrawingSavedState>() {
            public DrawingSavedState createFromParcel(Parcel source) {
                return new DrawingSavedState(source);
            }

            public DrawingSavedState[] newArray(int size) {
                return new DrawingSavedState[size];
            }
        };
    }
}
