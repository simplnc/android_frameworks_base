package com.example.dialerbridge;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple auto-record rules. In production, back this with persistent storage.
 */
public final class AutoRecordRules {
    private final Set<String> numbersAlways;
    private final boolean recordIncoming;
    private final boolean recordOutgoing;

    private AutoRecordRules(Set<String> numbersAlways, boolean recordIncoming, boolean recordOutgoing) {
        this.numbersAlways = numbersAlways;
        this.recordIncoming = recordIncoming;
        this.recordOutgoing = recordOutgoing;
    }

    public boolean shouldRecord(@NonNull String number, boolean isIncoming) {
        if (numbersAlways.contains(number)) return true;
        if (isIncoming && recordIncoming) return true;
        return !isIncoming && recordOutgoing;
    }

    public static Builder newBuilder() { return new Builder(); }

    public static final class Builder {
        private final Set<String> numbers = new HashSet<>();
        private boolean incoming;
        private boolean outgoing;
        public Builder addAlwaysRecordNumber(String e164) { numbers.add(e164); return this; }
        public Builder setRecordIncoming(boolean value) { incoming = value; return this; }
        public Builder setRecordOutgoing(boolean value) { outgoing = value; return this; }
        public AutoRecordRules build() { return new AutoRecordRules(numbers, incoming, outgoing); }
    }
}