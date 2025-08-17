package com.example.dialerbridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

final class NotificationActions {
    private NotificationActions() {}

    static Notification addStopAction(@NonNull Context context, @NonNull Notification base, @NonNull PendingIntent stopPi) {
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, "dialer_recording")
                : new Notification.Builder(context);
        b.setContentTitle("Recording call")
                .setContentText("Tap to stop")
                .setSmallIcon(android.R.drawable.presence_audio_busy)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,
                        "Stop",
                        stopPi).build());
        return b.build();
    }
}