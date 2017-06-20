package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import static java.lang.Float.parseFloat;

/**
 * Preference screen.
 *
 * Created by edinameshietedoho on 5/31/17.
 */

public class DisplaySettingsActivity extends AppCompatActivity  {

    public static final String KEY_DEPTH = "pref_wDepth";
    public static final String KEY_LENGTH = "pref_wLength";
    public static final String KEY_WIDTH = "pref_wWidth";
    public static final String KEY_UNIT = "pref_units";
    public static final String KEY_SDEPTH = "pref_spoilB";
    public static final String KEY_SWIDTH = "pref_sWidth";

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


            for (String key : new String[] { KEY_LENGTH, KEY_WIDTH , KEY_DEPTH, KEY_SWIDTH }) {
                findPreference(key).setSummary(Integer.toString(prefs.getInt(key, 0)) + mCurUnit );
            }
            for (String key: new String[] {KEY_SDEPTH}) {
                try {
                    findPreference(key);
                    float depth = parseFloat(key);
                    Log.d("DEPTH","" + depth + "");
                }
                catch (NumberFormatException e) {
                    Log.d("TYPE ERROR","Incorrect Input: (" + e.getLocalizedMessage() + ")" );
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            switch (key) {
                case KEY_SWIDTH:
                case KEY_LENGTH:
                case KEY_WIDTH:
                case KEY_DEPTH:
                    // Keeps track of length, width, depth, and spoil board depth inputs
                    pref.setSummary(Integer.toString(sharedPreferences.getInt(key, 0)) + mCurUnit);
                    break;
                case KEY_SDEPTH:
                    String newDepth = sharedPreferences.getString(KEY_SDEPTH, defaultUnit);
                    Float f1 = Float.parseFloat(newDepth);
                    Log.d("FLOAT",f1.toString());
                    pref.setSummary(Float.toString(Float.parseFloat(sharedPreferences.getString(key, String.valueOf(0)))) + mCurUnit);
                    break;
                case KEY_UNIT:
                    String newUnit = sharedPreferences.getString(KEY_UNIT, defaultUnit);
                    if (newUnit.equals(mCurUnit)) {
                        return;
                    }
                    convertDimensionsTo(newUnit);
                    pref.setSummary(newUnit);
                    break;
                default:
                    Log.d("PREF", String.format("unknown key %s", key));
                    break;
            }
        }

        private void convertDimensionsTo(String newUnit) {
            // either: in -> cm or cm -> in

            double factor;
            int descIndex;
            switch (newUnit) {
                case "in":
                    factor = 1 / 2.54;
                    descIndex = 0;
                    break;
                case "cm":
                    factor = 2.54;
                    descIndex = 1;
                    break;
                default:
                    Log.d("PREF", String.format("unfamiliar unit %s (currently in %s, staying there)", newUnit, mCurUnit));
                    return;
            }

            mCurUnit = newUnit;

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            int w = prefs.getInt(KEY_WIDTH, 0);
            int l = prefs.getInt(KEY_LENGTH, 0);
            int d = prefs.getInt(KEY_DEPTH, 0);
            float s = prefs.getFloat(KEY_SDEPTH,0);
            int b = prefs.getInt(KEY_SWIDTH,0);
            prefs.edit()
                    .putInt(KEY_WIDTH, (int) Math.round(w * factor))
                    .putInt(KEY_LENGTH, (int) Math.round(l * factor))
                    .putInt(KEY_DEPTH, (int) Math.round(d * factor))
                    .putFloat(KEY_SDEPTH, (float) Math.round(s * factor))
                    .putInt(KEY_SWIDTH, (int) Math.round(b * factor))
                    .apply();

            Toast.makeText(
                    getContext(),
                    "Converted to " + getResources().getStringArray(R.array.units)[descIndex] + ", adjusted stock dimensions",
                    Toast.LENGTH_SHORT)
                    .show();
        }


        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);

            if (view != null) {
                view.setBackgroundColor(getResources().getColor(android.R.color.background_light, null));
            }

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
