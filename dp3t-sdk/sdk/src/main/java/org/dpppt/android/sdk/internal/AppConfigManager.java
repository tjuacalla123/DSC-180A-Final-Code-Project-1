/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfigManager {

	private static AppConfigManager instance;

	public static synchronized AppConfigManager getInstance(Context context) {
		if (instance == null) {
			instance = new AppConfigManager(context);
		}
		return instance;
	}

	public static final int CALIBRATION_TEST_DEVICE_NAME_LENGTH = 4;

	public static final long DEFAULT_SCAN_INTERVAL = 1 * 60 * 1000L;
	public static final long DEFAULT_SCAN_DURATION = 20 * 1000L;
	private static final BluetoothScanMode DEFAULT_BLUETOOTH_SCAN_MODE = BluetoothScanMode.SCAN_MODE_LOW_POWER;




	private static final String PREFS_NAME = "dp3t_sdk_preferences";

	private static final String PREF_ADVERTISING_ENABLED = "advertisingEnabled";
	private static final String PREF_RECEIVING_ENABLED = "receivingEnabled";
	private static final String PREF_LAST_SYNC_DATE = "lastSyncDate";

	private static final String PREF_CALIBRATION_TEST_DEVICE_NAME = "calibrationTestDeviceName";
	private static final String PREF_SCAN_INTERVAL = "scanInterval";
	private static final String PREF_SCAN_DURATION = "scanDuration";
	private static final String PREF_BLUETOOTH_SCAN_MODE = "scanMode";

	private String appId;


	private SharedPreferences sharedPrefs;

	private AppConfigManager(Context context) {
		sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}


	public void setAdvertisingEnabled(boolean enabled) {
		sharedPrefs.edit().putBoolean(PREF_ADVERTISING_ENABLED, enabled).apply();
	}

	public boolean isAdvertisingEnabled() {
		return sharedPrefs.getBoolean(PREF_ADVERTISING_ENABLED, false);
	}

	public void setReceivingEnabled(boolean enabled) {
		sharedPrefs.edit().putBoolean(PREF_RECEIVING_ENABLED, enabled).apply();
	}

	public boolean isReceivingEnabled() {
		return sharedPrefs.getBoolean(PREF_RECEIVING_ENABLED, false);
	}

	public long getLastSyncDate() {
		return sharedPrefs.getLong(PREF_LAST_SYNC_DATE, 0);
	}


	public String getCalibrationTestDeviceName() {
		return sharedPrefs.getString(PREF_CALIBRATION_TEST_DEVICE_NAME, null);
	}

	public long getScanDuration() {
		return sharedPrefs.getLong(PREF_SCAN_DURATION, DEFAULT_SCAN_DURATION);
	}

	public long getScanInterval() {
		return sharedPrefs.getLong(PREF_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL);
	}

	public BluetoothScanMode getBluetoothScanMode() {
		return BluetoothScanMode.values()[sharedPrefs.getInt(PREF_BLUETOOTH_SCAN_MODE, DEFAULT_BLUETOOTH_SCAN_MODE.ordinal())];
	}

	public void clearPreferences() {
		sharedPrefs.edit().clear().apply();
	}

}
