package com.labgmail.pomona.greenberg.cnccarvingfactory;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.EditTextPreference;
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
    public static final String KEY_TOOL = "pref_tool";

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

            for (String key : new String[] { KEY_LENGTH, KEY_WIDTH, KEY_DEPTH, KEY_SDEPTH }) {
                findPreference(key).setSummary((prefs.getString(key, "0") + mCurUnit ));
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            switch (key) {
                    // Keeps track of length, width, depth, and spoil board depth inputs
                case KEY_DEPTH:
                case KEY_WIDTH:
                case KEY_LENGTH:
                case KEY_SDEPTH:
                    pref.setSummary(sharedPreferences.getString(key, "0") + mCurUnit);
                    break;
                case KEY_UNIT:
                    String newUnit = sharedPreferences.getString(KEY_UNIT, defaultUnit);
                    if (newUnit.equals(mCurUnit)) {
                        return;
                    }
                    convertDimensionsTo(newUnit);
                    pref.setSummary(newUnit);
                    break;
                case KEY_TOOL:
                    break;
                default:
                    Log.d("PREF", String.format("unknown key %s", key));
                    break;
            }
        }

        private void convertDimensionsTo(String newUnit) {
            // either: in -> cm or cm -> in

            float factor;
            int descIndex;
            switch (newUnit) {
                case "in":
                    factor = 1f / 2.54f;
                    descIndex = 0;
                    break;
                case "cm":
                    factor = 2.54f;
                    descIndex = 1;
                    break;
                default:
                    Log.d("PREF", String.format("unfamiliar unit %s (currently in %s, staying there)", newUnit, mCurUnit));
                    return;
            }

            mCurUnit = newUnit;

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            float w = Float.parseFloat(prefs.getString(KEY_WIDTH, "0"));
            float l = Float.parseFloat(prefs.getString(KEY_LENGTH, "0"));
            float d = Float.parseFloat(prefs.getString(KEY_DEPTH, "0"));
            float s = Float.parseFloat(prefs.getString(KEY_SDEPTH, "0"));
            Log.d("PREF", String.format("updating sdepth to %f", s * factor));
            prefs.edit()
                    .apply();

            ((EditTextPreference) findPreference(KEY_LENGTH)).setText(Float.toString(l * factor));
            ((EditTextPreference) findPreference(KEY_DEPTH)).setText(Float.toString(d * factor));
            ((EditTextPreference) findPreference(KEY_WIDTH)).setText(Float.toString(w * factor));
            ((EditTextPreference) findPreference(KEY_SDEPTH)).setText(Float.toString(s * factor));

            Toast.makeText(
                    getContext(),
                    "Converted to " + getResources().getStringArray(R.array.units)[descIndex] + ", adjusted stock dimensions and cleared drawing",
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
