package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
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
    private float stockDepth = -1;
    private float cutoffRight = -1;
    private float cutoffBottom = -1;
    private String stockUnit = "undef";

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

        Log.d("DIM",String.format("canvas %1.4fx%1.4f, stock %1.4fx%1.4f, wScale %1.4f lScale %1.4f, scaled to %1.4f %1.4f",
                width, height,
                stockWidth, stockLength,
                wPPI, lPPI,
                cutoffRight, cutoffBottom));

        brush.setARGB(255,0,0,0);
        brush.setStyle(Paint.Style.FILL);
        Log.d("DIM",String.format("drawing rectangle at %1.4f %1.4f %1.4f %1.4f", cutoffRight, 0f, width, height));

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
                Log.d("TOUCH", String.format("cutoff %1.4f/%1.4f stroke %s", cutoffRight, cutoffBottom, curStroke.toString()));
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
        } else if (key.equals(DisplaySettingsActivity.KEY_WIDTH)) {
            stockDepth = sharedPreferences.getInt(key,0);
        } else if (key.equals(DisplaySettingsActivity.KEY_UNIT)) {
            stockUnit = sharedPreferences.getString(key,"in");
        }

        postInvalidate();
    }

    public void initializeStockDimensions(SharedPreferences prefs) {
        stockLength = prefs.getInt(DisplaySettingsActivity.KEY_LENGTH, -1);
        stockWidth = prefs.getInt(DisplaySettingsActivity.KEY_WIDTH, -1);
        stockDepth = prefs.getInt(DisplaySettingsActivity.KEY_DEPTH, -1);
    }


    public void exportPath(){
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        try {
            dir.mkdirs();

            // compute the filename
            StringBuffer filename = new StringBuffer();
            CharSequence timestamp = DateFormat.format("yyyy-MM-ddThh:mm:ss", new Date());
            filename.append("STROKELIST-");
            filename.append(timestamp);
            filename.append("_");
            filename.append(Float.toString(stockWidth));
            filename.append("x");
            filename.append(Float.toString(stockLength));
            filename.append(".txt"); // TODO: change back to prg (associate prg w/ text?)


            File prg = new File(dir, filename.toString());
            if (!prg.createNewFile()) {
                throw new IOException("couldn't create file " + filename.toString());
            }

            PrintWriter out = new PrintWriter(new FileOutputStream(prg, false));

            out.write(strokes.toString());

            out.flush();
            out.close();

            MediaScannerConnection.scanFile(getContext(),
                    new String[] { prg.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("IO", "Scanned " + path + ":");
                            Log.i("IO", "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            Log.d("IO", e.getMessage());
        }
    }

    public void exportGCode() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        try {
            dir.mkdirs();

            // compute the filename
            StringBuffer filename = new StringBuffer();
            CharSequence timestamp = DateFormat.format("yyyy-MM-ddThh:mm:ss", new Date());
            filename.append(timestamp);
            filename.append("_");
            filename.append(Float.toString(stockWidth));
            filename.append("x");
            filename.append(Float.toString(stockLength));
            filename.append(".txt"); // TODO: change back to prg (associate prg w/ text?)


            File prg = new File(dir, filename.toString());
            if (!prg.createNewFile()) {
                throw new IOException("couldn't create file " + filename.toString());
            }

            PrintWriter out = new PrintWriter(new FileOutputStream(prg, false));

            float oldStockDepth = stockDepth;
            stockDepth = 0.72f;

            // compute scale
            float ipp = stockWidth / cutoffRight;

            // write metadata comments
            out.println("(generated by CNC Carving Factory)");
            out.printf("(expected stock dimensions: %1.4fx%1.4fx%1.4f[override] %s)\n",
                    stockWidth, stockLength, stockDepth, stockUnit);

            // write prelude
            int line = 1;
            out.println("(prelude)");
            out.printf("N%d G00 G17 G40 G90\n", line++);
            out.printf("N%d G70\n", line++);
            out.printf("N%d G54\n", line++);
            out.printf("N%d T1 M06\n", line++);
            out.printf("N%d D1\n", line++);
            out.printf("N%d M03 S18000\n", line++);

            // write the strokes into the file in GCode format
            out.println("(carving)");

            float boardHeight = 0.495f + 0.72f;
            float clearancePlane = boardHeight + 0.25f;
            float cuttingDepth = boardHeight - 0.25f;

            int numStrokes = 0;
            for (Stroke s : strokes) {
                boolean cutting = false;
                boolean fast = false;

                numStrokes += 1;
                out.printf("(stroke %d)\n", numStrokes);

                PointF last = null;
                for (PointF point : s) {
                    if (point.equals(last)) {
                        continue;
                    }

                    if (!cutting) {
                        // emit G00 moves, move in w/slow feed rate (first iteration)
                        out.printf("N%d G00 X%1.4f Y%1.4f\n", line++, point.x * ipp, point.y * ipp);
                        out.printf("N%d G00 Z%1.4f\n", line++, clearancePlane);
                        out.printf("N%d G01 Z%1.4f F80.0\n", line++, cuttingDepth);

                        cutting = true;
                    } else {
                        if (!fast) {
                            // first G01 move, set high feedrate (second iteration)
                            out.printf("N%d G01 X%1.4f Y%1.4f F250.0\n", line++, point.x * ipp, point.y * ipp);

                            fast = true;
                        } else {
                            // general moves (other iterations)
                            out.printf("N%d G01 X%1.4f Y%1.4f\n", line++, point.x * ipp, point.y * ipp);
                        }
                    }

                    last = point;
                }

                // emit pull out
                out.printf("N%d G00 X%1.4f Y%1.4f\n", line++, last.x * ipp, last.y * ipp);
                out.printf("N%d G00 Z%1.4f\n", line++, clearancePlane);
            }

            // write outlude
            out.println("(closing arguments)");
            out.printf("N%d D0\n", line++);
            out.printf("N%d G00 Z0\n", line++);
            out.printf("N%d G53\n", line++);
            out.printf("N%d G00 Y0.0000\n", line++);
            out.printf("N%d G00 X0.0000 M05\n", line++);
            out.printf("N%d G54\n", line++);
            out.printf("N%d M30\n", line++);
            out.printf("N%d %%",line++ );

            out.flush();
            out.close();

            MediaScannerConnection.scanFile(getContext(),
                    new String[] { prg.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("IO", "Scanned " + path + ":");
                            Log.i("IO", "-> uri=" + uri);
                        }
                    });

            Toast.makeText(getContext(), "Saved to " + prg.toString(), Toast.LENGTH_SHORT);
        } catch (IOException e) {
            Log.d("IO", e.getMessage());
        }
    }

}