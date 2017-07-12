package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

// Method that passes over Tool list from DrawingActivity to DrawingView
//What if each time they press the button a list is sent over, updated, and theres a curTool that keeps track.
/**
 * Central drawing activity for (eventual) output to a CNC machine.
 */
public class DrawingActivity extends AppCompatActivity {
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
    private static final float MAX_CUT_DEPTH = .4f;
    private final Handler mHideHandler = new Handler();
    private DrawingView mContentView;
    private ImageView swatch;
    private boolean pendingSave = false;
    public List<Tool> tools = new LinkedList<>();


    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
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
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        initializeTools();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.activity_drawing);



        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = (DrawingView) findViewById(R.id.fullscreen_content);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("PREF", prefs.getAll().toString());
        prefs.registerOnSharedPreferenceChangeListener(mContentView);
        mContentView.initializeStockDimensions(prefs);
        mContentView.setTool(tools.get(0));

        LinearLayout ll = (LinearLayout) findViewById(R.id.fullscreen_controls);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        for (int i=0; i<tools.size(); i++) {
            Button toolButton = new Button(this);
            toolButton.setId(i);
            toolButton.setText(tools.get(i).getToolName());
//            toolButton.setMinimumHeight();
            toolButton.setHeight(ActionBar.LayoutParams.MATCH_PARENT);
            toolButton.setMinimumWidth(ActionBar.LayoutParams.MATCH_PARENT);

            ll.addView(toolButton, lp);
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

        findViewById(R.id.undo_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mContentView.undo(); }
        });
        findViewById(R.id.clear_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mContentView.clear(); }
        });


        // tools proper
        // TODO programmatically generate tools from our library
        findViewById(R.id.half_inch_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mContentView.setTool(tools.get(0)); // TODO don't use a constant
                Toast.makeText(v.getContext(),"Selected Tool: Half Inch (.5)", Toast.LENGTH_SHORT).show();}
        });
        findViewById(R.id.quarter_inch_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mContentView.setTool(tools.get(1));
                Toast.makeText(v.getContext(),"Selected Tool: Quarter Inch (.25)", Toast.LENGTH_SHORT).show();}
        });



        // FULLSCREEN SETUP
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        // SETUP SAVE BUTTON
        final Activity self = this;
        findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("IO", "clicked");
                if (ContextCompat.checkSelfPermission(self, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    mContentView.exportGCode();
                    mContentView.exportImage();
                } else {
                    pendingSave = true;
                    ActivityCompat.requestPermissions(self,
                            new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            0);
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


        //DRAWING MODE BUTTON
//        ToggleButton drawingMode = (ToggleButton) findViewById(R.id.drawingModeButton);
//        drawingMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked) {
//                    // The toggle is enabled
//                } else {
//                    // The toggle is disabled
//                }
//            }
//        });
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

    public void initializeTools() {
        Tool half_inch = new Tool(1, 0.5f, 0.4f, 80f, 250f, 0f, "half inch");
        Tool quarter_inch = new Tool(2, 0.25f, 0.3f, 80f, 250f, 0f, "quarter inch");

        tools.add(half_inch);
        tools.add(quarter_inch);

    }
}
