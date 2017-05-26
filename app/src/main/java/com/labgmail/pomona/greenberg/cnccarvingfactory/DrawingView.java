package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by edinameshietedoho on 5/25/17.
 */

public class DrawingView extends View {
    private Stroke curStroke;
    private List<Stroke> strokes = new LinkedList<Stroke>();
    private Paint brush = new Paint();

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

        for (Stroke s : strokes) {
            canvas.drawPath(s.getPath(), s.getPaint());
        }

        if (curStroke != null) {
            canvas.drawPath(curStroke.getPath(), brush);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("UI",event.toString());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                curStroke = new Stroke(brush);
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

    protected void addMotionEvent(MotionEvent event) {
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
}
