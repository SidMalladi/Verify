package org.phenoapps.verify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.util.prefs.Preferences;

public class SettingsFragment extends PreferenceFragment {


    private void showChangeLog() {

    }
    private void showAboutDialog(Context ctx)
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent i = new Intent(getContext(), AboutActivity.class);
            startActivity(i);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(org.phenoapps.verify.R.xml.preferences);

        final SharedPreferences sharedPrefs = super.getPreferenceManager().getSharedPreferences();
        ListPreference mode = (ListPreference) findPreference(SettingsActivity.SCAN_MODE_LIST);
        Preference introButton = (Preference) findPreference(SettingsActivity.INTRO_BUTTON);
        Preference aboutButton = (Preference) findPreference(SettingsActivity.ABOUT_BUTTON);

        aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    showAboutDialog(getContext());
                }
                return true;
            }
        });

        introButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    final Intent intro_intent = new Intent(getContext(), IntroActivity.class);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(intro_intent);
                        }
                    });
                }
                return true;
            }
        });

        mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            //check if Pair mode is chosen, if it's disabled then show a message and switch
            //back to default mode.
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (o.equals("4") &&
                        sharedPrefs.getBoolean(SettingsActivity.DISABLE_PAIR, false)) {
                    ((ListPreference) preference).setValue("0");
                    Toast.makeText(getActivity(),
                            "Pair mode cannot be used without setting a pair ID.",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }
}
