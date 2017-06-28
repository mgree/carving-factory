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
 * Created by edinameshietedoho and soniagrunwaldon 5/25/17.
 */

public class DrawingView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int SMOOTHING_FACTOR = 3;
    private static final int CURVE_STEPS = 20;
    private static final float MAX_SINGLE_CUT_DEPTH = .4f;
    private static final float MIN_RADIUS = .25f;

    private Stroke curStroke;
    private List<Stroke> strokes = new LinkedList<>();
    private final Paint brush = new Paint();
    private float stockLength = -1;
    private float stockWidth = -1;
    private float stockDepth = -1;
    private float spoilDepth = -1;

    public List<Tools> tools = new LinkedList<>();
    public Tools half_inch, quarter_inch, curTool;

    private float cuttingDiameter = -1;

    private float cutoffRight = -1;
    private float cutoffBottom = -1;

    private float curDepth = 1.0f; /* between 0 and 1.0, ratio of valid cutting depth */

    private String stockUnit = "undef";

    private boolean debugPoints = false;
    private boolean clearStrokes = false;
    private float scale;

    private DepthMap depthMap;
    private boolean initialized = false;

    public static enum Mode {
        MANUAL_DEPTH,
        OVERDRAW
    };

    // TODO make the mode changeable
    private Mode drawingMode = Mode.OVERDRAW;

    public DrawingView(Context ctx) { this(ctx, null); }

    public DrawingView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        this.setBackgroundColor(Color.WHITE);

        cutoffRight = getWidth();
        cutoffBottom = getHeight();

        initializeBrush();
        intializeTools();
    }

    private void initializeBrush() {
        brush.setStyle(Paint.Style.STROKE);
        brush.setARGB(255, 0, 0, 0);
        brush.setAntiAlias(true);
    }

    private void intializeTools() {
        half_inch = new Tools(1, 0.5f, 0.4f, 80f, 250f);
        quarter_inch = new Tools(2, 0.25f, 0.4f, 80f, 250f);

        tools.add(half_inch);
        tools.add(quarter_inch);

        curTool = tools.get(0);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float height = canvas.getHeight();
        float width = canvas.getWidth();

        float wPPI = stockWidth > 0 ? width / stockWidth  : 1;
        float lPPI = stockLength > 0 ? height / stockLength : 1;

        scale = Math.min(wPPI, lPPI);

        cutoffBottom = scale * stockLength;
        cutoffRight = scale * stockWidth;

        brush.setColor(Color.BLACK);
        brush.setStyle(Paint.Style.FILL);

        if (!initialized) {
            depthMap = new DepthMap(stockWidth, stockLength, MIN_RADIUS, scale);
            initialized = true;
        }

        // RIGHT
        canvas.drawRect(cutoffRight, 0, width, height, brush);
        // BOTTOM
        canvas.drawRect(0, cutoffBottom, width, height, brush);

        for (Stroke s : strokes) {
            drawStroke(canvas, s, brush, scale);

            if (debugPoints) {
                brush.setColor(Color.RED);
                brush.setStrokeWidth(1);
                for (Anchor a : s) {
                    canvas.drawPoint(a.x, a.y, brush);
                }
            }

        }

        if (curStroke != null) {
            drawStroke(canvas, curStroke, brush, scale);
        }
    }

    private void drawStroke(Canvas canvas, Stroke s, Paint brush, float scale) {
        brush.setStrokeWidth(s.getTDiameter() * scale);

        if (s.isDegenerate()) {
            brush.setStyle(Paint.Style.FILL);

            Anchor p = s.centroid();
            brush.setAlpha(p.getAlpha());
            canvas.drawCircle(p.x, p.y, s.getStrokeWidth() * scale / 2, brush);
            depthMap.addPoint(p);
        } else {
            brush.setStyle(Paint.Style.STROKE);

            Anchor last = null;
            for (Anchor a : s) {
                brush.setAlpha(a.getAlpha());
                if (last != null) {
                    canvas.drawLine(last.x, last.y, a.x, a.y, brush);
                }
                depthMap.addPoint(a);
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

    public Paint getPaint() {
        return brush;
    }

    private void saveStroke() {
        if (curStroke != null) {
            Stroke fitted = curStroke.fitToNatCubic(SMOOTHING_FACTOR, CURVE_STEPS);
            strokes.add(fitted);
        }

        curStroke = null;
    }


    public void clear() {
        curStroke = null;
        strokes.clear();
        depthMap.clear();
        invalidate();
    }

    public void undo() {  //add removing strokes here
        if (strokes.size() > 0) {
            strokes.remove(strokes.size() - 1);
            for (Anchor a : strokes.get(strokes.size() - 1)){
                depthMap.removePoint(a);
            }
            invalidate();
        }
    }

    public void setToolHalf() {
        curTool = tools.get(0);
    }

    public void setToolQuarter() {
        curTool = tools.get(1);
    }

    public void setDepth(float depth) {
        curDepth = depth;
    } //where this is used change things

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
                depthMap.addPoint(updatedPoint);
                z = updatedPoint.z;
                Log.d("DEPTH", "Current Z: " + z);

                break;

            case MANUAL_DEPTH:
                z = curDepth;
                break;
        }

        curStroke.addPoint(x, y, z, time);
    }

    public void saveState(OutputStream state) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(state);

        out.writeInt(brush.getAlpha());
        out.writeFloat(brush.getStrokeWidth());
        out.writeObject(strokes);
        out.flush();
    }

    public void loadState(InputStream state) throws IOException {
        ObjectInputStream in = new ObjectInputStream(state);

        brush.setAlpha(in.readInt());
        brush.setStrokeWidth(in.readFloat());
        try {
            if (clearStrokes) {
                strokes = new LinkedList<>();
                clearStrokes = false;
            } else {
                //noinspection unchecked
                strokes = (LinkedList<Stroke>) in.readObject();
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

                scaleTool(sharedPreferences.getString(DisplaySettingsActivity.KEY_TOOL, "0"));
                clearStrokes = true;

                break;
            case DisplaySettingsActivity.KEY_TOOL:
                scaleTool(sharedPreferences.getString(key, "0"));

                break;
            case DisplaySettingsActivity.KEY_SDEPTH:
                spoilDepth = Float.parseFloat(sharedPreferences.getString(key, "0"));
                break;
            default:
                Log.d("PREF", "unknown preference " + key);
        }

        postInvalidate();
    }

    private void scaleTool(String toolDim) {
        // NB all tool dimensions MUST be in inches
        float factor = 1.0f;
        if (stockUnit.equals("in")) {
            factor = 1.0f;
        } else if (stockUnit.equals("cm")){
            factor = 2.54f;
        } else {
            Log.d("DIM", "funny unit " + stockUnit);
        }
        // Gets the current Tool being used and gets Diameter of that instead of reading in the string from Settings.
        cuttingDiameter = curTool.getDiameter() * factor;
    }


    public void initializeStockDimensions(SharedPreferences prefs) {
        stockLength = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_LENGTH, "-1"));
        stockWidth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_WIDTH, "-1"));
        stockDepth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_DEPTH, "-1"));
//        cuttingDiameter = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_TOOL, "-1"));
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
                            stockWidth,stockLength,stockDepth,stockUnit,cutoffRight,
                            spoilDepth,cuttingDiameter);
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

            Toast.makeText(getContext(), String.format("Saved to %s (%d lines of G-code)", filename.toString(), gcg.numLines()), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Couldn't save file (" + e.getLocalizedMessage() + ")", Toast.LENGTH_SHORT).show();
            Log.d("IO", e.toString());
        }
    }

}