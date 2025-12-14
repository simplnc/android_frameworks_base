/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.systemui.android.header;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;

import com.android.systemui.res.R;
import com.android.internal.util.android.Utils;

public class StaticHeaderProvider implements
        StatusBarHeaderMachine.IStatusBarHeaderProvider {

    public static final String TAG = "StaticHeaderProvider";
    private static final boolean DEBUG = false;

    private Context mContext;
    private Resources mRes;
    private String mImage;
    private String mPackageName;

    public StaticHeaderProvider(Context context) {
        mContext = context;
    }

    @Override
    public String getName() {
        return "static";
    }

    @Override
    public void settingsChanged(Uri uri) {
        final boolean customHeader = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0,
                UserHandle.USER_CURRENT) == 1;
        String imageUrl = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_IMAGE,
                UserHandle.USER_CURRENT);

        if (imageUrl != null && customHeader) {
            int idx = imageUrl.indexOf("/");
            String newPackageName = null;
            String newImage = null;

            if (idx != -1) {
                String[] parts = imageUrl.split("/");
                newPackageName = parts[0];
                newImage = parts[1];

                // Handle package name mapping
                if ("com.android.systemui.res".equals(newPackageName)) {
                    // Map to actual SystemUI package where drawables are located
                    newPackageName = "com.android.systemui";
                } else {
                    // For any other package, use as-is
                    newPackageName = "com.android.systemui";
                }
            } else {
                // Handle case where imageUrl doesn't have "/" separator
                // Try to parse as just image name (assume com.android.systemui package)
                if (imageUrl.startsWith("qs_header_image_")) {
                    newPackageName = "com.android.systemui";
                    newImage = imageUrl;
                } else if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                    // Handle file picker URIs - these should be handled by FileHeaderProvider
                    Log.d(TAG, "File URI detected, should be handled by FileHeaderProvider: " + imageUrl);
                    return;
                }
            }
            
            // Always reload if image changed or if resources aren't loaded
            if (newImage != null) {
                boolean imageChanged = !newImage.equals(mImage);
                boolean packageChanged = !newPackageName.equals(mPackageName);
                boolean needsReload = imageChanged || packageChanged || mRes == null;

                if (needsReload) {
                    mPackageName = newPackageName;
                    mImage = newImage;
                    loadHeaderImage();

                    // Try to load the drawable immediately and notify
                    Drawable drawable = getCurrent(Calendar.getInstance());
                    if (drawable != null) {
                        Log.i(TAG, "Image loaded successfully: " + mImage + " from package: " + mPackageName);
                        // Notify StatusBarHeaderMachine that we have a new image
                        // This should trigger the UI update
                    } else {
                        Log.w(TAG, "Failed to load drawable for: " + mImage + " from package: " + mPackageName);
                    }

                    Log.i(TAG, "Image changed to: " + mImage + " from package: " + mPackageName +
                          " (changed: " + imageChanged + ", packageChanged: " + packageChanged + ")");
                }
            } else if (newImage == null && mImage != null) {
                // Image was cleared
                mRes = null;
                mImage = null;
                mPackageName = null;
                Log.i(TAG, "Image cleared");
            }
        } else {
            // Reset when disabled
            mRes = null;
            mImage = null;
            mPackageName = null;
        }
    }

    @Override
    public void enableProvider() {
        // Reload settings when provider is enabled
        settingsChanged(null);
        // Force reload resources even if already loaded
        if (mImage != null && mPackageName != null) {
            loadHeaderImage();
            Log.i(TAG, "Provider enabled, loaded image: " + mImage + " from package: " + mPackageName);
        } else {
            Log.w(TAG, "Provider enabled but no image set");
        }
    }

    @Override
    public void disableProvider() {
    }

    private void loadHeaderImage() {
        Log.i(TAG, "Loading header image: " + mImage + " from package: " + mPackageName);
        mRes = loadResourcesForPackage(mPackageName);
        if (mRes == null && "com.android.systemui".equals(mPackageName)) {
            Log.d(TAG, "Failed to load from com.android.systemui, trying com.android.systemui.res");
            Resources alt = loadResourcesForPackage("com.android.systemui.res");
            if (alt != null) {
                mPackageName = "com.android.systemui.res";
                mRes = alt;
                Log.i(TAG, "Successfully loaded from com.android.systemui.res");
            }
        }
        if (mRes == null && "com.android.systemui.res".equals(mPackageName)) {
            Log.d(TAG, "Failed to load from com.android.systemui.res, trying com.android.systemui");
            Resources base = loadResourcesForPackage("com.android.systemui");
            if (base != null) {
                mPackageName = "com.android.systemui";
                mRes = base;
                Log.i(TAG, "Successfully loaded from com.android.systemui");
            }
        }
        if (mRes == null) {
            Log.e(TAG, "Failed to load resources for package: " + mPackageName);
        } else {
            // Verify the drawable exists
            int resId = mRes.getIdentifier(mImage, "drawable", mPackageName);
            if (resId == 0) {
                Log.w(TAG, "Drawable " + mImage + " not found in " + mPackageName);
            } else {
                Log.i(TAG, "Successfully found drawable " + mImage + " (resId: " + resId + ")");
            }
        }
    }

    private Resources loadResourcesForPackage(String pkg) {
        try {
            PackageManager packageManager = mContext.getPackageManager();
            return packageManager.getResourcesForApplication(pkg);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load icon pack " + pkg, e);
            return null;
        }
    }

    private boolean isValidDrawable(Drawable drawable) {
        if (drawable == null) {
            Log.w(TAG, "Drawable is null - invalid for header");
            return false;
        }

        // Check for ColorDrawable that's solid black/transparent
        if (drawable instanceof android.graphics.drawable.ColorDrawable) {
            android.graphics.drawable.ColorDrawable colorDrawable = (android.graphics.drawable.ColorDrawable) drawable;
            int color = colorDrawable.getColor();
            // Only reject if it's a solid color drawable that's black
            if (color == android.graphics.Color.BLACK) {
                Log.w(TAG, "ColorDrawable is solid black - may not be visible on dark themes");
                // Don't reject, just warn - let crDroid-style headers work
            }
        }
        // Check for BitmapDrawable with null bitmap
        else if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
            android.graphics.drawable.BitmapDrawable bitmapDrawable = (android.graphics.drawable.BitmapDrawable) drawable;
            android.graphics.Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap == null) {
                Log.w(TAG, "BitmapDrawable has null bitmap - invalid for header");
                return false;
            }
            // Don't do pixel-level validation as it rejects valid headers
            Log.d(TAG, "Valid BitmapDrawable: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        }

        return true;
    }

    private Drawable tryAlternativeFormats(String baseName, String packageName) {
        // Try different drawable names that might exist
        String[] alternatives = {
            baseName, // Already tried this
            baseName + "_alt",
            baseName.replace("qs_header_image_", "qs_header_"),
            // Try with different extensions conceptually (though resource system handles this)
        };

        for (String altName : alternatives) {
            if (altName.equals(baseName)) continue; // Skip the one we already tried

            int altResId = mRes.getIdentifier(altName, "drawable", packageName);
            if (altResId != 0) {
                try {
                    Drawable drawable = mRes.getDrawable(altResId, null);
                    if (drawable != null && isValidDrawable(drawable)) {
                        Log.d(TAG, "Found valid alternative drawable: " + altName + " for " + baseName);
                        return drawable;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Alternative drawable " + altName + " failed to load: " + e.getMessage());
                }
            }
        }

        // Try loading from alternative packages
        String[] altPackages = {"com.android.systemui.res", "com.android.systemui"};
        for (String altPkg : altPackages) {
            if (altPkg.equals(packageName)) continue;

            try {
                Resources altRes = loadResourcesForPackage(altPkg);
                if (altRes != null) {
                    int altResId = altRes.getIdentifier(baseName, "drawable", altPkg);
                    if (altResId != 0) {
                        try {
                            Drawable drawable = altRes.getDrawable(altResId, null);
                            if (drawable != null && isValidDrawable(drawable)) {
                                Log.d(TAG, "Found valid drawable in alternative package: " + baseName + " in " + altPkg);
                                return drawable;
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Alternative package drawable " + baseName + " in " + altPkg + " failed: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to try alternative package " + altPkg + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "No valid alternative formats found for " + baseName);
        return null;
    }

    @Override
    public Drawable getCurrent(final Calendar now) {
        if (mRes == null) {
            return null;
        }
        if (!Utils.isPackageInstalled(mContext, mPackageName)) {
            Log.w(TAG, "Header pack image " + mImage + " no longer available");
            return null;
        }
        try {
            int resId = mRes.getIdentifier(mImage, "drawable", mPackageName);
            return mRes.getDrawable(resId, null);
        } catch(Resources.NotFoundException e) {
            Log.w(TAG, "No drawable found for " + mImage + " in " + mPackageName);
        } catch(Exception e) {
            Log.e(TAG, "Exception loading drawable", e);
        }
        return null;
    }
}
