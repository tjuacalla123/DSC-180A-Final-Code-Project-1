/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.crypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.*;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import org.dpppt.android.sdk.internal.util.DayDate;
import org.dpppt.android.sdk.internal.util.Json;



public class CryptoModule {
	// 16 bit
	public static final int EPHID_LENGTH = 16;

	public static final int NUMBER_OF_DAYS_TO_KEEP_DATA = 21;
	public static final int NUMBER_OF_DAYS_TO_KEEP_EXPOSED_DAYS = 10;
	// 96 epochs, so every 15 minutes
	private static final int NUMBER_OF_EPOCHS_PER_DAY = 24 * 4;
	public static final int MILLISECONDS_PER_EPOCH = 24 * 60 * 60 * 1000 / NUMBER_OF_EPOCHS_PER_DAY;
	private static final byte[] BROADCAST_KEY = "broadcast key".getBytes();

	private static final String KEY_SK_LIST_JSON = "SK_LIST_JSON";
	private static final String KEY_EPHIDS_TODAY_JSON = "EPHIDS_TODAY_JSON";

	private static CryptoModule instance;

	private SharedPreferences esp;

	public static CryptoModule getInstance(Context context) {
		if (instance == null) {
			instance = new CryptoModule();
			try {
				String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
				instance.esp = EncryptedSharedPreferences.create("dp3t_store",
						KEY_ALIAS,
						context,
						EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
						EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
			} catch (GeneralSecurityException | IOException ex) {
				ex.printStackTrace();
			}
		}
		return instance;
	}

	public boolean init() {
		try {
			String stringKey = esp.getString(KEY_SK_LIST_JSON, null);
			if (stringKey != null) return true; //key already exists
			SKList skList = new SKList();
			skList.add(Pair.create(new DayDate(System.currentTimeMillis()), getNewRandomKey()));
			storeSKList(skList);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}
	// secret key / seed
	public byte[] getNewRandomKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
		SecretKey secretKey = keyGenerator.generateKey();
		return secretKey.getEncoded();
	}
	// seed list for all days
	protected SKList getSKList() {
		String skListJson = esp.getString(KEY_SK_LIST_JSON, null);
		return Json.safeFromJson(skListJson, SKList.class, SKList::new);
	}

	private void storeSKList(SKList skList) {
		esp.edit().putString(KEY_SK_LIST_JSON, Json.toJson(skList)).apply();
	}

	protected byte[] getSKt1(byte[] SKt0) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] SKt1 = digest.digest(SKt0);
			return SKt1;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm must be present!");
		}
	}
	// change secret keys based on day
	private void rotateSK() {
		SKList skList = getSKList();
		DayDate nextDay = skList.get(0).first.getNextDay();
		byte[] SKt1 = getSKt1(skList.get(0).second);
		skList.add(0, Pair.create(nextDay, SKt1));
		List<Pair<DayDate, byte[]>> subList = skList.subList(0, Math.min(NUMBER_OF_DAYS_TO_KEEP_DATA, skList.size()));
		skList = new SKList();
		skList.addAll(subList);
		storeSKList(skList);
	}

	// get secret seed for day
	protected byte[] getCurrentSK(DayDate day) {
		SKList SKList = getSKList();
		while (SKList.get(0).first.isBefore(day)) {
			rotateSK();
			SKList = getSKList();
		}
		assert SKList.get(0).first.equals(day);
		return SKList.get(0).second;
	}

	protected List<EphId> createEphIds(byte[] SK , boolean shuffle) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(new byte[]{0x01, 0x02, 0x03}, "HmacSHA256"));
			mac.update(BROADCAST_KEY);
			byte[] prf = mac.doFinal();

			//generate EphIDs
			SecretKeySpec keySpec = new SecretKeySpec(prf, "AES");
			// AES
			Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
			byte[] counter = new byte[16];
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(counter));
			ArrayList<EphId> ephIds = new ArrayList<>();
			byte[] emptyArray = new byte[EPHID_LENGTH];
			for (int i = 0; i < NUMBER_OF_EPOCHS_PER_DAY; i++) {
				ephIds.add(new EphId(cipher.update(emptyArray)));
			}
			if (shuffle) {
				Collections.shuffle(ephIds, new SecureRandom());
			}
			return ephIds;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new IllegalStateException("HmacSHA256 and AES algorithms must be present!", e);
		}
	}
	// Determine which epoch to determine which of the 96 ephids to send in that day
	private static int getEpochCounter(long time) {
		DayDate day = new DayDate(time);
		return (int) (time - day.getStartOfDayTimestamp()) / MILLISECONDS_PER_EPOCH;
	}

	public long getCurrentEpochStart() {
		long now = System.currentTimeMillis();
		return getEpochStart(now);
	}

	public static long getEpochStart(long time) {
		DayDate currentDay = new DayDate(time);
		return currentDay.getStartOfDayTimestamp() + getEpochCounter(time) * MILLISECONDS_PER_EPOCH;
	}

	// all received ephids
	private EphIdsForDay getStoredEphIdsForToday() {
		String ephIdsJson = esp.getString(KEY_EPHIDS_TODAY_JSON, "null");
		return Json.safeFromJson(ephIdsJson, EphIdsForDay.class, () -> null);
	}

	private void storeEphIdsForToday(EphIdsForDay ephIdsForDay) {
		esp.edit().putString(KEY_EPHIDS_TODAY_JSON, Json.toJson(ephIdsForDay)).apply();
	}

	protected List<EphId> getEphIdsForToday(DayDate currentDay) {
		EphIdsForDay ephIdsForDay = getStoredEphIdsForToday();
		if (ephIdsForDay == null || !ephIdsForDay.dayDate.equals(currentDay)) {
			byte[] SK = getCurrentSK(currentDay);
			ephIdsForDay = new EphIdsForDay();
			ephIdsForDay.dayDate = currentDay;
			ephIdsForDay.ephIds = createEphIds(SK, true);
			storeEphIdsForToday(ephIdsForDay);
		}
		return ephIdsForDay.ephIds;
	}

	public EphId getCurrentEphId() {
		long now = System.currentTimeMillis();
		DayDate currentDay = new DayDate(now);
		return getEphIdsForToday(currentDay).get(getEpochCounter(now));
	}


	@SuppressLint("ApplySharedPref")
	public void reset() {
		try {
			esp.edit().clear().commit();
			init();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
