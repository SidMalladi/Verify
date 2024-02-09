package org.phenoapps.verify;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActionMenuView;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    public static String FILE_NAME = "org.phenoapps.verify.FILE_NAME";
    public static String INTRO_BUTTON = "org.phenoapps.verify.INTRO";

    public static String ABOUT_BUTTON = "org.phenoapps.verify.ABOUT";

    public static String SCAN_MODE_LIST = "org.phenoapps.verify.SCAN_MODE";
    public static String AUDIO_ENABLED = "org.phenoapps.verify.AUDIO_ENABLED";
    public static String TUTORIAL_MODE = "org.phenoapps.verify.TUTORIAL_MODE";
    public static String NAME = "org.phenoapps.verify.NAME";

    public static String LIST_KEY_NAME = "org.phenoapps.verify.LIST_KEY_NAME";
    public static String PAIR_NAME = "org.phenoapps.verify.PAIR_NAME";
    public static String DISABLE_PAIR = "org.phenoapps.verify.DISABLE_PAIR";
    public static String AUX_INFO = "org.phenoapps.verify.AUX_INFO";

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_settings);
//
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.settings_container, new Fragment()) // Correct FrameLayout id
//                    .commit();
//        }
//
//        // Initialize BottomNavigationView and handle navigation item selection
//        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation); // Ensure you have this id in your layout
//        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
//            @Override
//            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                switch (item.getItemId()) {
//                    case android.R.id.home:
//                        // Handle home action
//                        return true;
//                    case android.R.id.compare:
//                        // Handle compare action
//                        return true;
//                    case android.R.id.settings:
//                        // Already in Settings, do nothing or refresh
//                        return true;
//                }
//                return false;
//            }
//        });
//
//        // Select settings item by default
//        bottomNav.setSelectedItemId(R.id.Settings); // Use the actual id of your settings menu item here
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here.
//        if (item.getItemId() == android.R.id.home) {
//            finish();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if(getSupportActionBar() != null){
        getSupportActionBar().setTitle(null);
        getSupportActionBar().getThemedContext();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
}
    @Override
    final public boolean onCreateOptionsMenu(Menu m) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(org.phenoapps.verify.R.menu.activity_main_toolbar, m);

        ActionMenuView bottomToolBar = (ActionMenuView) findViewById(R.id.bottom_toolbar);
        Menu bottomMenu = bottomToolBar.getMenu();
        inflater.inflate(R.menu.activity_main_bottom_toolbar, bottomMenu);

        for (int i = 0; i < bottomMenu.size(); i++) {
            bottomMenu.getItem(i).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
