/*
 * Copyright (C) 2018-2024 crDroid Android Project
 * Copyright (C) 2018-2019 AICP
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

package com.android.systemui.crdroid.logo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.res.R;

import java.util.ArrayList;

public abstract class LogoImage extends ImageView implements DarkReceiver {

    private Context mContext;

    private boolean mAttached;

    private boolean mShowLogo;
    public int mLogoPosition;
    private int mLogoStyle;
    private int mTintColor = Color.WHITE;
    private SettingsObserver mSettingsObserver;

    class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor("status_bar_logo"), false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(
                    Settings.System.getUriFor("status_bar_logo_position"),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(
                    Settings.System.getUriFor("status_bar_logo_style"),
                    false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public LogoImage(Context context) {
        this(context, null);
    }

    public LogoImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogoImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    protected abstract boolean isLogoVisible();

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAttached)
            return;

        mAttached = true;

        mSettingsObserver = new SettingsObserver(new Handler());
        mSettingsObserver.observe();
        updateSettings();

        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!mAttached)
            return;

        mAttached = false;
        
        if (mSettingsObserver != null) {
            mSettingsObserver.unobserve();
            mSettingsObserver = null;
        }
        
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        mTintColor = DarkIconDispatcher.getTint(areas, this, tint);
        // Only update if logo is enabled and this instance should be visible
        if (mShowLogo && isLogoVisible()) {
            updateLogo();
        } else {
            // Hide if not visible for this position
            setImageDrawable(null);
            setVisibility(View.GONE);
        }
    }

    public void updateLogo() {
        // Only update if logo should be visible for this position
        // Check both mShowLogo and isLogoVisible() to prevent duplicates
        if (!mShowLogo) {
            setImageDrawable(null);
            setVisibility(View.GONE);
            return;
        }
        
        // Check if this logo instance should be visible for current position
        if (!isLogoVisible()) {
            setImageDrawable(null);
            setVisibility(View.GONE);
            return;
        }
        
        Drawable drawable = null;
        switch (mLogoStyle) {
            case 0:
            default:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_horizondroid_logo);
                break;
            case 1:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_android_logo);
                break;
            case 2:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_adidas);
                break;
            case 3:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_alien);
                break;
            case 4:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_apple_logo);
                break;
            case 5:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_avengers);
                break;
            case 6:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_batman);
                break;
            case 7:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_batman_tdk);
                break;
            case 8:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_beats);
                break;
            case 9:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_biohazard);
                break;
            case 10:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_blackberry);
                break;
            case 11:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_cannabis);
                break;
            case 12:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_cool);
                break;
            case 13:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_devil);
                break;
            case 14:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_fire);
                break;
            case 15:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_heart);
                break;
            case 16:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_nike);
                break;
            case 17:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_pac_man);
                break;
            case 18:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_puma);
                break;
            case 19:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_rog);
                break;
            case 20:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_spiderman);
                break;
            case 21:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_superman);
                break;
            case 22:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_windows);
                break;
            case 23:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_xbox);
                break;
            case 24:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ghost);
                break;
            case 25:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ninja);
                break;
            case 26:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_robot);
                break;
            case 27:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ironman);
                break;
            case 28:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_captain_america);
                break;
            case 29:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_flash);
                break;
            case 30:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_tux_logo);
                break;
            case 31:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_ubuntu_logo);
                break;
            case 32:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_mint_logo);
                break;
            case 33:
                // Amogus logo - using default logo as fallback since ic_amogus doesn't exist
                drawable = mContext.getResources().getDrawable(R.drawable.ic_horizondroid_logo);
                break;
            case 34:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_playstation);
                break;
            case 35:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_steam);
                break;
            case 36:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_nintendo);
                break;
            case 37:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_discord);
                break;
            case 38:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_telegram);
                break;
            case 39:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_whatsapp);
                break;
            case 40:
                // Signal messaging app
                drawable = mContext.getResources().getDrawable(R.drawable.ic_signal_messaging);
                break;
            case 41:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_reddit);
                break;
            case 42:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_netflix);
                break;
            case 43:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_spotify);
                break;
            case 44:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_youtube);
                break;
            case 45:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_twitch);
                break;
            case 46:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_samsung);
                break;
            case 47:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_oneplus);
                break;
            case 48:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_xiaomi);
                break;
            case 49:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_google);
                break;
            case 50:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_github);
                break;
            case 51:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_gitlab);
                break;
            case 52:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_fedora);
                break;
            case 53:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_debian);
                break;
            case 54:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_arch);
                break;
            case 55:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_kali);
                break;
            case 56:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_popos);
                break;
            case 57:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_deadpool);
                break;
            case 58:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_wolverine);
                break;
            case 59:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_hulk);
                break;
            case 60:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_thor);
                break;
            case 61:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_starwars);
                break;
            case 62:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_pokemon);
                break;
            case 63:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_underarmour);
                break;
            case 64:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_reebok);
                break;
            case 65:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_vans);
                break;
            case 66:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_converse);
                break;
            case 67:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_instagram);
                break;
            case 68:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_facebook);
                break;
            case 69:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_tiktok);
                break;
            case 70:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_snapchat);
                break;
            case 71:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_linkedin);
                break;
            case 72:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_pinterest);
                break;
            case 73:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_tumblr);
                break;
            case 74:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_vimeo);
                break;
            case 75:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_soundcloud);
                break;
            case 76:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_bandcamp);
                break;
            case 77:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_paypal);
                break;
            case 78:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_venmo);
                break;
            case 79:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_cash_app);
                break;
            case 80:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_robinhood);
                break;
            case 81:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_uber);
                break;
            case 82:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_lyft);
                break;
            case 83:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_airbnb);
                break;
            case 84:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_patreon);
                break;
            case 85:
                drawable = mContext.getResources().getDrawable(R.drawable.ic_kickstarter);
                break;
        }

        if (drawable != null) {
            // Don't tint brand logos that have their own colors
            boolean shouldTint = shouldTintLogo(mLogoStyle);
            if (shouldTint) {
                drawable.setTint(mTintColor);
            } else {
                // Clear any existing tint for brand logos
                drawable.setTintList(null);
            }
        }
        setImageDrawable(drawable);
        setVisibility(View.VISIBLE);
    }

    private boolean shouldTintLogo(int logoStyle) {
        // Brand logos that should keep their original colors
        switch (logoStyle) {
            case 2:     // Adidas
            case 4:     // Apple
            case 7:     // Batman TDK
            case 8:     // Beats
            case 9:     // Biohazard
            case 10:    // Blackberry
            case 16:    // Nike
            case 18:    // Puma
            case 19:    // ROG
            case 23:    // Xbox
            case 33:    // Amogus (fallback)
            case 34:    // Playstation
            case 35:    // Steam
            case 36:    // Nintendo
            case 37:    // Discord
            case 38:    // Telegram
            case 39:    // WhatsApp
            case 40:    // Signal
            case 41:    // Reddit
            case 42:    // Netflix
            case 43:    // Spotify
            case 44:    // YouTube
            case 45:    // Twitch
            case 46:    // Samsung
            case 47:    // OnePlus
            case 48:    // Xiaomi
            case 49:    // Google
            case 50:    // GitHub
            case 51:    // GitLab
            case 57:    // Deadpool
            case 58:    // Wolverine
            case 59:    // Hulk
            case 60:    // Thor
            case 61:    // Star Wars
            case 62:    // Pokemon
            case 63:    // Under Armour
            case 64:    // Reebok
            case 65:    // Vans
            case 66:    // Converse
            case 67:    // Instagram
            case 68:    // Facebook
            case 69:    // TikTok
            case 70:    // Snapchat
            case 71:    // LinkedIn
            case 72:    // Pinterest
            case 73:    // Tumblr
            case 74:    // Vimeo
            case 75:    // SoundCloud
            case 76:    // Bandcamp
            case 77:    // PayPal
            case 78:    // Venmo
            case 79:    // Cash App
            case 80:    // Robinhood
            case 81:    // Uber
            case 82:    // Lyft
            case 83:    // Airbnb
            case 84:    // Patreon
            case 85:    // Kickstarter
                return false; // Keep original colors for brand logos
            default:
                return true;  // Tint generic/simple logos
        }
    }

    public void updateSettings() {
        mShowLogo = Settings.System.getIntForUser(mContext.getContentResolver(),
                "status_bar_logo", 0, UserHandle.USER_CURRENT) != 0;
        mLogoPosition = Settings.System.getIntForUser(mContext.getContentResolver(),
                "status_bar_logo_position", 0, UserHandle.USER_CURRENT);
        mLogoStyle = Settings.System.getIntForUser(mContext.getContentResolver(),
                "status_bar_logo_style", 0, UserHandle.USER_CURRENT);

        // First check if logo is enabled at all
        if (!mShowLogo) {
            setImageDrawable(null);
            setVisibility(View.GONE);
            return;
        }

        // Then check if this specific logo instance should be visible for current position
        if (!isLogoVisible()) {
            setImageDrawable(null);
            setVisibility(View.GONE);
            return;
        }

        // Logo is enabled and this instance should be visible
        updateLogo();
        setVisibility(View.VISIBLE);
    }
}
