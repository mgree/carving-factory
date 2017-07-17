package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;


/**
 * Central drawing activity for (eventual) output to a CNC machine.
 */
public class DrawingActivity extends AppCompatActivity implements InputManager.InputDeviceListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private DrawingView mContentView;
    private boolean pendingSave = false;
    public List<Tool> tools = new LinkedList<>();
    private InputManager mInputManager;



    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (!mContentView.getDrawingState()) {
                actionBar.show();
            }
            else if (mContentView.getDrawingState()) {
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_HIDE_NAVIGATION );
            }
            mControlsView.setVisibility(View.VISIBLE);

        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            else if (mContentView.getDrawingState()) {
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_HIDE_NAVIGATION );
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mInputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_drawing);

        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = (DrawingView) findViewById(R.id.fullscreen_content);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(mContentView);
        mContentView.initializeStockDimensions(prefs);

        initializeTools();

        // tools proper
        LinearLayout ll = (LinearLayout) findViewById(R.id.fullscreen_controls);

        //Add a button for each tool
        for (int i = 0; i < tools.size(); i++) {
            Button toolButton = new Button(this);
            toolButton.setId(tools.get(i).getToolNum());
            toolButton.setText(tools.get(i).getToolName());
            toolButton.setMinHeight(ll.getHeight());
            final int index = i;
            toolButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tool theTool = tools.get(index);
                    mContentView.setTool(theTool);
                    Toast.makeText(v.getContext(),"Selected Tool: " + theTool.getToolName() + " (" +
                            theTool.getDiameter() + ") ", Toast.LENGTH_SHORT).show();
                }
            });
            ll.addView(toolButton,160,200);
        }

        // TOOLBAR SETUP
        DepthSwatch swatch = (DepthSwatch) findViewById(R.id.depth_swatch);
        swatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContentView.setDepth(((DepthSwatch) v).getDepthSelected());
                Toast.makeText(v.getContext(),"Cutting Depth: " + ((DepthSwatch) v).getDepthSelected()*mContentView.getTool().getMaxCutDepth(), Toast.LENGTH_SHORT).show();
            }
        });
        swatch.setDepth(1.0f);

        mContentView.setDepthSwatch(swatch);

        findViewById(R.id.undo_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mContentView.undo();
                hide(); }
        });
        findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mContentView.clear();
            }
        });

        findViewById(R.id.live_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { mContentView.startLive(); }
        });

        // FULLSCREEN SETUP
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        // SETUP SAVE BUTTON
        final Activity self = this;
        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(self, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    mContentView.exportGCode();
                    mContentView.exportImage();
                } else {
                    pendingSave = true;
                    ActivityCompat.requestPermissions(self, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
            }
        });

        // SETTINGS BUTTON
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(self, DisplaySettingsActivity.class));
            }
        });
        
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 0) {
            Log.d("IO","huh?");
            return;
        }

        boolean success = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (success && pendingSave) {
            pendingSave = false;
            findViewById(R.id.save_button).performClick();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hide();
        mInputManager.registerInputDeviceListener(this, null);

        try {
            FileOutputStream state = openFileOutput("carving_state", MODE_PRIVATE);
            mContentView.saveState(state);
            state.close();
        } catch (FileNotFoundException e) {
            Log.e("PAUSE", "FNFE saving state");
        } catch (IOException e) {
            Log.e("PAUSE", "ioe saving state");
        }

        getPreferences(Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(mContentView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hide();
        mInputManager.registerInputDeviceListener(this, null);


        try {
            FileInputStream state = openFileInput("carving_state");
            mContentView.loadState(state);
            state.close();
        } catch (FileNotFoundException e) {
            Log.e("PAUSE", "FNFE loading state");
        } catch (IOException e) {
            Log.e("PAUSE", "ioe loading state");
        }

        getPreferences(Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(mContentView);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    // TODO: not possible to show system bars without drawing

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        else {
            mContentView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        mControlsView.setVisibility(View.GONE);

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void initializeTools() { //TODO ADDRPMS
        Tool half_inch = new Tool(1, 0.5f, 0.4f, 80f, 250f, "Half Inch");
        Tool quarter_inch = new Tool(2, 0.25f, 0.3f, 80f, 250f, "Quarter Inch");

        tools.add(half_inch);
        tools.add(quarter_inch);
        mContentView.setTool(tools.get(0));
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        if (isController(deviceId)) {
            mContentView.controllerAdded();
        }
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        mContentView.controllerRemoved();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {  }

    private boolean isController(int deviceId) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                return true;
            }
        return false;
    }
}