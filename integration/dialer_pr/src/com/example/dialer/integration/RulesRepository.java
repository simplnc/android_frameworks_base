package com.example.dialer.integration;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class RulesRepository {
    private static final String PREFS = "recording_rules";
    private static final String KEY_AUTO_IN = "auto_in";
    private static final String KEY_AUTO_OUT = "auto_out";
    private static final String KEY_SOURCE = "source"; // mic|voice_comm

    private final SharedPreferences prefs;

    public RulesRepository(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isAutoIncoming() { return prefs.getBoolean(KEY_AUTO_IN, false); }
    public boolean isAutoOutgoing() { return prefs.getBoolean(KEY_AUTO_OUT, false); }
    @NonNull public String getAudioSource() { return prefs.getString(KEY_SOURCE, "mic"); }

    public void setAutoIncoming(boolean value) { prefs.edit().putBoolean(KEY_AUTO_IN, value).apply(); }
    public void setAutoOutgoing(boolean value) { prefs.edit().putBoolean(KEY_AUTO_OUT, value).apply(); }
    public void setAudioSource(@NonNull String value) { prefs.edit().putString(KEY_SOURCE, value).apply(); }
}