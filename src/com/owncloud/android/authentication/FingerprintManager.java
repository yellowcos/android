/**
 *   ownCloud Android client application
 *
 *   @author David González Verdugo
 *   Copyright (C) 2018 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.BuildConfig;
import com.owncloud.android.ui.activity.FingerprintActivity;

import java.util.HashSet;
import java.util.Set;

public class FingerprintManager {

    private static final Set<Class> sExemptOfFingerprintActivites;

    static {
        sExemptOfFingerprintActivites = new HashSet<Class>();
        sExemptOfFingerprintActivites.add(FingerprintActivity.class);
        // other activities may be exempted, if needed
    }

    private static int FINGERPRINT_TIMEOUT = 1000;
        // keeping a "low" positive value is the easiest way to prevent the pass code is requested on rotations

    public static FingerprintManager mFingerprintManagerInstance = null;

    public static FingerprintManager getFingerprintManager() {
        if (mFingerprintManagerInstance == null) {
            mFingerprintManagerInstance = new FingerprintManager();
        }
        return mFingerprintManagerInstance;
    }

    private Long mTimestamp = 0l;
    private int mVisibleActivitiesCounter = 0;

    protected FingerprintManager() {};

    public void onActivityCreated(Activity activity) {
        if (!BuildConfig.DEBUG) {
            if (fingerprintIsEnabled()) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        } // else, let it go, or taking screenshots & testing will not be possible
    }

    public void onActivityStarted(Activity activity) {

        if (!sExemptOfFingerprintActivites.contains(activity.getClass()) &&
                fingerprintShouldBeRequested()){

            Intent i = new Intent(MainApp.getAppContext(), FingerprintActivity.class);
            activity.startActivity(i);
        }

        mVisibleActivitiesCounter++;    // keep it AFTER fingerprintShouldBeRequested was checked
    }

    public void onActivityStopped(Activity activity) {
        if (mVisibleActivitiesCounter > 0) {
            mVisibleActivitiesCounter--;
        }
        setUnlockTimestamp();
        PowerManager powerMgr = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (fingerprintIsEnabled() && powerMgr != null && !powerMgr.isScreenOn()) {
            activity.moveTaskToBack(true);
        }
    }

    private void setUnlockTimestamp() {
        mTimestamp = System.currentTimeMillis();
    }

    private boolean fingerprintShouldBeRequested(){
        if ((System.currentTimeMillis() - mTimestamp) > FINGERPRINT_TIMEOUT &&
                mVisibleActivitiesCounter <= 0
                ){
            return fingerprintIsEnabled();
        }
        return false;
    }

    private boolean fingerprintIsEnabled() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
        return (appPrefs.getBoolean(FingerprintActivity.PREFERENCE_SET_FINGERPRINT, false));
    }
}
