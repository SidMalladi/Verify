package org.phenoapps.verify;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;

import org.phenoapps.verify.R;

public class LoaderDBActivity extends AppCompatActivity {

    private HashSet<String> displayCols;

    private Uri mFileUri;
    private String mDelimiter;
    private String mFileExtension;
    private String mFilePath;
    private String mFileName;

    private Workbook mCurrentWorkbook;

    private String mIdHeader;

    private Button finishButton, doneButton,
            chooseHeaderButton, choosePairButton, skipButton;
    private ListView headerList;
    private LinearLayout pairLayout;
    private TextView tutorialText;
    private EditText separatorText;
    private String mHeader;
    private String mPairCol;
    private int mIdHeaderIndex;
    private int mPairColIndex;
    private HashSet<String> mDefaultCols;

    private IdEntryDbHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_load_file);

        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle(null);
            getSupportActionBar().getThemedContext();
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        ActivityCompat.requestPermissions(this, VerifyConstants.permissions, VerifyConstants.PERM_REQ);

        mDbHelper = new IdEntryDbHelper(this);

        displayCols = new HashSet<>(10);

        //default column names
        mDefaultCols = new HashSet<>(5);
        mDefaultCols.add("date");
        mDefaultCols.add("color");
        mDefaultCols.add("scan_count");
        mDefaultCols.add("user");
        mDefaultCols.add("note");

        initializeUI();

        mFileUri = getIntent().getData();

        int lastSlash = mFileUri.getPath().lastIndexOf('/');
        if (lastSlash != -1) {
            mFileName = mFileUri.getPath().substring(lastSlash + 1);
        } else mFileName = "";

        parseHeaders(mFileUri);

        //if unsupported file type, start delimiter tutorial
        if (mDelimiter == null || mHeader == null) {
            Toast.makeText(this, "There was a problem reading this file.", Toast.LENGTH_LONG).show();
            finish();
           /* if (mHeader == null) {
                tutorialText.setText("Error reading file.");
            } else {
                separatorText.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                doneButton.setVisibility(View.VISIBLE);
                tutorialText.setText(org.phenoapps.verify.R.string.choose_separator_tutorial);
                tutorialText.append(mHeader);
            }*/

        } else { //display header list
            displayHeaderList();
        }
    }

    private void parseHeaders(Uri data) {

        try {
            //query file path type
            mFilePath = getPath(LoaderDBActivity.this ,mFileUri);
            int lastDot = mFilePath.toString().lastIndexOf("."); // changed from mFileUri to mFilePath due to the files in download folder have URI without extension

            if (lastDot == -1) {
                Toast.makeText(this, "Imported file must have an extension. (e.g: .csv, .tsv)", Toast.LENGTH_LONG).show();
                finish();
            }


            mFileExtension = mFilePath.substring(lastDot + 1);
            StringBuilder header = new StringBuilder();

            //xls library support
            if (mCurrentWorkbook != null) mCurrentWorkbook.close();

            if (mFileExtension.equals("xlsx") || mFileExtension.equals("xls")) {
                mCurrentWorkbook = WorkbookFactory.create(new File(mFilePath));

                final int numSheets = mCurrentWorkbook.getNumberOfSheets();
                if (numSheets > 0) {
                    final Sheet s = mCurrentWorkbook.getSheetAt(0);
                    final Iterator rows = s.rowIterator();
                    final Row row = (Row) rows.next();
                    final Iterator cells = row.cellIterator();
                    while (cells.hasNext()) {
                        final Cell cell = (Cell) cells.next();
                        header.append(cell.toString());
                        if (cells.hasNext()) header.append(",");
                    }

                    mDelimiter = ",";
                    mHeader = header.toString();
                }
            } else {
                //plain text file support
                if (mFileExtension.equals("csv")) { //files ending in .csv
                    mDelimiter = ",";
                } else if (mFileExtension.equals("tsv") || mFileExtension.equals("txt")) { //files ending in .txt
                    mDelimiter = "\t";
                } else
                    mDelimiter = null; //non-supported file type, display header for user to choose delimiter

                final InputStream is = getContentResolver().openInputStream(mFileUri);
                if (is != null) {
                    final BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    mHeader = br.readLine();
                    br.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeUI() {

        headerList = ((ListView) findViewById(org.phenoapps.verify.R.id.headerList));

        headerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        headerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                chooseHeaderButton.setEnabled(true);
                mIdHeaderIndex = position;
                mIdHeader = ((TextView) view).getText().toString().replaceAll("\\s+", "");
            }
        });

        tutorialText = (TextView) findViewById(R.id.tutorialTextView);
        separatorText = (EditText) findViewById(org.phenoapps.verify.R.id.separatorTextView);

        pairLayout = (LinearLayout) findViewById(R.id.pairLayout);
        choosePairButton = (Button) findViewById(org.phenoapps.verify.R.id.choosePairButton);
        chooseHeaderButton = (Button) findViewById(org.phenoapps.verify.R.id.chooseHeaderButton);
        doneButton = ((Button) findViewById(org.phenoapps.verify.R.id.doneButton));
        finishButton = ((Button) findViewById(org.phenoapps.verify.R.id.finishButton));
        skipButton = ((Button) findViewById(R.id.skipButton));

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                doneButton.setVisibility(View.GONE);
                separatorText.setVisibility(View.GONE);
                mDelimiter = separatorText.getText().toString();
                if (mDelimiter.isEmpty()) mDelimiter = ",";
                displayHeaderList();
            }
        });

        chooseHeaderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                chooseHeaderButton.setVisibility(View.GONE);

                tutorialText.setText(org.phenoapps.verify.R.string.choose_pair_button_tutorial);
                pairLayout.setVisibility(View.VISIBLE);
                choosePairButton.setEnabled(false);

                headerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        choosePairButton.setEnabled(true);
                        mPairCol = ((TextView) view).getText().toString();
                        mPairColIndex = position;
                    }
                });

                displayColsList(true);

            }
        });

        choosePairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pairLayout.setVisibility(View.GONE);
                tutorialText.setText(org.phenoapps.verify.R.string.columns_tutorial);
                finishButton.setVisibility(View.VISIBLE);
                finishButton.setEnabled(false);
                displayColsList(false);
            }
        });

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //create database
                insertColumns();

                //initialize intent
                final Intent intent = new Intent();
                intent.putExtra(VerifyConstants.LIST_ID_EXTRA, mIdHeader);
                intent.putExtra(VerifyConstants.PAIR_COL_EXTRA, mPairCol);
                intent.putExtra(VerifyConstants.FILE_NAME, mFileName);
                setResult(RESULT_OK, intent);
                finish();

            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                mPairCol = null;
                mPairColIndex = -1;
                choosePairButton.setVisibility(View.GONE);
                skipButton.setVisibility(View.GONE);
                tutorialText.setText(org.phenoapps.verify.R.string.columns_tutorial);
                finishButton.setVisibility(View.VISIBLE);
                finishButton.setEnabled(false);
                displayColsList(false);
            }
        });
    }

    private synchronized void insertColumns() {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS VERIFY");

        final StringBuilder dbExecCreate = new StringBuilder(32);

        dbExecCreate.append("CREATE TABLE VERIFY(");
        dbExecCreate.append(mIdHeader);
        dbExecCreate.append(" TEXT PRIMARY KEY");

        if (mPairCol != null) {
            dbExecCreate.append(", ");
            dbExecCreate.append(mPairCol);
            dbExecCreate.append(" TEXT");
        }
        dbExecCreate.append(", date DATE");
        dbExecCreate.append(", user TEXT");
        dbExecCreate.append(", note TEXT");
        dbExecCreate.append(", scan_count INT DEFAULT 0");
        dbExecCreate.append(", color INT");

        final String[] cols = displayCols.toArray(new String[] {});
        final int colSize = cols.length;
        for (int i = 0; i < colSize; i++) {
            if (!cols[i].matches("[0-9]+")) {
                dbExecCreate.append(',');
                dbExecCreate.append(cols[i].replaceAll("\\s+", ""));
                dbExecCreate.append(" TEXT");
            }
        }
        dbExecCreate.append(");");

        try {
            db.execSQL(dbExecCreate.toString());

        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        db.close();

        if (mFileExtension.equals("xls")) {
            parseAndInsertXLS();
        } else if (mFileExtension.equals("xlsx")) {
            parseAndInsertXLS();
        } else {
            parseAndInsertCSV();
        }
    }

    private void parseAndInsertCSV() {

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            final InputStream is = getContentResolver().openInputStream(mFileUri);
            if (is != null) {

                if (mDelimiter != null) {
                    final BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    final String[] headers = br.readLine().replaceAll("\\s+", "").split(mDelimiter);

                    String temp;
                    ContentValues entry = new ContentValues();
                    db.beginTransaction();
                    while ((temp = br.readLine()) != null) {
                        String[] id_line = temp.replaceAll("\\s+", "").split(mDelimiter);
                        int size = id_line.length;
                        if (size != 0 && size <= headers.length) {

                            entry.put(headers[mIdHeaderIndex], id_line[mIdHeaderIndex]);

                            for (int i = 0; i < size; i++) {

                                if (displayCols.contains(headers[i]) || mDefaultCols.contains(headers[i]) ||
                                        headers[i].equals(mPairCol)) {
                                    entry.put(headers[i], id_line[i]);
                                }
                            }
                            long newRowId = db.insert("VERIFY", null, entry);
                            entry.clear();
                        } else Log.d("ROW ERROR", temp);
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseAndInsertXLS() {

        try {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            final int numSheets = mCurrentWorkbook.getNumberOfSheets();
            if (numSheets > 0) {
                final Sheet s = mCurrentWorkbook.getSheetAt(0);
                final Iterator rowIterator = s.rowIterator();
                final Row headerRow = (Row) rowIterator.next(); //skip first header line
                final String[] headers = new String[(int) headerRow.getLastCellNum()];
                final Iterator headerIterator = headerRow.cellIterator();
                int count = 0;
                while (headerIterator.hasNext()) {
                    headers[count] = ((Cell) headerIterator.next()).getStringCellValue();
                    count++;
                }

                db.beginTransaction();

                ContentValues entry = new ContentValues();
                while (rowIterator.hasNext()) {
                    Iterator cellIterator =
                            ((Row) rowIterator.next()).cellIterator();
                    count = 0;
                    while (cellIterator.hasNext()) {
                        String val = ((Cell) cellIterator.next()).toString();
                        if (displayCols.contains(headers[count]) || mDefaultCols.contains(headers[count])
                                || headers[count].equals(mPairCol) || headers[count].equals(mIdHeader))
                            entry.put(headers[count], val);
                        count++;
                    }
                    long rowId = db.insert("VERIFY", null, entry);
                    entry.clear();
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        } catch(IllegalStateException e) {
            e.printStackTrace();
            //probably from importing improper SQLite column name
        }
    }

    private void displayColsList(boolean pairMode) {

        headerList.setVisibility(View.VISIBLE);

        final String[] headers = mHeader.split(mDelimiter);
        if (headers.length > 0 && headers[0] != null) {
            final ArrayAdapter<String> idAdapter =
                    new ArrayAdapter<>(this, org.phenoapps.verify.R.layout.row);
            for (String h : headers) {
                if (!h.equals(headers[mIdHeaderIndex]) && !mDefaultCols.contains(h) &&
                        !h.equals(mPairCol)) {
                    idAdapter.add(h);
                }
            }
            headerList.setAdapter(idAdapter);
        }

        if (pairMode && mPairCol == null) {
            headerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            headerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    choosePairButton.setEnabled(true);
                    mPairCol = ((TextView) view).getText().toString().replaceAll("\\s+", "");
                }
            });
        } else {
            headerList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

            //Select all by default
            for(int i = 0; i < headerList.getCount(); i++) {
                headerList.setItemChecked(i, !headerList.isItemChecked(i));
                final String newCol = headerList.getAdapter().getItem(i).toString();
                if (displayCols.contains(newCol)) displayCols.remove(newCol);
                displayCols.add(newCol.replaceAll("\\s+", ""));
            }
            finishButton.setEnabled(true);

            headerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final String newCol = ((TextView) view).getText().toString();
                    if (displayCols.contains(newCol)) displayCols.remove(newCol);
                    else displayCols.add(newCol.replaceAll("\\s+", ""));
                }
            });
        }
    }

    private void displayHeaderList() {

        tutorialText.setText(org.phenoapps.verify.R.string.choose_header_tutorial);
        chooseHeaderButton.setVisibility(View.VISIBLE);
        chooseHeaderButton.setEnabled(false);
        headerList.setVisibility(View.VISIBLE);

        if (mHeader == null) {
            headerList.setAdapter(new ArrayAdapter<String>(this, org.phenoapps.verify.R.layout.row));
            tutorialText.setText("Error reading file.");
            return;
        }

        final String[] headers = mHeader.split(mDelimiter);
        if (headers.length > 0 && headers[0] != null) {
            final ArrayAdapter<String> idAdapter =
                    new ArrayAdapter<>(this, org.phenoapps.verify.R.layout.row);
            for (String h : headers) {
                if (!mDefaultCols.contains(h)) idAdapter.add(h);
            }
            headerList.setAdapter(idAdapter);
        } else {
            headerList.setAdapter(new ArrayAdapter<String>(this, org.phenoapps.verify.R.layout.row));
            tutorialText.setText("Error reading file.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileUri != null)
            outState.putString(VerifyConstants.CSV_URI, mFileUri.toString());
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

    public static String getPath(final Context context, final Uri uri){
        String absolutePath = getLocalPath(context, uri);
        return absolutePath != null ? absolutePath : uri.toString();
    }


    //based on https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
    public static String getLocalPath(final Context context , Uri uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {

                if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                    final String[] doc =  DocumentsContract.getDocumentId(uri).split(":");
                    final String documentType = doc[0];
                    
                    if ("primary".equalsIgnoreCase(documentType)) {
                        return Environment.getExternalStorageDirectory() + "/" + doc[1];
                        }
                } else if ("com.android.providers.media.documents".equals(uri.getAuthority()) ||
                        "com.google.android.apps.docs.storage".equals(uri.getAuthority()) ||
                        "com.microsoft.skydrive.content.StorageAccessProvider".equals(uri.getAuthority())) {
                    String fileName = getFileName(context, uri);
                    File cacheDir = getDocumentCacheDir(context);
                    File file = generateFileName(fileName, cacheDir);
                    String destinationPath = null;
                    if (file != null) {
                        destinationPath = file.getAbsolutePath();
                        saveFileFromUri(context, uri, destinationPath);
                    }
                    return destinationPath;
                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (!id.isEmpty()) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                    }
                    String[] contentUriPrefixesToTry = new String[]{
                            "content://downloads/public_downloads",
                            "content://downloads/my_downloads",
                            "content://downloads/all_downloads"
                    };

                    for (String contentUriPrefix : contentUriPrefixesToTry) {
                        try {
                            Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                            String path = getDataColumn(context, contentUri, null, null);
                            if (path != null) {
                                return path;
                            }
                        } catch (Exception e) {}
                    }

                    String fileName = getFileName(context, uri);
                    File cacheDir = getDocumentCacheDir(context);
                    File file = generateFileName(fileName, cacheDir);
                    String destinationPath = null;
                    if (file != null) {
                        destinationPath = file.getAbsolutePath();
                        saveFileFromUri(context, uri, destinationPath);
                    }
                    return destinationPath;
                }
            }
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            } else if ("com.estrongs.files".equals(uri.getAuthority())) {
                return uri.getPath();
            }
        }
        return null;
    }

    public static String getFileName(@NonNull Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String fileName = null;

        if (mimeType == null && context != null) {
            String path = getPath(context, uri);
            if (path == null) {
                fileName = getName(uri.toString());
            } else {
                File file = new File(path);
                fileName = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null,
                    null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                fileName = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }

        return fileName;
    }

    public static String getName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf('/');
        return fileName.substring(index + 1);
    }

    public static File getDocumentCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), "documents");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File generateFileName(@Nullable String name, File directory) {
        if (name == null) {
            return null;
        }

        File file = new File(directory, name);

        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }

            int index = 0;

            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }

        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }

        return file;
    }

    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream uriInputStream = null;
        BufferedOutputStream bufferStream = null;
        try {
            uriInputStream = context.getContentResolver().openInputStream(uri);
            bufferStream = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            uriInputStream.read(buf);
            do {
                bufferStream.write(buf);
            } while (uriInputStream.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (uriInputStream != null) uriInputStream.close();
                if (bufferStream != null) bufferStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
