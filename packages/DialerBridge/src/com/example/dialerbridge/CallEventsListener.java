package com.example.dialerbridge;

import android.telecom.Call;
import android.telecom.InCallService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for wiring InCallService events to the CallRecordingController.
 */
public final class CallEventsListener {
    private final CallRecordingController controller;

    @Nullable private Call currentCall;
    private boolean isIncoming;

    public CallEventsListener(@NonNull CallRecordingController controller) {
        this.controller = controller;
    }

    public void onCallAdded(@NonNull InCallService service, @NonNull Call call) {
        this.currentCall = call;
        this.isIncoming = call.getDetails() != null && call.getDetails().getCallDirection() == Call.Details.DIRECTION_INCOMING;
        String number = CallNumberExtractor.getDisplayNumber(call);
        controller.startRecording(number, isIncoming);
    }

    public void onCallRemoved(@NonNull InCallService service, @NonNull Call call) {
        if (call == currentCall) {
            controller.stopRecording();
            currentCall = null;
        }
    }
}