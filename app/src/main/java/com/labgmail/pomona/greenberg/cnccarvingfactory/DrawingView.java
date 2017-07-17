package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.app.PendingIntent.getActivity;
import static android.support.v4.content.ContextCompat.startActivity;

/**
 * Full-screen drawing view.
 * <p>
 * Created by edinameshietedoho and soniagrunwald on 5/25/17.
 */

public class DrawingView extends View implements SharedPreferences.OnSharedPreferenceChangeListener {

    private boolean usingController = false;
    private static final int DRAWSPEED = 5;
    private boolean nowDrawing = false;
    private DepthSwatch depthSwatch;
    private float rTrigValue = 0.0f;
    private float lTrigValue = 0.0f;
    private StringBuilder errorLog = new StringBuilder();


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

    private Tool curTool = null;

    private float cutoffRight = -1;
    private float cutoffBottom = -1;

    private float curDepth = 1.0f; /* between 0 and 1.0, ratio of valid cutting depth */

    private String stockUnit = "undef";

    private boolean debugPoints = false;
    private boolean clearStrokes = false;
    private boolean isDrawing = false;
    private float scale;

    private DepthMap depthMap;
    private boolean initialized = false;

    private float height, width;

    private float currX, currY;

    public enum Mode {
        MANUAL_DEPTH,
        OVERDRAW
    }


    private Bitmap drawing;

    // TODO make the mode changeable
    private Mode drawingMode = Mode.OVERDRAW;

    public DrawingView(Context ctx) {
        this(ctx, null);
    }

    public DrawingView(Context ctx, AttributeSet attrs) {

        super(ctx, attrs);
        this.setBackgroundColor(Color.WHITE);

        cutoffRight = getWidth();
        cutoffBottom = getHeight();

        initializeBrush();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initialized = false;
        setFocusable(true);
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
        isDrawing = true;
        Log.d("DRAWING", String.valueOf(isDrawing));

        if (isInEditMode()) {
            canvas.drawColor(Color.WHITE);
            return;
        }

        height = canvas.getHeight();
        width = canvas.getWidth();

        float wPPI = stockWidth > 0 ? width / stockWidth : 1;
        float lPPI = stockLength > 0 ? height / stockLength : 1;

        scale = Math.min(wPPI, lPPI);

        cutoffBottom = scale * stockLength;
        cutoffRight = scale * stockWidth;

        //if in normal tablet drawing mode
        if (!initialized) {

            currX = cutoffRight / 2;
            currY = cutoffBottom / 2;

            drawing = Bitmap.createBitmap(Math.round(cutoffRight), Math.round(cutoffBottom), Bitmap.Config.ARGB_8888);
            redrawAll();

            if (!usingController) {
                depthMap = new DepthMap(stockWidth, stockLength, MIN_RADIUS, scale);
            }

            initialized = true;

        }

        brush.setColor(Color.BLACK);
        brush.setStyle(Paint.Style.FILL);
        brush.setStrokeWidth(scale * curTool.getDiameter());

        // Draw the cut off rectangles
        canvas.drawRect(cutoffRight, 0, width, height, brush);
        canvas.drawRect(0, cutoffBottom, width, height, brush);

        //draw the old strokes
        canvas.drawBitmap(drawing, 0, 0, brush);

        //draw the current stroke
        if (curStroke != null) {
            drawStroke(canvas, curStroke, brush, scale);
        }

        //draw cursor if using controller
        if (usingController) {
            brush.setStyle(Paint.Style.FILL);
            brush.setColor(Color.RED);
            canvas.drawCircle(currX, currY, 20, brush);
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


    /* Draws a single stroke, if needed draw a dot */
    private void drawStroke(Canvas canvas, Stroke s, Paint brush, float scale) {
        float toolDim = s.getTDiameter();
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
                if (!usingController) {
                    Anchor updatedPoint = depthMap.updateZ(new Anchor(x, y, z, time), curTool.getDiameter());
                    curStroke.addPoint(updatedPoint);
                    z = updatedPoint.z;
                } else {
                    z = curDepth;
                }
                break;

            case MANUAL_DEPTH:
                z = curDepth;
                break;
        }
        curStroke.addPoint(x, y, z, time);
    }

    /* Saves a fitted stroke to the bitmap and stroke list */
    private void saveStroke() {
        if (curStroke != null) {
            Stroke fitted = curStroke.fitToNatCubic(SMOOTHING_FACTOR, CURVE_STEPS);
            strokes.add(fitted);

            if (!usingController) {
                for (Anchor a : fitted) {
                    depthMap.addPoint(a);
                }
            }

            Canvas canvas = new Canvas(drawing);
            drawStroke(canvas, fitted, brush, scale);
        }
        curStroke = null;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!usingController) {
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
        }
        return true;

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



    //JOYSTICK INPUT HANDLING CODE
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        // Check that the event came from a game controller
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();
            for (int i = 0; i < historySize; i++) {
                processJoystickInput(event, i);
            }

            // Process the current movement sample in the batch (position -1)
            processJoystickInput(event, -1);

            postInvalidate();
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range =
                device.getMotionRange(axis, event.getSource());

        if (range != null) {
            final float flat = range.getFlat();
            final float value =
                    historyPos < 0 ? event.getAxisValue(axis) :
                            event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    private void processJoystickInput(MotionEvent event,  int historyPos) {
        InputDevice mInputDevice = event.getDevice();

        // Find X from the left control stick, hat axis, or the right control stick.
        float x = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X, historyPos);
        if (x == 0) {
            x = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_HAT_X, historyPos);
        }
        if (x == 0) {
            x = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_Z, historyPos);
        }

        // Find Y from the left control stick, hat switch, or the right control stick.
        float y = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y, historyPos);
        if (y == 0) {
            y = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_HAT_Y, historyPos);
        }
        if (y == 0) {
            y = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_RZ, historyPos);
        }

        // Update the current point based on the new x and y values and bound them
        currX += (x * DRAWSPEED);
        currY += (y * DRAWSPEED);
        currX = Math.max(0, Math.min(currX, cutoffRight));
        currY = Math.max(0, Math.min(currY, cutoffBottom));

        try {
            //get the trigger values
            rTrigValue = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
            lTrigValue = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);

            curDepth += rTrigValue / 20f; //divide by 10 to lessen sensitivity
            curDepth -= lTrigValue / 20f;
            curDepth = Math.min(1f, Math.max(0f, curDepth)); //bound at black and white

            //update the red line on the depth swatch
            depthSwatch.setDepth(curDepth);

            //Add that point to the stroke
            if (nowDrawing) {
                addPoint(currX,
                        currY,
                        event.getEventTime());
            }
        } catch (Exception e) {
            errorLog.append(e.toString());
        }

    }

    //Process buttons
        // A toggles drawing mode, B exits app entirely, X calls undo, Y calls clear
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_GAMEPAD)
                == InputDevice.SOURCE_GAMEPAD) {
            if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                if (nowDrawing) {
                    saveStroke();
                }
                nowDrawing = !nowDrawing;
            }
            if (keyCode == KeyEvent.KEYCODE_BUTTON_X) {
                undo();
            }
            if (keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
                clear();
            }
            if (keyCode == KeyEvent.KEYCODE_BUTTON_START) {
                appendLog(errorLog.toString());
                //save the gcode here instead?

            }
        }
        return super.onKeyDown(keyCode, event);
    }
    //End of controller code


    /* Clears the stroke list, depthmap, screen, and bitmap. Opens dialog to confirm first. */
    public void clear() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        curStroke = null;

                        if (strokes != null) { strokes.clear(); }
                        if (!usingController && depthMap != null) { depthMap.clear(); }
                        Canvas canvas = new Canvas(drawing);
                        canvas.drawColor(Color.WHITE);

                        postInvalidate();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.cancel();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Clear Drawing");
        builder.setMessage("Are you sure you want to clear? You cannot undo this action");
        builder.setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener).show();
    }


    /* Undoes the last stroke (redraws/remakes the bitmap) */
    public void undo() {
        int size = strokes.size();

        if (size > 0) {
            if (!usingController) {
                for (Anchor a : strokes.get(size - 1)) {
                    depthMap.removePoint(a);
                }
            }
            strokes.remove(size - 1);
            redrawAll();
            postInvalidate();
        }
    }


    //Sort the stroke list by tool.
    private LinkedList<Stroke> sortStrokes(LinkedList<Stroke> existingStrokes){
        HashMap<Tool, LinkedList<Stroke>> map = new HashMap<>();

        //for each stroke, check its tool, and place the stroke in the correct bin in the map
        //if the bin doesn't exist, make a new bin for that tool
        for (Stroke s : existingStrokes){
            if (map.containsKey(s.getTool())){
                map.get(s.getTool()).add(s);
            } else {
                map.put(s.getTool(), new LinkedList<Stroke>());
                map.get(s.getTool()).add(s);
            }
        }

        //output a final list that is the concatentation of each of the bins
        LinkedList<Stroke> finalAns = new LinkedList<>();
        Iterator<Tool> mapIterator = map.keySet().iterator();
        while (mapIterator.hasNext()) {
            Tool key = mapIterator.next();
            finalAns.addAll(map.get(key));
        }
        return finalAns;
    }



    protected void controllerAdded() {
        usingController = true;
        Toast.makeText(getContext(), "Controller detected. Touch screen disabled", Toast.LENGTH_SHORT).show();
        postInvalidate();
    }

    protected void controllerRemoved() {
        try {
            usingController = false;
            Toast.makeText(getContext(), "No controller detected. Touch screen enabled", Toast.LENGTH_SHORT).show();
            postInvalidate();
        } catch (Exception e) {
            errorLog.append(e);
        }
    }

    public void setDepthSwatch(DepthSwatch ds) {
        depthSwatch = ds;
    }

    public void setTool(Tool tool) {
        curTool = tool;
    }

    public Tool getTool() {
        return curTool;
    }

    public void setDepth(float depth) {
        curDepth = depth;
    }

    public boolean getDrawingState() {
        return isDrawing;
    }


    public void appendLog(String text) {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        try {
            dir.mkdirs();

            // compute the filename
            File prg = new File(dir, "Error.txt");
            prg.createNewFile();

            PrintWriter out = new PrintWriter(new FileOutputStream(prg, false));
            out.write(text);
            out.flush();
            out.close();

            MediaScannerConnection.scanFile(getContext(),
                    new String[]{prg.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("IO", "Scanned " + path + ":");
                            Log.i("IO", "-> uri=" + uri);
                        }
                    });

            Toast.makeText(getContext(), "Error Logged", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d("EXCEPTION", "Exception: " + e);
        }
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
                strokes = new LinkedList<>();
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
                    GCodeGenerator.singlePass(sortStrokes((LinkedList<Stroke>) strokes), //THIS NOW SORTS STROKES BY TOOL
                            stockWidth, stockLength, stockDepth, stockUnit, cutoffRight,
                            spoilDepth);
            out.write(gcg.toString());
            out.close();

            MediaScannerConnection.scanFile(getContext(),
                    new String[]{prg.toString()}, null,
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

    public void exportImage() {

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        try {
            dir.mkdirs();

            StringBuilder filename = new StringBuilder();
            CharSequence timestamp = DateFormat.format("yyyy-MM-ddThh:mm:ss", new Date());
            filename.append(timestamp);
            filename.append("_Image.png");

            File file = new File(dir, filename.toString());
            FileOutputStream fOut = new FileOutputStream(file);

            drawing.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaScannerConnection.scanFile(getContext(), new String[]{file.getAbsolutePath()}, null, null);

        } catch (IOException e) {
            Toast.makeText(getContext(), "Couldn't save image (" + e.getLocalizedMessage() + ")", Toast.LENGTH_SHORT).show();
            Log.d("IO", e.toString());
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
                Float.parseFloat(sharedPreferences.getString(DisplaySettingsActivity.KEY_TOOL, "0"));
                clearStrokes = true;
                clear();
                break;
            case DisplaySettingsActivity.KEY_TOOL:
                Float.parseFloat(sharedPreferences.getString(key, "0"));
                break;
            case DisplaySettingsActivity.KEY_SDEPTH:
                spoilDepth = Float.parseFloat(sharedPreferences.getString(key, "0"));
                break;
            default:
                Log.d("PREF", "unknown preference " + key);
        }

        postInvalidate();
    }


    public void initializeStockDimensions(SharedPreferences prefs) {
        stockLength = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_LENGTH, "-1"));
        stockWidth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_WIDTH, "-1"));
        stockDepth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_DEPTH, "-1"));
        spoilDepth = Float.parseFloat(prefs.getString(DisplaySettingsActivity.KEY_SDEPTH, "-1"));
    }


    //LIVE MODE CODE
    private static final String CNC_IP = "192.168.0.100";
    private static final int CNC_PORT = 5900;
    private static final String CNC_USER = "";
    private static final String CNC_PASSWORD = "c"; // really?!
    private boolean live = false;
    private VNCConnection vnc;

    public void startLive() {

        final DrawingView dv = this;

        //CREATE A DIALOG TO CHECK THEY WANT TO START LIVE
        //In the future the inputs will likely be put into this dialog box
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        Log.d("LIVE", "entering startLive()");

                        if (live || vnc != null) {
                            Log.d("LIVE", "aborting startLive---already live");
                            return;
                        }

                        // make connection
                        vnc = new VNCConnection(dv, CNC_IP, CNC_PORT, CNC_USER, CNC_PASSWORD);

                        Log.d("LIVE", "set to live");
                        live = true;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.cancel();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Start Live Mode");
        builder.setMessage("Before starting live mode, check you are on the manual gcode input page on your CNC machine.");


        builder.setPositiveButton("Continue", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener).show();


    }

    public void stopLive() {
        Log.d("LIVE", "entering stopLive()");

        live = false;

        if (vnc != null) {
            vnc.disconnect();
            vnc = null;
        }
    }
}