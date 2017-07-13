package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Swatch-based color picker.
 *
 * Created by edinameshietedoho on 6/15/17.
 */

public class DepthSwatch extends android.support.v7.widget.AppCompatImageButton {

    private float depth = 1.0f;
    private final Paint paint = new Paint();

    public float clickY;

    public DepthSwatch(Context ctx) { this(ctx, null, 0); }

    public DepthSwatch(Context ctx, AttributeSet attrs) { this(ctx, attrs, 0); }

    public DepthSwatch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.setBackgroundColor(Color.WHITE);

        setImageDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] { Color.BLACK, Color.WHITE}));

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("COLOR","drawing line at " + clickY);

        float y = (1 - depth) * canvas.getHeight();
        canvas.drawLine(0, y, canvas.getWidth(), y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // proportion on the widget may go out of [0,1], so we bound it
        float proportionDownWidget = Math.max(Math.min(event.getY() / getHeight(), 1), 0);

        depth = 1 - proportionDownWidget;
        postInvalidate();

        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }

        return true;
    }

    /**
     *
     * @return 1.0 for maximum depth to 0.0 for minimum depth
     */
    public float getDepthSelected() {
        return depth;
    }

    public void setDepth(float theDepth) {
        depth = theDepth;
        postInvalidate();
    }
}
