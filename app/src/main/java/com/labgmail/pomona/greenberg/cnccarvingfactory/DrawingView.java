package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by edinameshietedoho on 5/25/17.
 */

public class DrawingView extends View {
    private Path curPath;
    private List<Path> strokes = new LinkedList<Path>();


    public DrawingView(Context ctx) { super(ctx); }

    public DrawingView(Context ctx, AttributeSet attrs) { super(ctx, attrs); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint brush = new Paint();
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeWidth(5);
        brush.setARGB(255, 0, 0, 0);

        for (Path p : strokes) {
            canvas.drawPath(p, brush);
        }

        if (curPath != null) {
            canvas.drawPath(curPath, brush);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("UI",event.toString());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                curPath = new Path();
                addMotionEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                addMotionEvent(event);
                break;
            case MotionEvent.ACTION_UP:
                addMotionEvent(event);
                Log.d("TOUCH", curPath.toString());
                strokes.add(curPath);
                curPath = null;
                break;
            default:
                break;
        }

        return true;
    }

    protected void addTouchCoordinate(long time, float x, float y) {
        if (curPath.isEmpty()) {
            curPath.moveTo(x,y);
        } else {
            curPath.lineTo(x,y);
        }
    }

    protected void addMotionEvent(MotionEvent event) {
        for (int h = 0; h < event.getHistorySize(); h+= 1) {
            addTouchCoordinate(event.getHistoricalEventTime(h),
                    event.getHistoricalX(h),
                    event.getHistoricalY(h));
        }

        addTouchCoordinate(event.getEventTime(),
                event.getX(),
                event.getY());

        invalidate();
    }
}
