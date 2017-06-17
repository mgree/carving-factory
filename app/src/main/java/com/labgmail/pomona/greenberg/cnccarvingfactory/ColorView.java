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


// TODO: Use gradient and shape to create a gradient of color.
public class ColorView extends android.support.v7.widget.AppCompatImageButton {

    private int mAlpha = 255;
    private final Paint paint = new Paint();

    public float clickX,clickY,eventX;

    public ColorView(Context ctx) { this(ctx, null, 0); }

    public ColorView(Context ctx, AttributeSet attrs) { this(ctx, attrs, 0); }

    public ColorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setImageDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] { 0xff000000, 0x00000000 }));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("COLOR","drawing line at " + clickY);

        float y = (1 - ((float) (mAlpha / 255f))) * canvas.getHeight();
        canvas.drawLine(0, y, canvas.getWidth(), y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // proportion on the widget may go out of [0,1], so we bound it
        float proportionDownWidget = Math.max(Math.min(event.getY() / getHeight(), 1), 0);

        mAlpha = Math.round(255 * (1 - proportionDownWidget));
        postInvalidate();

        Log.d("COLOR",Integer.toString(mAlpha));
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.d("COLOR","clicked @ " + mAlpha);
            performClick();
//             updateLine(); breaks code
        }

        //Works but problem is it pops up for all the values passed over until the final selection
//        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//        builder.setCancelable(true);
//        builder.setTitle("Alpha Value");
//        builder.setMessage(" " + mAlpha);
//
//        AlertDialog dialog = builder.create();
//        dialog.show();

        return true;
    }

    public int getAlphaSelected() {
        return mAlpha;
    }
}
