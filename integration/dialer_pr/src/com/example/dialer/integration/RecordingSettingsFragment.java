package com.example.dialer.integration;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.dialer_pr.R;

public class RecordingSettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.recording_preferences, rootKey);
        SwitchPreferenceCompat incoming = findPreference("pref_auto_incoming");
        SwitchPreferenceCompat outgoing = findPreference("pref_auto_outgoing");
        ListPreference source = findPreference("pref_audio_source");
        if (source != null) {
            source.setEntries(new CharSequence[]{"Microphone", "Voice communication"});
            source.setEntryValues(new CharSequence[]{"mic", "voice_comm"});
        }
    }
}