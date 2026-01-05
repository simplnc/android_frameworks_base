/*
 * Copyright (C) 2025 BashaMobile
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.server.location.privacy;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.location.Location;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;
import java.util.Random;

/**
 * LocationPrivacyHelper handles location privacy enforcement at the framework level.
 * This is used by LocationManagerService to mask and coarsen locations based on user privacy settings.
 */
public final class LocationPrivacyHelper {
    private static final String TAG = "LocationPrivacyHelper";
    
    // Settings keys
    private static final String KEY_LOCATION_PRECISION = "location_precision";
    private static final String KEY_LOCATION_SHARING = "location_sharing_enabled";
    private static final String KEY_BACKGROUND_ACCESS = "location_background_access_enabled";
    private static final String KEY_TIME_RESTRICTIONS = "location_time_restrictions_enabled";
    
    // Coarse location parameters - significantly reduce accuracy
    private static final float COARSE_NOISE_RADIUS_METERS = 5000.0f; // 5km noise radius
    private static final float APPROXIMATE_NOISE_RADIUS_METERS = 1000.0f; // 1km noise radius
    
    private static final Random sRandom = new Random();
    
    // Private constructor to prevent instantiation
    private LocationPrivacyHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Check if location should be masked (blocked) based on privacy settings
     */
    public static boolean shouldMaskLocation(@NonNull Context context, @NonNull String packageName, boolean isBackground) {
        ContentResolver resolver = context.getContentResolver();
        
        // Check if location sharing is disabled
        boolean sharingEnabled = Settings.Secure.getInt(resolver,
                KEY_LOCATION_SHARING, 1) == 1;
        if (!sharingEnabled) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Location masked: sharing disabled for " + packageName);
            }
            return true;
        }
        
        // Check if background access is disabled and this is a background request
        if (isBackground) {
            boolean backgroundEnabled = Settings.Secure.getInt(resolver,
                    KEY_BACKGROUND_ACCESS, 1) == 1;
            if (!backgroundEnabled) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Location masked: background access disabled for " + packageName);
                }
                return true;
            }
        }
        
        // Check time restrictions
        boolean timeRestrictionsEnabled = Settings.Secure.getInt(resolver,
                KEY_TIME_RESTRICTIONS, 0) == 1;
        if (timeRestrictionsEnabled && !isTimeAllowed(resolver)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Location masked: time restrictions active for " + packageName);
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Apply location coarsening based on precision setting
     * This significantly reduces location accuracy for privacy
     */
    @Nullable
    public static Location coarsenLocation(@NonNull Context context, @Nullable Location location) {
        if (location == null) {
            return null;
        }
        
        ContentResolver resolver = context.getContentResolver();
        String precision = Settings.Secure.getString(resolver, KEY_LOCATION_PRECISION);
        if (precision == null) {
            precision = "precise";
        }
        
        // If precise mode, don't coarsen (but still respect other privacy settings)
        if ("precise".equals(precision)) {
            return location;
        }
        
        Location coarsenedLocation = new Location(location);
        
        switch (precision) {
            case "coarse":
                // Maximum privacy: Add significant noise (5km radius)
                coarsenLocationWithNoise(coarsenedLocation, COARSE_NOISE_RADIUS_METERS);
                // Reduce accuracy dramatically
                coarsenedLocation.setAccuracy(Math.max(coarsenedLocation.getAccuracy(), 5000.0f));
                break;
                
            case "approximate":
                // Medium privacy: Add moderate noise (1km radius)
                coarsenLocationWithNoise(coarsenedLocation, APPROXIMATE_NOISE_RADIUS_METERS);
                coarsenedLocation.setAccuracy(Math.max(coarsenedLocation.getAccuracy(), 1000.0f));
                break;
        }
        
        return coarsenedLocation;
    }
    
    /**
     * Add random noise to location coordinates to reduce accuracy
     */
    private static void coarsenLocationWithNoise(Location location, float radiusMeters) {
        // Generate random angle and distance
        double angle = sRandom.nextDouble() * 2 * Math.PI;
        double distance = sRandom.nextDouble() * radiusMeters;
        
        // Calculate offset in meters
        double latOffset = distance * Math.cos(angle) / 111320.0; // meters to degrees (latitude)
        double lonOffset = distance * Math.sin(angle) / (111320.0 * Math.cos(Math.toRadians(location.getLatitude())));
        
        // Apply offset
        location.setLatitude(location.getLatitude() + latOffset);
        location.setLongitude(location.getLongitude() + lonOffset);
    }
    
    /**
     * Check if current time is within allowed time periods
     */
    private static boolean isTimeAllowed(ContentResolver resolver) {
        // Get allowed time periods from settings
        String allowedTimes = Settings.Secure.getString(resolver, "location_allowed_times");
        if (allowedTimes == null || "always".equals(allowedTimes)) {
            return true;
        }
        
        // Parse time restrictions
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        
        switch (allowedTimes) {
            case "daytime":
                // 6 AM to 10 PM
                return currentHour >= 6 && currentHour < 22;
            case "business":
                // 9 AM to 5 PM
                return currentHour >= 9 && currentHour < 17;
            case "evening":
                // 6 PM to 11 PM
                return currentHour >= 18 && currentHour < 23;
            case "custom":
                // Custom times would need additional settings
                // For now, default to daytime
                return currentHour >= 6 && currentHour < 22;
            default:
                return true;
        }
    }
    
    /**
     * Check if location services should be available based on privacy settings
     * Used by QS location tile to determine if location can be enabled
     */
    public static boolean isLocationAvailable(@NonNull Context context) {
        // Location is available if at least one privacy feature allows it
        ContentResolver resolver = context.getContentResolver();
        boolean sharingEnabled = Settings.Secure.getInt(resolver,
                KEY_LOCATION_SHARING, 1) == 1;
        return sharingEnabled;
    }
    
    /**
     * Get current location precision setting
     */
    @NonNull
    public static String getLocationPrecision(@NonNull Context context) {
        ContentResolver resolver = context.getContentResolver();
        String precision = Settings.Secure.getString(resolver, KEY_LOCATION_PRECISION);
        return precision != null ? precision : "precise";
    }
}

