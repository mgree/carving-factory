package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
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

    public static final String KEY_DEPTH = "pref_wDepth";
    public static final String KEY_LENGTH = "pref_wLength";
    public static final String KEY_WIDTH = "pref_wWidth";
    public static final String KEY_UNIT = "pref_units";

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private String defaultUnit;
        private String mCurUnit;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

            defaultUnit = getResources().getStringArray(R.array.unit_values)[0];
            mCurUnit = prefs.getString(KEY_UNIT, defaultUnit);
            findPreference(KEY_UNIT).setSummary(mCurUnit);

            for (String key : new String[] { KEY_LENGTH, KEY_WIDTH /*, KEY_DEPTH */}) {
                findPreference(key).setSummary(Integer.toString(prefs.getInt(key, 0)) + mCurUnit );
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            if (key.equals(KEY_LENGTH) || key.equals(KEY_WIDTH) || key.equals(KEY_DEPTH)) {
                // Keeps track of both length and width inputs (and depth eventually)
                pref.setSummary(Integer.toString(sharedPreferences.getInt(key, 0)) + mCurUnit);
            } else if (key.equals(KEY_UNIT)) {
                String newUnit = sharedPreferences.getString(KEY_UNIT, defaultUnit);

                if (newUnit.equals(mCurUnit)) {
                    return;
                }

                convertDimensionsTo(newUnit);
                pref.setSummary(newUnit);
            } else {
                Log.d("PREF",String.format("unknown key %s",key));
            }
        }

        private void convertDimensionsTo(String newUnit) {
            // either: in -> cm or cm -> in

            double factor;
            if (newUnit.equals("in")) {
                factor = 1/2.54;
            } else if (newUnit.equals("cm")) {
                factor = 2.54;
            } else {
                Log.d("PREF",String.format("unfamiliar unit %s (currently in %s, staying there)", newUnit, mCurUnit));
                return;
            }

            mCurUnit = newUnit;

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            int w = prefs.getInt(KEY_WIDTH, 0);
            int l = prefs.getInt(KEY_LENGTH, 0);
            //int d = prefs.getInt(KEY_DEPTH, 0);
            prefs.edit()
                    .putInt(KEY_WIDTH, (int) Math.round(w * factor))
                    .putInt(KEY_LENGTH, (int) Math.round(l * factor))
//                    .putInt(KEY_DEPTH, (int) Math.round(d * factor))
                    .commit();
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

}
