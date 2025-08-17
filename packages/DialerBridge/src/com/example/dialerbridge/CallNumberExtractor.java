package com.example.dialerbridge;

import android.net.Uri;
import android.telecom.Call;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CallNumberExtractor {
    private CallNumberExtractor() {}

    @NonNull
    public static String getDisplayNumber(@Nullable Call call) {
        if (call == null || call.getDetails() == null) return "unknown";
        Uri handle = call.getDetails().getHandle();
        if (handle == null) return "unknown";
        String scheme = handle.getScheme();
        String schemeSpecific = handle.getSchemeSpecificPart();
        if (schemeSpecific == null || schemeSpecific.isEmpty()) return scheme != null ? scheme : "unknown";
        return schemeSpecific;
    }
}