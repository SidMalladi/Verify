package org.phenoapps.verify;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;




public class UriHandler {

    /**
     * Resolve the file name from the content Uri.
     * @param context
     * @param uri
     * @return
     */
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

    /**
     * Returns the effective file name from the provided Uri.
     * @param fileName
     * @return
     */
    public static String getName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf('/');
        return fileName.substring(index + 1);
    }

    /**
     * Returns the documents directory in the storage.
     * @param context
     * @return
     */
    public static File getDocumentCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), "documents");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Generates new file in the specified cached directory.
     * @param name
     * @param directory
     * @return
     */
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

    /**
     * creates a locally cached file for content from any cloud provider.
     * @param context
     * @param uri
     * @param destinationPath
     */

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

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
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

    /**
     * The utility method helps to resolve the local path of a file from a content Uri.
     * @param context
     * @param uri
     * @return
     */
    public static String getLocalPath(final Context context, Uri uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {

                if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                    final String[] doc = DocumentsContract.getDocumentId(uri).split(":");
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
                        } catch (Exception e) {
                        }
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
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            } else if ("com.estrongs.files".equals(uri.getAuthority())) {
                return uri.getPath();
            }
        }
        return null;
    }


//  All the class methods implemented in reference to https://github.com/coltoscosmin/FileUtils/blob/master/FileUtils.java#L290

    /**
     *
     * @param context the context from which the utility method is called in
     * @param uri uri of the file to access
     * @return path of the file to be accessed
     */
    public static String getPath(final Context context, final Uri uri) {
        String absolutePath = getLocalPath(context, uri);
        return absolutePath != null ? absolutePath : uri.toString();
    }
}