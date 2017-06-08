package com.labgmail.pomona.greenberg.cnccarvingfactory;

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

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            for (String key : new String[] { KEY_LENGTH, KEY_WIDTH /*, KEY_DEPTH */}) {
                findPreference(key).setSummary(Integer.toString(prefs.getInt(key, 0)) + "in");
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            if (pref == null) {
                return;
            }

            if (key.equals(KEY_LENGTH) || key.equals(KEY_WIDTH) || key.equals(KEY_DEPTH)) {
                int val = sharedPreferences.getInt(key, 0);
                pref.setSummary(Integer.toString(val) + "in");
            } else {
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

}
