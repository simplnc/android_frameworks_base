/*
 * Copyright (C) 2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.keyguard;

import android.content.Context;
import android.view.View;

/**
 * Disabled stub for OnePlus-style lock screen controller.
 * This no-op implementation ensures vendor-specific behavior is not executed.
 */
public class OnePlusLockScreenController {
    public OnePlusLockScreenController(Context context, View rootView) {
        // no-op
    }

    public void setNotificationCount(int count) {
        // no-op
    }

    public void onDestroy() {
        // no-op
    }

    public void refresh() {
        // no-op
    }
}
