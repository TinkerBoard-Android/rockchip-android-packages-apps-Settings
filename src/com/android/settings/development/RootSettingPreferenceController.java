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

	if (rootSetting){
		addRootEnableToCmdline();
	}else{
		removeRootEnableFromCmdline();
	}

        return true;
    }

    private void addRootEnableToCmdline(){
	String filePath = "/dtoverlay/cmdline.txt";
	String newEntry = "root_enable=1";
    	try{
		String content = readFile(filePath);
		StringBuilder updatedContent = new StringBuilder();

		String[] lines = content.split("\n");

		if (lines.length == 1 && lines[0].trim().equals("##### Note: Each line must be less than 160 words in txt. #####")) {
			updatedContent.append(lines[0]).append("\n").append(newEntry);
		} else {
			boolean entryExists = false;
			for (String line : lines) {
				if (line.contains(newEntry)) {
					entryExists = true;
				}
				updatedContent.append(line).append("\n");
			}
			if (!entryExists) {
				updatedContent.append(newEntry);
			}
		}
        	writeFile(filePath, updatedContent.toString().trim());
	}catch (IOException e){
		e.printStackTrace();
	}
    }

    private void removeRootEnableFromCmdline(){
	String filePath = "/dtoverlay/cmdline.txt";
	String entryToRemove = "root_enable=1";
	try{
		String content = readFile(filePath);

		content = content.replace(entryToRemove, "").trim();
		content = content.replaceAll(" +", " ");
		writeFile(filePath, content);
	}catch (IOException e){
		e.printStackTrace();
	}
    }
    private String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        return content.toString();
    }

    private void writeFile(String filePath, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(content);
        writer.close();
    }

    @Override
    public void updateState(Preference preference) {
        String rootSettingMode = SystemProperties.get("persist.root_enable.mode", "false");
	boolean isCmdlineEnabled = false;
	String cmdlineContent = readCmdlineFile("/dtoverlay/cmdline.txt");
	if (cmdlineContent != null) {
		isCmdlineEnabled = cmdlineContent.contains("root_enable=1");
	}

	boolean isChecked = "true".equals(rootSettingMode) && isCmdlineEnabled;
	mPreference.setChecked(isChecked);
    }

    private String readCmdlineFile(String filePath) {
	StringBuilder content = new StringBuilder();
	try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
		String line;
		while ((line = reader.readLine()) != null) {
			content.append(line);
		}
	}catch (IOException e) {
		e.printStackTrace();
		return null;
	}
	return content.toString();
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
