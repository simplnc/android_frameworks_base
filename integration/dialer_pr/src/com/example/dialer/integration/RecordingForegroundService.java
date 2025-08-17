package com.example.dialer.integration;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class RecordingForegroundService extends Service {
    public static final String ACTION_START = "com.example.dialer.integration.START";
    public static final String ACTION_STOP = "com.example.dialer.integration.STOP";
    public static final String ACTION_ERROR = "com.example.dialer.integration.ERROR";
    public static final String EXTRA_FILE = "extra_file";
    public static final String EXTRA_ERROR = "extra_error";

    private static final String CHANNEL_ID = "dialer_recording";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            ensureChannel();
            Notification n = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Recording call")
                    .setContentText("Recording in progress")
                    .setSmallIcon(android.R.drawable.presence_audio_busy)
                    .setOngoing(true)
                    .build();
            startForeground(1001, n);
        } else if (ACTION_ERROR.equals(action)) {
            stopForeground(true);
            stopSelf();
        } else if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Call Recording", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(channel);
            }
        }
    }
}