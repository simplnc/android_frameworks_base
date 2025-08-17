package com.example.dialerbridge;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

final class MediaStoreWriter {
    private MediaStoreWriter() {}

    @Nullable
    public static OutputStream openOutputStream(@NonNull Context context, @NonNull File suggestedFile) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null; // Caller should write to file directly
        }
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, suggestedFile.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/CallRecordings");
        Uri uri = cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Failed to insert into MediaStore");
        return cr.openOutputStream(uri, "w");
    }
}