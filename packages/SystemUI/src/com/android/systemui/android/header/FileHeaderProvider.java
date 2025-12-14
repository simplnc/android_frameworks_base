/*
 *  Copyright (C) 2018 The OmniROM Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Calendar;

public class FileHeaderProvider implements
        StatusBarHeaderMachine.IStatusBarHeaderProvider {

    public static final String TAG = "FileHeaderProvider";
    private static final boolean DEBUG = false;

    private Context mContext;
    private Drawable mImage = null;
    private String mLastLoadedPath = null;
    private boolean mIsEnabled = false;

    public FileHeaderProvider(Context context) {
        mContext = context;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public void settingsChanged(Uri uri) {
        if (!mIsEnabled) {
            return;
        }
        final String newPath = getCustomHeaderPath();
        if (TextUtils.equals(newPath, mLastLoadedPath) && mImage != null) {
            return;
        }
        cleanupCurrentImage();
        mLastLoadedPath = null;
        if (isCustomHeaderEnabled()) {
            loadHeaderImage();
        }
    }
    
    private boolean isCustomHeaderEnabled() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0,
                UserHandle.USER_CURRENT) == 1;
    }
    
    private String getCustomHeaderPath() {
        return Settings.System.getStringForUser(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_FILE_HEADER_IMAGE,
                    UserHandle.USER_CURRENT);
    }

    @Override
    public void enableProvider() {
        mIsEnabled = true;
        if (isCustomHeaderEnabled()) {
            loadHeaderImage();
            startAnimationIfNeeded();
        }
    }

    @Override
    public void disableProvider() {
        mIsEnabled = false;
        stopAnimationIfNeeded();
    }

    private void loadHeaderImage() {
        if (mContext == null) return;
        String path = getCustomHeaderPath();
        if (path == null || path.isEmpty()) return;

        mLastLoadedPath = path;
        
        // Handle content:// URIs (from file picker)
        if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri uri = Uri.parse(path);
            String lowerPath = path.toLowerCase();
            boolean isAnimatedCandidate = lowerPath.endsWith(".gif") || lowerPath.endsWith(".webp");
            
            if (isAnimatedCandidate && loadAnimatedImageFromUri(uri, path)) {
                return;
            }
            loadStaticImageFromUri(uri, path);
            return;
        }
        
        // Handle file paths
        File file = new File(path);
        if (!file.exists()) {
            Log.w(TAG, "Custom header file missing: " + path);
            return;
        }

        String lowerPath = path.toLowerCase();
        boolean isAnimatedCandidate = lowerPath.endsWith(".gif") || lowerPath.endsWith(".webp");
        if (isAnimatedCandidate && loadAnimatedImage(file, path)) {
            return;
        }

        loadStaticImage(path);
    }

    @Override
    public Drawable getCurrent(final Calendar now) {
        if (mImage == null && isCustomHeaderEnabled()) {
        loadHeaderImage();
        }
        return mImage;
    }

    private boolean loadAnimatedImage(File imageFile, String path) {
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(imageFile);
            Drawable drawable = ImageDecoder.decodeDrawable(source);
            if (drawable == null) {
                Log.w(TAG, "ImageDecoder returned null for " + path);
                return false;
            }
            cleanupCurrentImage();
            mImage = drawable;
            if (drawable instanceof AnimatedImageDrawable) {
                AnimatedImageDrawable animDrawable = (AnimatedImageDrawable) drawable;
                animDrawable.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                if (mIsEnabled && !animDrawable.isRunning()) {
                    animDrawable.start();
                }
                if (DEBUG) Log.d(TAG, "Animated header applied: " + path);
            }
            return true;
        } catch (IOException | OutOfMemoryError e) {
            Log.e(TAG, "Failed to load animated header: " + path, e);
        }
        return false;
    }

    private void loadStaticImage(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            Log.w(TAG, "Invalid image bounds for " + path);
            return;
        }
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode bitmap for " + path);
                return;
            }
            cleanupCurrentImage();
            mImage = new BitmapDrawable(mContext.getResources(), bitmap);
            if (DEBUG) Log.d(TAG, "Static header applied: " + path);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OOM decoding header: " + path, e);
        }
    }

    private boolean loadAnimatedImageFromUri(Uri uri, String path) {
        try {
            ContentResolver resolver = mContext.getContentResolver();
            ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
            Drawable drawable = ImageDecoder.decodeDrawable(source);
            if (drawable == null) {
                Log.w(TAG, "ImageDecoder returned null for URI: " + path);
                return false;
            }
            cleanupCurrentImage();
            mImage = drawable;
            if (drawable instanceof AnimatedImageDrawable) {
                AnimatedImageDrawable animDrawable = (AnimatedImageDrawable) drawable;
                animDrawable.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                if (mIsEnabled && !animDrawable.isRunning()) {
                    animDrawable.start();
                }
                if (DEBUG) Log.d(TAG, "Animated header applied from URI: " + path);
            }
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: SystemUI lacks permission to access picker URI: " + path + " - This is expected for photo picker URIs", e);
            // Don't crash - just log the error. The photo picker grants temporary access that SystemUI can't use.
        } catch (IOException | OutOfMemoryError e) {
            Log.e(TAG, "Failed to load animated header from URI: " + path, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception loading animated header from URI: " + path, e);
        }
        return false;
    }

    private void loadStaticImageFromUri(Uri uri, String path) {
        try {
            ContentResolver resolver = mContext.getContentResolver();
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Log.w(TAG, "Failed to open file descriptor for URI: " + path);
                return;
            }
            FileDescriptor fd = pfd.getFileDescriptor();
            
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd, null, options);
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.w(TAG, "Invalid image bounds for URI: " + path);
                pfd.close();
                return;
            }
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
            pfd.close();
            
            if (bitmap == null) {
                Log.w(TAG, "Failed to decode bitmap from URI: " + path);
                return;
            }
            cleanupCurrentImage();
            mImage = new BitmapDrawable(mContext.getResources(), bitmap);
            if (DEBUG) Log.d(TAG, "Static header applied from URI: " + path);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: SystemUI lacks permission to access picker URI: " + path + " - This is expected for photo picker URIs", e);
            // Don't crash - just log the error. The photo picker grants temporary access that SystemUI can't use.
        } catch (IOException | OutOfMemoryError e) {
            Log.e(TAG, "Failed to load static header from URI: " + path, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception loading static header from URI: " + path, e);
        }
    }

    private void cleanupCurrentImage() {
        if (mImage instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable anim = (AnimatedImageDrawable) mImage;
            if (anim.isRunning()) {
                anim.stop();
            }
        } else if (mImage instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) mImage).getBitmap();
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        mImage = null;
    }

    private void startAnimationIfNeeded() {
        if (mImage instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable anim = (AnimatedImageDrawable) mImage;
            if (!anim.isRunning()) {
                anim.start();
            }
        }
    }

    private void stopAnimationIfNeeded() {
        if (mImage instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable anim = (AnimatedImageDrawable) mImage;
            if (anim.isRunning()) {
                anim.stop();
            }
        }
    }
}
