package com.example.dialerbridge;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.text.format.DateFormat;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Locale;

/**
 * Minimal call recording controller with clear API surface for a Dialer.
 * This is intentionally independent and high-verbosity for readability.
 */
public final class CallRecordingController {
    public interface Listener {
        void onRecordingStarted(@NonNull File file);
        void onRecordingError(@NonNull String errorMessage);
        void onRecordingFinished(@NonNull File file);
    }

    private static final String NOTIFICATION_CHANNEL_ID = "dialer_recording";

    private final Context applicationContext;
    private final Listener listener;

    private volatile boolean recording;
    @Nullable private Thread recordingThread;
    private RecordingConfig config = RecordingConfig.newBuilder().build();
    private boolean manageNotification = true;

    public CallRecordingController(@NonNull Context context, @NonNull Listener listener) {
        this.applicationContext = context.getApplicationContext();
        this.listener = listener;
    }

    public void setConfig(@NonNull RecordingConfig config) {
        this.config = config;
    }

    public void setManageNotification(boolean value) {
        this.manageNotification = value;
    }

    public boolean hasRequiredPermissions() {
        boolean recordAudio = applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean writeStorage;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeStorage = true; // app-specific storage, or MediaStore
        } else {
            writeStorage = applicationContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return recordAudio && writeStorage;
    }

    @MainThread
    public synchronized void startRecording(@NonNull String phoneNumber, boolean isIncoming) {
        if (recording) return;
        if (!hasRequiredPermissions()) {
            listener.onRecordingError("Missing RECORD_AUDIO or storage permission");
            return;
        }

        File outputFile = buildOutputFile(phoneNumber, isIncoming);
        if (manageNotification) {
            ensureOngoingNotification();
        }

        recording = true;
        recordingThread = new Thread(() -> doRecord(outputFile), "DialerBridge-Recorder");
        recordingThread.start();
        listener.onRecordingStarted(outputFile);
    }

    @MainThread
    public synchronized void stopRecording() {
        recording = false;
    }

    private File buildOutputFile(String phoneNumber, boolean isIncoming) {
        String direction = isIncoming ? "IN" : "OUT";
        String dateStr = DateFormat.format("yyyyMMdd_HHmmss", new Date()).toString();
        String baseName = String.format(Locale.US, "%s_%s_%s.wav", direction, phoneNumber, dateStr);

        File dir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (dir == null) {
            dir = applicationContext.getFilesDir();
        }
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, baseName);
    }

    private void ensureOngoingNotification() {
        NotificationManager nm = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Call Recording",
                    NotificationManager.IMPORTANCE_LOW
            );
            nm.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                : new Notification.Builder(applicationContext);
        builder.setContentTitle("Recording call")
                .setContentText("Call recording in progress")
                .setSmallIcon(android.R.drawable.presence_audio_busy)
                .setOngoing(true);
        nm.notify(1001, builder.build());
    }

    private void clearOngoingNotification() {
        NotificationManager nm = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(1001);
    }

    private void doRecord(File output) {
        int sampleRate = config.sampleRateHz;
        int channelConfig = config.channelConfig;
        int audioFormat = config.audioFormat;
        int source = config.audioSource;
        int minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        AudioRecord recorder = new AudioRecord(
                source,
                sampleRate,
                channelConfig,
                audioFormat,
                Math.max(minBuffer, 4096)
        );
        ByteBuffer buffer = ByteBuffer.allocateDirect(Math.max(minBuffer, 4096));

        try {
            java.io.OutputStream out;
            java.io.FileOutputStream fallback = null;
            java.io.OutputStream mediaOut = MediaStoreWriter.openOutputStream(applicationContext, output);
            if (mediaOut != null) {
                out = mediaOut;
            } else {
                fallback = new FileOutputStream(output);
                out = fallback;
            }
            recorder.startRecording();
            while (recording) {
                int read = recorder.read(buffer, buffer.capacity());
                if (read > 0) {
                    byte[] tmp = new byte[read];
                    buffer.rewind();
                    buffer.get(tmp, 0, read);
                    out.write(tmp);
                    buffer.clear();
                }
            }
            recorder.stop();
            if (manageNotification) {
                clearOngoingNotification();
            }
            out.close();
            listener.onRecordingFinished(output);
        } catch (IOException ioe) {
            listener.onRecordingError("I/O error: " + ioe.getMessage());
        } catch (Throwable t) {
            listener.onRecordingError("Unexpected error: " + t.getMessage());
        } finally {
            try { recorder.release(); } catch (Throwable ignored) {}
        }
    }
}