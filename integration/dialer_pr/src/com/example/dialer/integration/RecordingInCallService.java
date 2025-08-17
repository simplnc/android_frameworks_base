package com.example.dialer.integration;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;

import androidx.annotation.Nullable;

import com.example.dialerbridge.AutoRecordRules;
import com.example.dialerbridge.CallEventsListener;
import com.example.dialerbridge.CallRecordingController;
import com.example.dialerbridge.RecordingConfig;

public class RecordingInCallService extends InCallService implements CallRecordingController.Listener {
    private CallRecordingController controller;
    private CallEventsListener listener;

    @Override
    public void onCallAdded(Call call) {
        ensureController();
        listener.onCallAdded(this, call);
    }

    @Override
    public void onCallRemoved(Call call) {
        if (listener != null) listener.onCallRemoved(this, call);
    }

    private void ensureController() {
        if (controller != null) return;
        controller = new CallRecordingController(getApplicationContext(), this);
        controller.setManageNotification(false); // ForegroundService will manage notification
        controller.setConfig(RecordingConfig.newBuilder()
                .setAudioSource(android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setSampleRateHz(16000)
                .build());
        AutoRecordRules rules = AutoRecordRules.newBuilder()
                .setRecordIncoming(true)
                .setRecordOutgoing(true)
                .build();
        listener = new CallEventsListener(controller, rules);
    }

    @Override
    public void onRecordingStarted(java.io.File file) {
        Intent i = new Intent(this, RecordingForegroundService.class)
                .setAction(RecordingForegroundService.ACTION_START)
                .putExtra(RecordingForegroundService.EXTRA_FILE, file.getAbsolutePath());
        startForegroundService(i);
    }

    @Override
    public void onRecordingError(String errorMessage) {
        Intent i = new Intent(this, RecordingForegroundService.class)
                .setAction(RecordingForegroundService.ACTION_ERROR)
                .putExtra(RecordingForegroundService.EXTRA_ERROR, errorMessage);
        startForegroundService(i);
    }

    @Override
    public void onRecordingFinished(java.io.File file) {
        Intent i = new Intent(this, RecordingForegroundService.class)
                .setAction(RecordingForegroundService.ACTION_STOP);
        startForegroundService(i);
    }
}