/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.sdk;

import java.util.Collection;



public class TracingStatus {

	private int numberOfContacts;
	private boolean advertising;
	private boolean receiving;
	private long lastSyncDate;


	private Collection<ErrorState> errors;

	public TracingStatus(int numberOfContacts, boolean advertising, boolean receiving,
			long lastSyncDate, Collection<ErrorState> errors) {
		this.numberOfContacts = numberOfContacts;
		this.advertising = advertising;
		this.receiving = receiving;
		this.lastSyncDate = lastSyncDate;
		this.errors = errors;
	}

	public boolean isAdvertising() {
		return advertising;
	}

	public boolean isReceiving() {
		return receiving;
	}

	public Collection<ErrorState> getErrors() {
		return errors;
	}

	public enum ErrorState {
		MISSING_LOCATION_PERMISSION(R.string.dp3t_sdk_service_notification_error_location_permission),
		BLE_DISABLED(R.string.dp3t_sdk_service_notification_error_bluetooth_disabled),
		BLE_NOT_SUPPORTED(R.string.dp3t_sdk_service_notification_error_bluetooth_not_supported),
		BLE_INTERNAL_ERROR(R.string.dp3t_sdk_service_notification_error_bluetooth_internal_error),
		BLE_ADVERTISING_ERROR(R.string.dp3t_sdk_service_notification_error_bluetooth_advertising_error),
		BLE_SCANNER_ERROR(R.string.dp3t_sdk_service_notification_error_bluetooth_scanner_error),
		BATTERY_OPTIMIZER_ENABLED(R.string.dp3t_sdk_service_notification_error_battery_optimization);

		private int errorString;

		ErrorState(int errorString) {
			this.errorString = errorString;
		}

		public int getErrorString() {
			return errorString;
		}
	}

}
