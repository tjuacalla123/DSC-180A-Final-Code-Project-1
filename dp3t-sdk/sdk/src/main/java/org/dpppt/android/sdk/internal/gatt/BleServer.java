package org.dpppt.android.sdk.internal.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.crypto.CryptoModule;
import org.dpppt.android.sdk.internal.crypto.EphId;
import org.dpppt.android.sdk.internal.crypto.EphemeralIdGenerator;
import org.dpppt.android.sdk.internal.logger.Logger;

public class BleServer {

	private static final String TAG = "BleServer";

	private static final String DP3T_16_BIT_UUID = "FD68";

	// For scanning other bluetooth devices with same UUID
	public static final UUID SERVICE_UUID = UUID.fromString("0000" + DP3T_16_BIT_UUID + "-0000-1000-8000-00805F9B34FB");
	//public static final UUID SERVICE_UUID2 = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
	// For gatt characteristic
	public static final UUID TOTP_CHARACTERISTIC_UUID = UUID.fromString("8c8494e3-bab5-1848-40a0-1b06991c0001");

	private final Context context;

	// callback function to get advertise status
	private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartFailure(int errorCode) {
			Logger.e(TAG, "advertise onStartFailure: " + errorCode);
			BluetoothServiceStatus.getInstance(context).updateAdvertiseStatus(errorCode);
		}

		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			Logger.i(TAG, "advertise onStartSuccess: " + settingsInEffect.toString());
			BluetoothServiceStatus.getInstance(context).updateAdvertiseStatus(BluetoothServiceStatus.ADVERTISE_OK);
		}
	};
	private BluetoothAdapter mAdapter;
	private BluetoothLeAdvertiser mLeAdvertiser;

	public BleServer(Context context) {
		this.context = context;
	}

	private byte[] getAdvertiseData() {
		//CryptoModule cryptoModule = CryptoModule.getInstance(context);
		// Get current EphId based on epoch

		byte[] advertiseData = EphemeralIdGenerator.generateEphemeralId("Tim");
		//byte[] advertiseData = cryptoModule.getCurrentEphId().getData();

		return advertiseData;
	}

	public BluetoothState startAdvertising() {
		BluetoothManager mManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

		if (mManager == null || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			return BluetoothState.NOT_SUPPORTED;
		}

		// bluetooth low energy adapter
		mAdapter = mManager.getAdapter();
		mLeAdvertiser = mAdapter.getBluetoothLeAdvertiser();
		if (mLeAdvertiser == null) {
			return BluetoothState.NOT_SUPPORTED;
		}

		// How advertising will occur (We configured it to be ADVERTISE_MODE_BALANCED
		// and ADVERTISE_TX_POWER_ULTRA_LOW respectively
		AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
		settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
		settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW);
		settingBuilder.setConnectable(true);
		settingBuilder.setTimeout(0);
		AdvertiseSettings settings = settingBuilder.build();

		// Settings for advertising
		AdvertiseData.Builder advBuilder = new AdvertiseData.Builder();
		advBuilder.setIncludeTxPowerLevel(true);
		advBuilder.setIncludeDeviceName(false);
		advBuilder.addServiceUuid(new ParcelUuid(SERVICE_UUID));

		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		String zipcoders = appConfigManager.zippy;
		byte[] byte_zip = zipcoders.getBytes(StandardCharsets.UTF_8);


		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(getAdvertiseData());
			outputStream.write(byte_zip);
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] c = outputStream.toByteArray();

		// binding ephID to advertiser
		advBuilder.addServiceData(new ParcelUuid(SERVICE_UUID), c);

		mLeAdvertiser.startAdvertising(settings, advBuilder.build(), advertiseCallback);
		Logger.d(TAG, "started advertising (only advertiseData), advertiseMode " + settings.getMode() + " powerLevel " +
				settings.getTxPowerLevel());

		return BluetoothState.ENABLED;
	}

	// stop advertising activity
	public void stopAdvertising() {
		if (mLeAdvertiser != null) {
			mLeAdvertiser.stopAdvertising(advertiseCallback);
		}
	}

	// remove instance
	public void stop() {
		stopAdvertising();
		mAdapter = null;
	}

}
