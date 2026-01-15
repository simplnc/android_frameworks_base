/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android;

/**
 * Constants for the Android Manifest.
 */
public final class Manifest {
    /**
     * @hide
     */
    public Manifest() {}

    /**
     * The permissions that applications may request.
     */
    public static final class permission {
        /**
         * @hide
         */
        public permission() {}

        /**
         * Allows an application to manage device policy sandbox features.
         * @hide
         */
        public static final String MANAGE_DEVICE_POLICY_SANDBOX =
                "android.permission.MANAGE_DEVICE_POLICY_SANDBOX";
    }
}