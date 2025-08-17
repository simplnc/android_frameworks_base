package com.example.dialerbridge;

import android.media.AudioFormat;
import android.media.MediaRecorder;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Immutable recording configuration.
 */
public final class RecordingConfig {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.VOICE_COMMUNICATION})
    public @interface AudioSource {}

    public static final int DEFAULT_SAMPLE_RATE_HZ = 16000;
    public static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

    public final int sampleRateHz;
    public final int channelConfig;
    public final int audioFormat;
    public final int audioSource;

    private RecordingConfig(int sampleRateHz, int channelConfig, int audioFormat, int audioSource) {
        this.sampleRateHz = sampleRateHz;
        this.channelConfig = channelConfig;
        this.audioFormat = audioFormat;
        this.audioSource = audioSource;
    }

    @NonNull
    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private int sampleRateHz = DEFAULT_SAMPLE_RATE_HZ;
        private int channelConfig = DEFAULT_CHANNEL_CONFIG;
        private int audioFormat = DEFAULT_AUDIO_FORMAT;
        private int audioSource = DEFAULT_AUDIO_SOURCE;

        public Builder setSampleRateHz(int value) {
            this.sampleRateHz = value; return this;
        }
        public Builder setChannelConfig(int value) {
            this.channelConfig = value; return this;
        }
        public Builder setAudioFormat(int value) {
            this.audioFormat = value; return this;
        }
        public Builder setAudioSource(@AudioSource int value) {
            this.audioSource = value; return this;
        }
        public RecordingConfig build() {
            return new RecordingConfig(sampleRateHz, channelConfig, audioFormat, audioSource);
        }
    }
}