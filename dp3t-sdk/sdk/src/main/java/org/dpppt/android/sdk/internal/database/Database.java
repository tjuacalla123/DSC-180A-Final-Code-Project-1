/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk.internal.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;


import java.util.ArrayList;

import java.util.List;
import org.dpppt.android.sdk.internal.BroadcastHelper;

import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.crypto.EphId;


import org.dpppt.android.sdk.internal.database.models.Handshake;
import org.dpppt.android.sdk.internal.util.DayDate;
// Database that has handshake entries
public class Database {

	private DatabaseOpenHelper databaseOpenHelper;
	private DatabaseThread databaseThread;

	public Database(@NonNull Context context) {
		databaseOpenHelper = DatabaseOpenHelper.getInstance(context);
		databaseThread = DatabaseThread.getInstance(context);
	}


	// TODO: for now, does nothing, maybe use for removing location?
	public void removeOldData() {
		databaseThread.post(() -> {
			SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
			DayDate lastDayToKeep = new DayDate().subtractDays(21);
		});
	}

	public ContentValues addHandshake(Context context, Handshake handshake) {
		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(Handshakes.EPHID, handshake.getEphId().getData());
		values.put(Handshakes.TIMESTAMP, handshake.getTimestamp());
		values.put(Handshakes.TX_POWER_LEVEL, handshake.getTxPowerLevel());
		values.put(Handshakes.RSSI, handshake.getRssi());
		values.put(Handshakes.PHY_PRIMARY, handshake.getPrimaryPhy());
		values.put(Handshakes.PHY_SECONDARY, handshake.getSecondaryPhy());
		values.put(Handshakes.TIMESTAMP_NANOS, handshake.getTimestampNanos());
		databaseThread.post(() -> {
			db.insert(Handshakes.TABLE_NAME, null, values);
			BroadcastHelper.sendUpdateBroadcast(context);
		});
		return values;
	}

	public List<Handshake> getHandshakes() {
		SQLiteDatabase db = databaseOpenHelper.getReadableDatabase();
		Cursor cursor = db.query(Handshakes.TABLE_NAME, Handshakes.PROJECTION, null, null, null, null, Handshakes.ID);
		return getHandshakesFromCursor(cursor);
	}


	public void getHandshakes(@NonNull ResultListener<List<Handshake>> resultListener) {
		databaseThread.post(new Runnable() {
			List<Handshake> handshakes = new ArrayList<>();

			@Override
			public void run() {
				handshakes = getHandshakes();
				databaseThread.onResult(() -> resultListener.onResult(handshakes));
			}
		});
	}

	private List<Handshake> getHandshakesFromCursor(Cursor cursor) {
		List<Handshake> handshakes = new ArrayList<>();
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndexOrThrow(Handshakes.ID));
			long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Handshakes.TIMESTAMP));
			EphId ephId = new EphId(cursor.getBlob(cursor.getColumnIndexOrThrow(Handshakes.EPHID)));
			int txPowerLevel = cursor.getInt(cursor.getColumnIndexOrThrow(Handshakes.TX_POWER_LEVEL));
			int rssi = cursor.getInt(cursor.getColumnIndexOrThrow(Handshakes.RSSI));
			String primaryPhy = cursor.getString(cursor.getColumnIndexOrThrow(Handshakes.PHY_PRIMARY));
			String secondaryPhy = cursor.getString(cursor.getColumnIndexOrThrow(Handshakes.PHY_SECONDARY));
			long timestampNanos = cursor.getLong(cursor.getColumnIndexOrThrow(Handshakes.TIMESTAMP_NANOS));
			Handshake handShake = new Handshake(id, timestamp, ephId, txPowerLevel, rssi, primaryPhy, secondaryPhy,
					timestampNanos);
			handshakes.add(handShake);
		}
		cursor.close();
		return handshakes;
	}

	public void recreateTables(ResultListener<Void> listener) {
		databaseThread.post(() -> {
			recreateTablesSynchronous();
			listener.onResult(null);
		});
	}

	public void recreateTablesSynchronous() {
		databaseOpenHelper.recreateTables(databaseOpenHelper.getWritableDatabase());
	}


}
