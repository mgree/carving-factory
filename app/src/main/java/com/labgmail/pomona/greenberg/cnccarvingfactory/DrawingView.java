package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.graphics.Bitmap;


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
 * Created by edinameshietedoho and soniagrunwald on 5/25/17.
 */

public class DrawingView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int SMOOTHING_FACTOR = 3;
    private static final int CURVE_STEPS = 20;
    private static final float MIN_RADIUS = .25f;

    private Stroke curStroke;
    private List<Stroke> strokes = new LinkedList<>();
    private final Paint brush = new Paint();
    private float stockLength = -1;
    private float stockWidth = -1;
    private float stockDepth = -1;
    private float spoilDepth = -1;

    public Tool curTool;

    private float cutoffRight = -1;
    private float cutoffBottom = -1;

    private float curDepth = 1.0f; /* between 0 and 1.0, ratio of valid cutting depth */

    private String stockUnit = "undef";

    private boolean debugPoints = false;
    private boolean clearStrokes = false;
    private float scale;

    private DepthMap depthMap;
    private boolean initialized = false;

    private float height, width;

    public enum Mode {
        MANUAL_DEPTH,
        OVERDRAW
    };

    private Bitmap drawing;

    // TODO make the mode changeable
    private Mode drawingMode = Mode.OVERDRAW;

    public DrawingView(Context ctx) { this(ctx, null); }

    public DrawingView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.setBackgroundColor(Color.WHITE);

        cutoffRight = getWidth();
        cutoffBottom = getHeight();

        initializeBrush();
        Log.d("STOOL", " " + curTool);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initialized = false;
    }

    private void initializeBrush() {
        brush.setStyle(Paint.Style.STROKE);
        brush.setARGB(255, 0, 0, 0);
        brush.setAntiAlias(true);
        brush.setStrokeJoin(Paint.Join.ROUND);
        brush.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        height = canvas.getHeight();
        width = canvas.getWidth();

        float wPPI = stockWidth > 0 ? width / stockWidth  : 1;
        float lPPI = stockLength > 0 ? height / stockLength : 1;

        scale = Math.min(wPPI, lPPI);

        cutoffBottom = scale * stockLength;
        cutoffRight = scale * stockWidth;

        if (!initialized) {
            drawing = Bitmap.createBitmap(Math.round(cutoffRight), Math.round(cutoffBottom), Bitmap.Config.ARGB_8888);
            redrawAll();

            depthMap = new DepthMap(stockWidth, stockLength, MIN_RADIUS, scale);
            initialized = true;
        }

        brush.setColor(Color.BLACK);
        brush.setStyle(Paint.Style.FILL);
        brush.setStrokeWidth(scale * curTool.getDiameter());
        // At start curTool doesn't have a value

        // RIGHT
        canvas.drawRect(cutoffRight, 0, width, height, brush);
        // BOTTOM
        canvas.drawRect(0, cutoffBottom, width, height, brush);

        //draw the old strokes
        canvas.drawBitmap(drawing, 0, 0, brush);

        //draw the current stroke
        if (curStroke != null) {
            drawStroke(canvas, curStroke, brush, scale);
        }
    }

    /* Will clear the canvas and redraw all the existing strokes */
    private void redrawAll() {
        // fresh canvas, clear the bitmap
        Canvas canvas = new Canvas(drawing);
        canvas.drawColor(Color.WHITE);

        for (Stroke s : strokes) {
            drawStroke(canvas, s, brush, scale);

            if (debugPoints) {
                brush.setColor(Color.RED);
                brush.setStrokeWidth(2);
                for (Anchor a : s) {
                    canvas.drawPoint(a.x, a.y, brush);
                }
            }
        }
    }

    /* Draws a single stroke */
    private void drawStroke(Canvas canvas, Stroke s, Paint brush, float scale) {
        float toolDim = scaleTool(s.getTDiameter());
        brush.setStrokeWidth(toolDim * scale);

        if (s.isDegenerate()) {
            brush.setStyle(Paint.Style.FILL);
            Anchor a = s.centroid();
            brush.setColor(a.getColor());
            canvas.drawCircle(a.x, a.y, toolDim * scale / 2, brush);
        } else {
            brush.setStyle(Paint.Style.STROKE);
            Anchor last = null;
            for (Anchor a : s) {
                brush.setColor(a.getColor());
                if (last != null) {
                    canvas.drawLine(last.x, last.y, a.x, a.y, brush);
                }
                last = a;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                addMotionEvent(event);
                break;
            case MotionEvent.ACTION_UP:
                addMotionEvent(event);
                saveStroke();
                break;
            default:
                break;
        }
        return true;

    }

    /* Saves a fitted stroke to the bitmap and stroke list */
    private void saveStroke() {
        if (curStroke != null) {
            Stroke fitted = curStroke.fitToNatCubic(SMOOTHING_FACTOR, CURVE_STEPS);
            strokes.add(fitted);

            for (Anchor a : fitted) {
                depthMap.addPoint(a);
            }
            Canvas canvas = new Canvas(drawing);
            drawStroke(canvas, fitted, brush, scale);
        }
        curStroke = null;
    }

    /* Clears the stroke list, depthmap, screen, and bitmap */
    public void clear() {
        curStroke = null;

        if (strokes != null) {
            strokes.clear();
        }

        if (depthMap != null) {
            depthMap.clear();
        }

        Canvas canvas = new Canvas(drawing);
        canvas.drawColor(Color.WHITE);

        postInvalidate();
    }

    /* Undoes the last stroke (redraws/remakes the bitmap) */
    public void undo(){
        int size = strokes.size();

        if (size > 0) {
            for (Anchor a : strokes.get(size - 1)){
                depthMap.removePoint(a);
            }
            strokes.remove(size - 1);
            redrawAll();
            postInvalidate();
        }
    }

    public void setTool(Tool tool) {
        curTool = tool;
    }

    public void setDepth(float depth) {
        curDepth = depth;
    }

    private void addMotionEvent(MotionEvent event) {
        for (int h = 0; h < event.getHistorySize(); h += 1) {
            addPoint(event.getHistoricalX(h),
                     event.getHistoricalY(h),
                     event.getHistoricalEventTime(h));
        }

        addPoint(event.getX(),
                 event.getY(),
                 event.getEventTime());

        postInvalidate();
    }

    /* Adds a point to the current stroke */
    private void addPoint(float x, float y, long time) {

        if (x > cutoffRight || y > cutoffBottom) {
            saveStroke();
            return;
        }
        if (curStroke == null) {
            curStroke = new Stroke(curTool);
        }
        float z = curDepth;
        switch (drawingMode) {

            case OVERDRAW:
                Anchor updatedPoint = depthMap.updateZ(new Anchor (x, y, z, time), curTool.getDiameter());
                curStroke.addPoint(updatedPoint);
                z = updatedPoint.z;
                break;

            case MANUAL_DEPTH:
                z = curDepth;
                break;
        }
        curStroke.addPoint(x, y, z, time);
    }

    public void saveState(OutputStream state) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(state);

        out.writeFloat(width);
        out.writeFloat(height);
        out.writeInt(brush.getColor());
        out.writeFloat(brush.getStrokeWidth());
        out.writeObject(strokes);
        out.writeObject(curTool);
        out.flush();
    }

    public void loadState(InputStream state) throws IOException {
        ObjectInputStream in = new ObjectInputStream(state);

        width = in.readFloat();
        height = in.readFloat();

        brush.setColor(in.readInt());
        brush.setStrokeWidth(in.readFloat() * scale);

        try {
            if (clearStrokes) {
                strokes = new LinkedList<> ();
                clear();
                clearStrokes = false;
            } else {
                //noinspection unchecked
                strokes = (LinkedList<Stroke>) in.readObject();
                curTool = (Tool) in.readObject();
                initialized = false;
                postInvalidate();
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case DisplaySettingsActivity.KEY_LENGTH:
                stockLength = Float.parseFloat(sharedPreferences.getString(key, "0"));
                break;
            case DisplaySettingsActivity.KEY_WIDTH:
                stockWidth = Float.parseFloat(sharedPreferences.getString(key, "0"));
                break;
            case DisplaySettingsActivity.KEY_DEPTH:
                stockDepth = Float.parseFloat(sharedPreferences.getString(key, "0"));
                break;
            case DisplaySettingsActivity.KEY_UNIT:
                stockUnit = sharedPreferences.getString(key, "in");
                scaleTool(Float.parseFloat(sharedPreferences.getString(DisplaySettingsActivity.KEY_TOOL, "0")));
                clearStrokes = true;
                clear();
                break;
            case DisplaySettingsActivity.KEY_TOOL:
                scaleTool(Float.parseFloat(sharedPreferences.getString(key, "0")));
                break;
            case DisplaySettingsActivity.KEY_SDEPTH:
                spoilDepth = Float.parseFloat(sharedPreferences.getString(key, "0"));
                break;
            default:
                Log.d("PREF", "unknown preference " + key);
        }

        postInvalidate();
    }

    private float scaleTool(float toolDim) {
        // NB all tool dimensions MUST be in inches
        float factor = 1.0f;
        if (stockUnit.equals("in")) {
            factor = 1.0f;
        } else {
            Log.d("DIM", "funny unit " + stockUnit);
        }
        return toolDim * factor;
    }


    public void initializeStockDimensions(SharedPreferences prefs) {
        stockLength = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_LENGTH, "-1"));
        stockWidth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_WIDTH, "-1"));
        stockDepth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_DEPTH, "-1"));
        spoilDepth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_SDEPTH, "-1"));
    }

    public void exportGCode() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        try {
            dir.mkdirs();

            // compute the filename
            StringBuilder filename = new StringBuilder();
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
            GCodeGenerator gcg =
                    GCodeGenerator.singlePass(strokes,
                            stockWidth, stockLength, stockDepth, stockUnit, cutoffRight,
                            spoilDepth);
            out.write(gcg.toString());
            out.close();

            MediaScannerConnection.scanFile(getContext(),
                    new String[] { prg.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("IO", "Scanned " + path + ":");
                            Log.i("IO", "-> uri=" + uri);
                        }
                    });

            Toast.makeText(getContext(), String.format("Saved file and image to %s (%d lines of G-code)", filename.toString(), gcg.numLines()), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(getContext(), "Couldn't save file (" + e.getLocalizedMessage() + ")", Toast.LENGTH_SHORT).show();
            Log.d("IO", e.toString());
        }
    }

    public void exportImage(){

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

            try {
                dir.mkdirs();

                StringBuilder filename = new StringBuilder();
                CharSequence timestamp = DateFormat.format("yyyy-MM-ddThh:mm:ss", new Date());
                filename.append(timestamp);
                filename.append("_Image.png");

                Integer counter = 0;
                File file = new File(dir, filename.toString());
                FileOutputStream fOut = new FileOutputStream(file);

                drawing.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();

                MediaScannerConnection.scanFile(getContext(), new String[] {file.getAbsolutePath()}, null, null);

            } catch (IOException e) {
                Toast.makeText(getContext(), "Couldn't save image (" + e.getLocalizedMessage() + ")", Toast.LENGTH_SHORT).show();
                Log.d("IO", e.toString());
            }





        }


}