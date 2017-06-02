package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by edinameshietedoho on 5/31/17.
 */

public class DisplaySettingsActivity extends AppCompatActivity  {

    public static final String KEY_LENGTH = "pref_length";
    public static final String KEY_WIDTH = "pref_width";
    public static final String KEY_DEPTH = "pref_depth";

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            for (String key : new String[] { KEY_LENGTH, KEY_WIDTH, KEY_DEPTH }) {
                findPreference(key).setSummary(prefs.getString(key, null));
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);
            if (pref != null) {
                String val = sharedPreferences.getString(key,null);
                Log.d("PREF",String.format("updated %s to %s",key,val));
                pref.setSummary(val);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);

            view.setBackgroundColor(getResources().getColor(android.R.color.background_light, null));

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }


        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs,String key){
                // Wants this line to live outside of the brackets. Why?
                prefs.registerOnSharedPreferenceChangeListener(this);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    // So what has to happen is a preference needs to be made (a custom preference that extends Preference with a NumberPicker)
    // That needs to be run in a PreferenceFragment
    // Set the default value
    // Go over Preferences and Settings API figure out how to get vaue entered to make a new rectangle with those dimensions...
    // Hmm will probably need a conversion factor for square feet to dp(whatever unit of measure that is)
    // Ok we have a full day ahead of us tomorrow. Get ready buddy ole pall.
    // Ok update, the settings button changes the view and they each individually save their values but they do nothing with them.
    // Also i need to figure out how to make it so that the only values that can be entered are integer values.
    // Also how to keep the pop up window from poping up whenever someone opens the app (the alpha picker).
}
//