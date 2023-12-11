package org.phenoapps.verify;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(org.phenoapps.verify.R.xml.preferences);

        final SharedPreferences sharedPrefs = super.getPreferenceManager().getSharedPreferences();
        ListPreference mode = (ListPreference) findPreference(SettingsActivity.SCAN_MODE_LIST);

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
