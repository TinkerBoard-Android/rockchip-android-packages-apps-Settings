package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class RootSettingPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnResume, OnPause,
        PreferenceControllerMixin {

    private static final String TAG = "RootSettingCtrl";
    private static final String PREFERENCE_KEY = "root_setting_switch";
    private static final String PERSIST_ROOT_ENABLE_MODE = "persist.root_enable.mode";

    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;
    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;

    private RestrictedSwitchPreference mPreference;

    public RootSettingPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
	updateState(mPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean rootSetting = (Boolean) newValue;

	SystemProperties.set("persist.root_enable.mode", rootSetting ? "true" : "false");

        return true;
    }


    @Override
    public void updateState(Preference preference) {
        String rootSettingMode = SystemProperties.get("persist.root_enable.mode", "false");

	boolean isChecked = "true".equals(rootSettingMode);
	mPreference.setChecked(isChecked);
    }

    @Override
    public void onResume() {
        if (mPreference == null) {
            return;
        }
    }

    @Override
    public void onPause() {
        if (mPreference == null) {
            return;
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set("persist.root_enable.mode", "false");
	mPreference.setChecked(false);
    }

}
