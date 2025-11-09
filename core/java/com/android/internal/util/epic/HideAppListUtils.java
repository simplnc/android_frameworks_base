package com.android.internal.util.epic;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HideAppListUtils {
    enum Action {
        ADD,
        REMOVE,
        SET
    }

    private static boolean isBootCompleted() {
        return SystemProperties.getBoolean("sys.boot_completed", false);
    }

    public static boolean shouldHideAppList(Context context, String packageName) {
        return shouldHideAppList(context.getContentResolver(), packageName);
    }

    public static boolean shouldHideAppList(ContentResolver cr, String packageName) {
        if (cr == null || packageName == null) {
            return false;
        }
        
        // Allow app hiding to work immediately, not just after boot completion
        // This ensures apps are hidden from launcher even during boot process

        Set<String> apps = getApps(cr);
        if (apps.isEmpty()) {
            return false;
        }

        // Use case-sensitive exact match for package names
        // Package names are case-sensitive in Android
        boolean shouldHide = apps.contains(packageName);
        if (shouldHide) {
            android.util.Log.d("HideAppListUtils", "Hiding app: " + packageName + " (found in hide list)");
        }
        return shouldHide;
    }

    public static Set<String> getApps(Context context) {
        if (context == null) {
            return new HashSet<>();
        }

        return getApps(context.getContentResolver());
    }

    public static Set<String> getApps(ContentResolver cr) {
        if (cr == null) {
            return new HashSet<>();
        }

        String apps = "";
        try {
            apps = Settings.Secure.getString(cr, Settings.Secure.HIDE_APPLIST);
        } catch (IllegalStateException e) {
            android.util.Log.e("HideAppListUtils", "Failed to get HIDE_APPLIST setting", e);
            return new HashSet<>();
        }
        if (apps != null && !apps.isEmpty() && !apps.equals(",")) {
            Set<String> appSet = new HashSet<>();
            for (String pkg : apps.split(",")) {
                String trimmed = pkg != null ? pkg.trim() : "";
                if (!trimmed.isEmpty()) {
                    appSet.add(trimmed);
                }
            }
            android.util.Log.d("HideAppListUtils", "Loaded " + appSet.size() + " hidden apps from HIDE_APPLIST");
            // Debug: Log specific apps we're looking for
            String[] targetApps = {"com.sourajitk.ambient_music", "network.loki.messenger", 
                "com.aurora.store", "com.aurora.services", "org.microg.gms"};
            for (String target : targetApps) {
                if (appSet.contains(target)) {
                    android.util.Log.d("HideAppListUtils", "Found target app in hide list: " + target);
                } else {
                    android.util.Log.w("HideAppListUtils", "Target app NOT found in hide list: " + target);
                }
            }
            return appSet;
        }

        android.util.Log.w("HideAppListUtils", "HIDE_APPLIST setting is empty or invalid: " + apps);
        return new HashSet<>();
    }

    private static void putAppsForUser(
            Context context, String packageName, int userId, Action action) {
        if (context == null || userId < 0) {
            return;
        }

        final Set<String> apps = getApps(context);
        switch (action) {
            case ADD:
                apps.add(packageName);
                break;
            case REMOVE:
                apps.remove(packageName);
                break;
            case SET:
                // Don't change
                break;
        }

        Settings.Secure.putStringForUser(
                context.getContentResolver(),
                Settings.Secure.HIDE_APPLIST,
                String.join(",", apps),
                userId);
    }

    public void addApp(Context mContext, String packageName, int userId) {
        if (mContext == null || packageName == null || userId < 0) {
            return;
        }

        putAppsForUser(mContext, packageName, userId, Action.ADD);
    }

    public void removeApp(Context mContext, String packageName, int userId) {
        if (mContext == null || packageName == null || userId < 0) {
            return;
        }

        putAppsForUser(mContext, packageName, userId, Action.REMOVE);
    }

    public void setApps(Context mContext, int userId) {
        if (mContext == null || userId < 0) {
            return;
        }

        putAppsForUser(mContext, null, userId, Action.SET);
    }
}
