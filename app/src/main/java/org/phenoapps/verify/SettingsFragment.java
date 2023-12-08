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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(ctx);
        {
            android.view.View personView = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                personView = getLayoutInflater().inflate(
                        R.layout.about, new android.widget.LinearLayout(ctx),
                        false);
            }
            {
                assert personView != null;
                android.widget.TextView versionTextView = (android.widget.TextView)
                        personView.findViewById(org.phenoapps.verify.R.id.tvVersion);
                try
                {
                    android.content.pm.PackageInfo packageInfo =
                            ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                    assert packageInfo     != null;
                    assert versionTextView != null;
                    versionTextView.setText(this.getResources().getString(
                            org.phenoapps.verify.R.string.versiontitle) +
                            ' ' + packageInfo.versionName);
                }
                catch (android.content.pm.PackageManager.NameNotFoundException e)
                { e.printStackTrace(); }
                versionTextView.setOnClickListener(new android.view.View.OnClickListener()
                {
                    @java.lang.Override
                    public void onClick(android.view.View v)
                    { showChangeLog(); }
                });
            }

            builder.setCancelable(true);
            builder.setTitle     (this.getResources().getString(
                    org.phenoapps.verify.R.string.about));
            builder.setView(personView);
        }

        builder.setNegativeButton(
                this.getResources().getString(org.phenoapps.verify.R.string.ok),
                new android.content.DialogInterface.OnClickListener()
                {
                    @java.lang.Override
                    public void onClick(android.content.DialogInterface dialog, int which)
                    {
                        assert dialog != null;
                        dialog.dismiss();
                    }
                });

        builder.show();
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
