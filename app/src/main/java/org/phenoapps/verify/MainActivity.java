package org.phenoapps.verify;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    final static private String line_separator = System.getProperty("line.separator");

    private IdEntryDbHelper mDbHelper;

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    //database prepared statements
    private SQLiteStatement sqlUpdateNote;
    private SQLiteStatement sqlDeleteId;
    private SQLiteStatement sqlUpdateChecked;
    private SQLiteStatement sqlUpdateUserAndDate;

    private SparseArray<String> mIds;

    //global variable to track matching order
    private int mMatchingOrder;

    private String mListId;

    //pair mode vars
    private String mPairCol;
    private String mNextPairVal;

    private String mFileName = "";

    private Toolbar navigationToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(org.phenoapps.verify.R.layout.activity_main);

        mIds = new SparseArray<>();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

final View auxInfo = findViewById(R.id.auxScrollView);
final View auxValue = findViewById(R.id.auxValueView);

        if (sharedPref.getBoolean(SettingsActivity.AUX_INFO, false)) {
auxInfo.setVisibility(View.VISIBLE);
auxValue.setVisibility(View.VISIBLE);

        } else {
auxInfo.setVisibility(View.GONE);
auxValue.setVisibility(View.GONE);
}

        mPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

if (sharedPreferences.getBoolean(SettingsActivity.AUX_INFO, false)) {
auxInfo.setVisibility(View.VISIBLE);
auxValue.setVisibility(View.VISIBLE);
} else {
auxInfo.setVisibility(View.GONE);
auxValue.setVisibility(View.GONE);
}
            }
        };

        sharedPref.registerOnSharedPreferenceChangeListener(mPrefListener);

        if (!sharedPref.getBoolean("onlyLoadTutorialOnce", false)) {
            launchIntro();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("onlyLoadTutorialOnce", true);
            editor.apply();
        } else {
            boolean tutorialMode = sharedPref.getBoolean(SettingsActivity.TUTORIAL_MODE, false);

            if (tutorialMode)
                launchIntro();
        }

        mFileName = sharedPref.getString(SettingsActivity.FILE_NAME, "");

        ActivityCompat.requestPermissions(this, VerifyConstants.permissions, VerifyConstants.PERM_REQ);

        mNextPairVal = null;
        mMatchingOrder = 0;
        mPairCol = null;

        initializeUIVariables();

        mDbHelper = new IdEntryDbHelper(this);

        loadSQLToLocal();

        if (mListId != null)
            updateCheckedItems();
    }

    private void copyRawToVerify(File verifyDirectory, String fileName, int rawId) {

        String fieldSampleName = verifyDirectory.getAbsolutePath() + "/" + fileName;
        File fieldSampleFile = new File(fieldSampleName);
        if (!Arrays.asList(verifyDirectory.listFiles()).contains(fieldSampleFile)) {
            try {
                InputStream inputStream = getResources().openRawResource(rawId);
                FileOutputStream foStream =  new FileOutputStream(fieldSampleName);
                byte[] buff = new byte[1024];
                int read = 0;
                try {
                    while ((read = inputStream.read(buff)) > 0) {
                        foStream.write(buff, 0, read);
                    }
                    scanFile(this, fieldSampleFile);
                } finally {
                    inputStream.close();
                    foStream.close();
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }

    public static void scanFile(Context ctx, File filePath) {
        MediaScannerConnection.scanFile(ctx, new String[] { filePath.getAbsolutePath()}, null, null);
    }

    private void prepareStatements() {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            String updateNoteQuery = "UPDATE VERIFY SET note = ? WHERE " + mListId + " = ?";
            sqlUpdateNote = db.compileStatement(updateNoteQuery);

            String deleteIdQuery = "DELETE FROM VERIFY WHERE " + mListId + " = ?";
            sqlDeleteId = db.compileStatement(deleteIdQuery);

            String updateCheckedQuery = "UPDATE VERIFY SET color = 1 WHERE " + mListId + " = ?";
            sqlUpdateChecked = db.compileStatement(updateCheckedQuery);

            String updateUserAndDateQuery =
                    "UPDATE VERIFY SET user = ?, date = ?, scan_count = scan_count + 1 WHERE " + mListId + " = ?";
            sqlUpdateUserAndDate = db.compileStatement(updateUserAndDateQuery);
        } catch(SQLiteException e) {
            e.printStackTrace();
        }
    }

    private void initializeUIVariables() {

        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle("CheckList");
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        final EditText scannerTextView = ((EditText) findViewById(R.id.scannerTextView));
        scannerTextView.setSelectAllOnFocus(true);
        scannerTextView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        checkScannedItem();
                    }
                }
                return false;
            }
        });

        ListView idTable = ((ListView) findViewById(R.id.idTable));
        idTable.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
idTable.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
scannerTextView.setText(((TextView) view).getText().toString());
                scannerTextView.setSelection(scannerTextView.getText().length());
                scannerTextView.requestFocus();
                scannerTextView.selectAll();
                checkScannedItem();
                }
        });

idTable.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
@Override
public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//get app settings
insertNoteIntoDb(((TextView) view).getText().toString());
return true;
}
        });

        TextView valueView = (TextView) findViewById(R.id.valueView);
        valueView.setMovementMethod(new ScrollingMovementMethod());

        findViewById(org.phenoapps.verify.R.id.clearButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scannerTextView.setText("");
            }
        });
    }

    private synchronized void checkScannedItem() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsActivity.SCAN_MODE_LIST, "-1"));
boolean displayAux = sharedPref.getBoolean(SettingsActivity.AUX_INFO, true);

        String scannedId = ((EditText) findViewById(org.phenoapps.verify.R.id.scannerTextView))
                .getText().toString();

        if (mIds != null && mIds.size() > 0) {
            //update database
            exertModeFunction(scannedId);

            //view updated database
            SQLiteDatabase db = mDbHelper.getReadableDatabase();

            String table = IdEntryContract.IdEntry.TABLE_NAME;
            String[] selectionArgs = new String[]{scannedId};
            Cursor cursor = db.query(table, null, mListId + "=?", selectionArgs, null, null, null);

            String[] headerTokens = cursor.getColumnNames();
            StringBuilder values = new StringBuilder();
            StringBuilder auxValues = new StringBuilder();
            if (cursor.moveToFirst()) {
                for (String header : headerTokens) {

                    if (!header.equals(mListId)) {

                        final String val = cursor.getString(
                                cursor.getColumnIndexOrThrow(header)
                        );

                        if (header.equals("color") || header.equals("scan_count") || header.equals("date")
                                || header.equals("user") || header.equals("note")) {
                            if (header.equals("color")) continue;
                            else if (header.equals("scan_count")) auxValues.append("Number of scans");
                            else if (header.equals("date")) auxValues.append("Date");
                            else auxValues.append(header);
                            auxValues.append(" : ");
                            if (val != null) auxValues.append(val);
                            auxValues.append(line_separator);
                        } else {
                            values.append(header);
                            values.append(" : ");
                            if (val != null) values.append(val);
                            values.append(line_separator);
                        }
                    }
                }
                                cursor.close();
((TextView) findViewById(org.phenoapps.verify.R.id.valueView)).setText(values.toString());
((TextView) findViewById(R.id.auxValueView)).setText(auxValues.toString());
((EditText) findViewById(R.id.scannerTextView)).setText("");
            } else {
                if (scanMode != 2) {
                    ringNotification(false);
                }
            }
        }
    }

    private Boolean checkIdExists(String id) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        final String table = IdEntryContract.IdEntry.TABLE_NAME;
        final String[] selectionArgs = new String[] { id };
        final Cursor cursor = db.query(table, null, mListId + "=?", selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    private synchronized void insertNoteIntoDb(@NonNull final String id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter a note for the given item.");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String value = input.getText().toString();
                if (!value.isEmpty()) {

                    final SQLiteDatabase db = mDbHelper.getWritableDatabase();

                    if (sqlUpdateNote != null) {
                        sqlUpdateNote.bindAllArgsAsStrings(new String[]{
                                value, id
                        });
                        sqlUpdateNote.executeUpdateDelete();
                    }
                }
            }
        });

        builder.show();
    }

    private synchronized void exertModeFunction(@NonNull String id) {

        //get app settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int scanMode = Integer.valueOf(sharedPref.getString(SettingsActivity.SCAN_MODE_LIST, "-1"));

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        if (scanMode == 0 ) { //default mode
            mMatchingOrder = 0;
            ringNotification(checkIdExists(id));

        } else if (scanMode == 1) { //order mode
            final int tableIndex = getTableIndexById(id);

            if (tableIndex != -1) {
                if (mMatchingOrder == tableIndex) {
                    mMatchingOrder++;
                    Toast.makeText(this, "Order matches id: " + id + " at index: " + tableIndex, Toast.LENGTH_SHORT).show();
                    ringNotification(true);
                } else {
                    Toast.makeText(this, "Scanning out of order!", Toast.LENGTH_SHORT).show();
                    ringNotification(false);
                }
            }
        } else if (scanMode == 2) { //filter mode, delete rows with given id

            mMatchingOrder = 0;
            if (sqlDeleteId != null) {
                sqlDeleteId.bindAllArgsAsStrings(new String[]{id});
                sqlDeleteId.executeUpdateDelete();
            }
            updateFilteredArrayAdapter(id);

        } else if (scanMode == 3) { //if color mode, update the db to highlight the item

            mMatchingOrder = 0;
            if (sqlUpdateChecked != null) {
                sqlUpdateChecked.bindAllArgsAsStrings(new String[]{id});
                sqlUpdateChecked.executeUpdateDelete();
            }
        } else if (scanMode == 4) { //pair mode

            mMatchingOrder = 0;

            if (mPairCol != null) {

                //if next pair id is waiting, check if it matches scanned id and reset mode
                if (mNextPairVal != null) {
                    if (mNextPairVal.equals(id)) {
                        ringNotification(true);
                        Toast.makeText(this, "Scanned paired item: " + id, Toast.LENGTH_SHORT).show();
                    }
                    mNextPairVal = null;
                } else { //otherwise query for the current id's pair
                    String table = IdEntryContract.IdEntry.TABLE_NAME;
                    String[] columnsNames = new String[] { mPairCol };
                    String selection = mListId + "=?";
                    String[] selectionArgs = { id };
                    Cursor cursor = db.query(table, columnsNames, selection, selectionArgs, null, null, null);
                    if (cursor.moveToFirst()) {
                        mNextPairVal = cursor.getString(
                                cursor.getColumnIndexOrThrow(mPairCol)
                        );
                    } else mNextPairVal = null;
                    cursor.close();
                }
            }
        }
        //always update user and datetime
        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());

        if (sqlUpdateUserAndDate != null) { //no db yet
            String name = sharedPref.getString(SettingsActivity.NAME, "");
            sqlUpdateUserAndDate.bindAllArgsAsStrings(new String[]{
                    name,
                    sdf.format(c.getTime()),
                    id
            });
            sqlUpdateUserAndDate.executeUpdateDelete();
        }

        updateCheckedItems();
    }

    private synchronized void updateCheckedItems() {

        final SQLiteDatabase db = mDbHelper.getReadableDatabase();

        //list of ideas to populate and update the view with
        final HashSet<String> ids = new HashSet<>();

        final String table = IdEntryContract.IdEntry.TABLE_NAME;
        final String[] columns = new String[] { mListId };
        final String selection = "color = 1";

        try {
            final Cursor cursor = db.query(table, columns, selection, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(
                            cursor.getColumnIndexOrThrow(mListId)
                    );

                    ids.add(id);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        ListView idTable = (ListView) findViewById(org.phenoapps.verify.R.id.idTable);
        for (int position = 0; position < idTable.getCount(); position++) {

            final String id = (idTable.getItemAtPosition(position)).toString();

            if (ids.contains(id)) {
                idTable.setItemChecked(position, true);
            } else idTable.setItemChecked(position, false);
        }
    }

    private synchronized void loadSQLToLocal() {

        mIds = new SparseArray<>();

        mDbHelper = new IdEntryDbHelper(this);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mListId = sharedPref.getString(SettingsActivity.LIST_KEY_NAME, null);
        mPairCol = sharedPref.getString(SettingsActivity.PAIR_NAME, null);

        if (mListId != null) {
            prepareStatements();
            loadBarcodes();
            buildListView();
        }
    }

    private void loadBarcodes() {

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        try {
            final String table = IdEntryContract.IdEntry.TABLE_NAME;
            final Cursor cursor = db.query(table, null, null, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    final String[] headers = cursor.getColumnNames();
                    for (String header : headers) {

                        final String val = cursor.getString(
                                cursor.getColumnIndexOrThrow(header)
                        );

                        if (header.equals(mListId)) {
                            mIds.append(mIds.size(), val);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();

        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private synchronized void askUserExportFileName() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose name for exported file.");
        final EditText input = new EditText(this);

        final Calendar c = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int lastDot = mFileName.lastIndexOf('.');
        if (lastDot != -1) {
            mFileName = mFileName.substring(0, lastDot);
        }
        input.setText("Verify_"+ sdf.format(c.getTime()));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

       builder.setPositiveButton("Export", new DialogInterface.OnClickListener() {
           @Override
           public void onClick(DialogInterface dialogInterface, int which) {
               String value = input.getText().toString();
               mFileName = value;
               final Intent i;
               if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                   i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                   i.setType("*/*");
                   i.putExtra(Intent.EXTRA_TITLE, value+".csv");
                   startActivityForResult(Intent.createChooser(i, "Choose folder to export file."), VerifyConstants.PICK_CUSTOM_DEST);
               }else{
                   writeToExportPath();
               }
           }
       });
       builder.show();
    }

    public void writeToExportPath(){
        String value = mFileName;

        if (!value.isEmpty()) {
            if (isExternalStorageWritable()) {
                try {
                    File verifyDirectory = new File(Environment.getExternalStorageDirectory().getPath() + "/Verify");
                    final File output = new File(verifyDirectory, value + ".csv");
                    final FileOutputStream fstream = new FileOutputStream(output);
                    final SQLiteDatabase db = mDbHelper.getReadableDatabase();
                    final String table = IdEntryContract.IdEntry.TABLE_NAME;
                    final Cursor cursor = db.query(table, null, null, null, null, null, null);
                    //final Cursor cursor = db.rawQuery("SElECT * FROM VERIFY", null);

                    //first write header line
                    final String[] headers = cursor.getColumnNames();
                    for (int i = 0; i < headers.length; i++) {
                        if (i != 0) fstream.write(",".getBytes());
                        fstream.write(headers[i].getBytes());
                    }
                    fstream.write(line_separator.getBytes());
                    //populate text file with current database values
                    if (cursor.moveToFirst()) {
                        do {
                            for (int i = 0; i < headers.length; i++) {
                                if (i != 0) fstream.write(",".getBytes());
                                final String val = cursor.getString(
                                        cursor.getColumnIndexOrThrow(headers[i])
                                );
                                if (val == null) fstream.write("null".getBytes());
                                else fstream.write(val.getBytes());
                            }
                            fstream.write(line_separator.getBytes());
                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                    fstream.flush();
                    fstream.close();
                    scanFile(MainActivity.this, output);
                            /*MediaScannerConnection.scanFile(MainActivity.this, new String[] {output.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.v("scan complete", path);
                                }
                            });*/
                }catch (NullPointerException npe){
                    npe.printStackTrace();
                    Toast.makeText(this, "Error in opening the Specified file", Toast.LENGTH_LONG).show();
                }
                catch (SQLiteException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error exporting file, is your table empty?", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException io) {
                    io.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this,
                        "External storage not writable.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this,
                    "Must enter a file name.", Toast.LENGTH_SHORT).show();
        }
    }

    public void writeToExportPath(Uri uri){

        String value = mFileName;

        if (uri == null){
            Toast.makeText(this, "Unable to open the Specified file", Toast.LENGTH_LONG).show();
            return;
        }

        if (!value.isEmpty()) {
            if (isExternalStorageWritable()) {
                try {
                    final File output = new File(uri.getPath());
                    final OutputStream fstream = getContentResolver().openOutputStream(uri);
                    final SQLiteDatabase db = mDbHelper.getReadableDatabase();
                    final String table = IdEntryContract.IdEntry.TABLE_NAME;
                    final Cursor cursor = db.query(table, null, null, null, null, null, null);
                    //final Cursor cursor = db.rawQuery("SElECT * FROM VERIFY", null);

                    //first write header line
                    final String[] headers = cursor.getColumnNames();
                    for (int i = 0; i < headers.length; i++) {
                        if (i != 0) fstream.write(",".getBytes());
                        fstream.write(headers[i].getBytes());
                    }
                    fstream.write(line_separator.getBytes());
                    //populate text file with current database values
                    if (cursor.moveToFirst()) {
                        do {
                            for (int i = 0; i < headers.length; i++) {
                                if (i != 0) fstream.write(",".getBytes());
                                final String val = cursor.getString(
                                        cursor.getColumnIndexOrThrow(headers[i])
                                );
                                if (val == null) fstream.write("null".getBytes());
                                else fstream.write(val.getBytes());
                            }
                            fstream.write(line_separator.getBytes());
                        } while (cursor.moveToNext());
                    }

                    cursor.close();
                    fstream.flush();
                    fstream.close();
                    scanFile(MainActivity.this, output);
                            /*MediaScannerConnection.scanFile(MainActivity.this, new String[] {output.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.v("scan complete", path);
                                }
                            });*/
                }catch (NullPointerException npe){
                    npe.printStackTrace();
                    Toast.makeText(this, "Error in opening the Specified file", Toast.LENGTH_LONG).show();
                }
                catch (SQLiteException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error exporting file, is your table empty?", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException io) {
                    io.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this,
                        "External storage not writable.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this,
                    "Must enter a file name.", Toast.LENGTH_SHORT).show();
        }
    }

    //returns index of table with identifier = id, returns -1 if not found
    private int getTableIndexById(String id) {

        ListView idTable = (ListView) findViewById(org.phenoapps.verify.R.id.idTable);
        final int size = idTable.getAdapter().getCount();
        int ret = -1;
        for (int i = 0; i < size; i++) {
            final String temp = (String) idTable.getAdapter().getItem(i);
            if (temp.equals(id)) {
                ret = i;
                break; //break out of for-loop early
            }
        }

        return ret;
    }

    private void updateFilteredArrayAdapter(String id) {

        ListView idTable = (ListView) findViewById(org.phenoapps.verify.R.id.idTable);
        //update id table array adapter
        final ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(this, org.phenoapps.verify.R.layout.row);
        final int oldSize = idTable.getAdapter().getCount();

        for (int i = 0; i < oldSize; i++) {
            final String temp = (String) idTable.getAdapter().getItem(i);
            if (!temp.equals(id)) updatedAdapter.add(temp);
        }
        idTable.setAdapter(updatedAdapter);
    }

    private void ringNotification(boolean success) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean audioEnabled = sharedPref.getBoolean(SettingsActivity.AUDIO_ENABLED, true);

        if(success) { //ID found
            if(audioEnabled) {
                if (success) {
                    try {
                        int resID = getResources().getIdentifier("plonk", "raw", getPackageName());
                        MediaPlayer chimePlayer = MediaPlayer.create(MainActivity.this, resID);
                        chimePlayer.start();

                        chimePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                            }
                        });
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        if(!success) { //ID not found
            ((TextView) findViewById(org.phenoapps.verify.R.id.valueView)).setText("");

            if (audioEnabled) {
                if(!success) {
                    try {
                        int resID = getResources().getIdentifier("error", "raw", getPackageName());
                        MediaPlayer chimePlayer = MediaPlayer.create(MainActivity.this, resID);
                        chimePlayer.start();

                        chimePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            public void onCompletion(MediaPlayer mp) {
                                mp.release();
                            }
                        });
                    } catch (Exception ignore) {
                    }
                }
            } else {
                if (!success) {
                    Toast.makeText(this, "Scanned ID not found", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
    final public boolean onOptionsItemSelected(MenuItem item) {
        DrawerLayout dl = (DrawerLayout) findViewById(R.id.drawer_layout);
        int actionCamera = R.id.action_camera;
        int actionImport = R.id.action_import;
        int actionHome = R.id.Home;
        int actionCompare = R.id.Compare;
        int actionSettings = R.id.Settings;

        if (item.getItemId() == actionImport){
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            final int scanMode = Integer.valueOf(sharedPref.getString(SettingsActivity.SCAN_MODE_LIST, "-1"));
            final Intent i;
            File verifyDirectory = new File(getExternalFilesDir(null), "/Verify");

            File[] files = verifyDirectory.listFiles();


            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select files from?");
            builder.setPositiveButton("Storage",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            Intent i;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            }else{
                                i = new Intent(Intent.ACTION_GET_CONTENT);
                            }
                            i.setType("*/*");
                            startActivityForResult(Intent.createChooser(i, "Choose file to import."), VerifyConstants.DEFAULT_CONTENT_REQ);
                        }
                    });

            builder.setNegativeButton("Verify Directory",
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {

                            AlertDialog.Builder fileBuilder = new AlertDialog.Builder(MainActivity.this);
                            fileBuilder.setTitle("Select the sample file");
                            final int[] checkedItem = {-1};
                            String[] listItems = verifyDirectory.list();
                            fileBuilder.setSingleChoiceItems(listItems, checkedItem[0],(fileDialog, which) -> {
                                checkedItem[0] = which;

                                Intent i = new Intent(MainActivity.this, LoaderDBActivity.class);
                                i.setData(Uri.fromFile(files[which]));
                                startActivityForResult(i, VerifyConstants.LOADER_INTENT_REQ);
                                fileDialog.dismiss();
                            });

                            fileBuilder.show();

                        }
                    });
            builder.show();
        } else if (item.getItemId() == android.R.id.home){
            dl.openDrawer(GravityCompat.START);
        }
        else if (item.getItemId() == actionHome){

        }
        else if ( item.getItemId() == actionCompare) {
            final Intent compareIntent = new Intent(MainActivity.this, CompareActivity.class);
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    startActivity(compareIntent);
                }
            });
        } else if (item.getItemId() == actionSettings) {
            final Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivityForResult(settingsIntent, VerifyConstants.SETTINGS_INTENT_REQ);
        } else if(item.getItemId() == actionCamera){
            final Intent cameraIntent = new Intent(this, ScanActivity.class);
            startActivityForResult(cameraIntent, VerifyConstants.CAMERA_INTENT_REQ);
        }
        else{
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    final protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {

            if (intent != null) {
                switch (requestCode) {
                    case VerifyConstants.PICK_CUSTOM_DEST:
                        writeToExportPath(intent.getData());
                        break;
                    case VerifyConstants.DEFAULT_CONTENT_REQ:
                        Intent i = new Intent(this, LoaderDBActivity.class);
                        i.setData(intent.getData());
                        startActivityForResult(i, VerifyConstants.LOADER_INTENT_REQ);
                        break;
                    case VerifyConstants.LOADER_INTENT_REQ:

                        mListId = null;
                        mPairCol = null;
                        mFileName = "";

                        if (intent.hasExtra(VerifyConstants.FILE_NAME))
                            mFileName = intent.getStringExtra(VerifyConstants.FILE_NAME);
                        if (intent.hasExtra(VerifyConstants.LIST_ID_EXTRA))
                            mListId = intent.getStringExtra(VerifyConstants.LIST_ID_EXTRA);
                        if (intent.hasExtra(VerifyConstants.PAIR_COL_EXTRA))
                            mPairCol = intent.getStringExtra(VerifyConstants.PAIR_COL_EXTRA);

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        final SharedPreferences.Editor editor = sharedPref.edit();

                        int scanMode = Integer.valueOf(sharedPref.getString(SettingsActivity.SCAN_MODE_LIST, "-1"));

                        if (mPairCol != null) {
                            editor.putBoolean(SettingsActivity.DISABLE_PAIR, false);
                            if (scanMode != 4) showPairDialog();
                        } else {
                            editor.putBoolean(SettingsActivity.DISABLE_PAIR, true);
                        }

                        if (mPairCol == null && scanMode == 4) {
                            editor.putString(SettingsActivity.SCAN_MODE_LIST, "0");
                            Toast.makeText(this,
                                    "Switching to default mode, no pair ID found.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        editor.putString(SettingsActivity.FILE_NAME, mFileName);
                        editor.putString(SettingsActivity.PAIR_NAME, mPairCol);
                        editor.putString(SettingsActivity.LIST_KEY_NAME, mListId);
                        editor.apply();

                        clearListView();
                        loadSQLToLocal();
                        updateCheckedItems();
                        break;
                }

                if (intent.hasExtra(VerifyConstants.CAMERA_RETURN_ID)) {
                    ((EditText) findViewById(org.phenoapps.verify.R.id.scannerTextView))
                            .setText(intent.getStringExtra(VerifyConstants.CAMERA_RETURN_ID));
                    checkScannedItem();
                }
            }
        }
    }

    private void buildListView() {

        ListView idTable = (ListView) findViewById(org.phenoapps.verify.R.id.idTable);
ArrayAdapter<String> idAdapter =
new ArrayAdapter<>(this, org.phenoapps.verify.R.layout.row);
        int size = mIds.size();
        for (int i = 0; i < size; i++) {
idAdapter.add(this.mIds.get(this.mIds.keyAt(i)));
        }
idTable.setAdapter(idAdapter);
    }

    private void clearListView() {

        ListView idTable = (ListView) findViewById(org.phenoapps.verify.R.id.idTable);
final ArrayAdapter<String> adapter =
new ArrayAdapter<>(this, org.phenoapps.verify.R.layout.row);

        idTable.setAdapter(adapter);
adapter.notifyDataSetChanged();
    }

    private void showPairDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pair column selected, would you like to switch to Pair mode?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(SettingsActivity.SCAN_MODE_LIST, "4");
                editor.apply();
            }
        });

        builder.setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }

    @Override
    final protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    final public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void launchIntro() {

        new Thread(new Runnable() {
            @Override
            public void run() {

            //  Launch app intro
            final Intent i = new Intent(MainActivity.this, IntroActivity.class);

            runOnUiThread(new Runnable() {
                @Override public void run() {
                    startActivity(i);
                }
            });


            }
        }).start();
    }

    /* Checks if external storage is available for read and write */
    static private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    @Override
    final public void onDestroy() {
        mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, String[] permissions, int[] granted) {

        super.onRequestPermissionsResult(resultCode, permissions, granted);
        boolean externalWriteAccept = false;
        if (resultCode == VerifyConstants.PERM_REQ) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals("android.permission.WRITE_EXTERNAL_STORAGE")) {
                    externalWriteAccept = true;
                }
            }
        }
        if (externalWriteAccept && isExternalStorageWritable()) {

            File verifyDirectory = new File(getExternalFilesDir(null), "/Verify");

            if (!verifyDirectory.isDirectory()) {
                final boolean makeDirsSuccess = verifyDirectory.mkdirs();
                if (!makeDirsSuccess) Log.d("Verify Make Directory", "failed");
            }
            copyRawToVerify(verifyDirectory, "field_sample.csv", R.raw.field_sample);
            copyRawToVerify(verifyDirectory, "verify_pair_sample.csv", R.raw.verify_pair_sample);
        }
    }

    @Override
    final public void onPause() {
        super.onPause();
    }

}
