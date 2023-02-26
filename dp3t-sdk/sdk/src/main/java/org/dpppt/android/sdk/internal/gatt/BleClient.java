package org.dpppt.android.sdk.internal.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dpppt.android.sdk.internal.AppConfigManager;
import org.dpppt.android.sdk.internal.BroadcastHelper;
import org.dpppt.android.sdk.internal.crypto.EphId;

import org.dpppt.android.sdk.internal.database.Database;
import org.dpppt.android.sdk.internal.database.models.Handshake;
import org.dpppt.android.sdk.internal.logger.Logger;

import org.json.JSONArray;

import org.json.JSONObject;

import static org.dpppt.android.sdk.internal.gatt.BleServer.SERVICE_UUID;


public class BleClient {

	private static final String TAG = "BleClient";

	private final Context context;
	private BluetoothLeScanner bleScanner;
	private ScanCallback bleScanCallback;

	private HashMap<String, List<Handshake>> scanResultMap = new HashMap<>();
	private ArrayList<byte[]> diff_location_ephid = new ArrayList<>();

	// constructor
	public BleClient(Context context) {this.context = context;}

	public BluetoothState start() {
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// check for bluetooth
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			BroadcastHelper.sendErrorUpdateBroadcast(context);
			return bluetoothAdapter == null ? BluetoothState.NOT_SUPPORTED : BluetoothState.DISABLED;
		}
		// Bluetooth low energy
		bleScanner = bluetoothAdapter.getBluetoothLeScanner();
		if (bleScanner == null) {
			return BluetoothState.NOT_SUPPORTED;
		}

		// uuid scanfilter
		List<ScanFilter> scanFilters = new ArrayList<>();
		scanFilters.add(new ScanFilter.Builder()
				.setServiceUuid(new ParcelUuid(SERVICE_UUID))
				.build());

		// Scan for Apple devices as iOS does not advertise service uuid when in background,
		// but instead pushes it to the "overflow" area (manufacturer data).
		// 2 scan filters: UUID and Apple manufacturing ID
		scanFilters.add(new ScanFilter.Builder()
				.setManufacturerData(0x004c, new byte[0])
				.build());

		// Settings for bluetooth scan
		ScanSettings.Builder settingsBuilder = new ScanSettings.Builder()
				.setScanMode(AppConfigManager.getInstance(context).getBluetoothScanMode().getSystemValue())
				.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
				.setReportDelay(0)
				.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
				.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			settingsBuilder
					.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
					.setLegacy(true);
		}
		ScanSettings scanSettings = settingsBuilder.build();

		BluetoothServiceStatus bluetoothServiceStatus = BluetoothServiceStatus.getInstance(context);

		// Callback function will retrieve data
		bleScanCallback = new ScanCallback() {
			private static final String TAG = "ScanCallback";

			public void onScanResult(int callbackType, ScanResult result) {
				bluetoothServiceStatus.updateScanStatus(BluetoothServiceStatus.SCAN_OK);
				if (result.getScanRecord() != null) {
					onDeviceFound(result);
				}
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				bluetoothServiceStatus.updateScanStatus(BluetoothServiceStatus.SCAN_OK);
				Logger.d(TAG, "Batch size " + results.size());
				for (ScanResult result : results) {
					onScanResult(0, result);
				}
			}

			public void onScanFailed(int errorCode) {
				bluetoothServiceStatus.updateScanStatus(errorCode);
				Logger.e(TAG, "error: " + errorCode);
			}
		};

		bleScanner.startScan(scanFilters, scanSettings, bleScanCallback);
		Logger.i(TAG, "started BLE scanner, scanMode: " + scanSettings.getScanMode() + " scanFilters: " + scanFilters.size());

		return BluetoothState.ENABLED;
	}

	private void onDeviceFound(ScanResult scanResult) {
		try {
			BluetoothDevice bluetoothDevice = scanResult.getDevice();
			final String deviceAddr = bluetoothDevice.getAddress();

			int power = scanResult.getScanRecord().getTxPowerLevel();
			if (power == Integer.MIN_VALUE) {
				Logger.d(TAG, "No power levels found for " + deviceAddr + ", use default of 12dbm");
				power = 12;
			}

			List<Handshake> handshakesForDevice = scanResultMap.get(deviceAddr);
			if (handshakesForDevice == null) {
				handshakesForDevice = new ArrayList<>();
				scanResultMap.put(deviceAddr, handshakesForDevice);
			}
			// get the EphId payload

			byte[] payload = scanResult.getScanRecord().getServiceData(new ParcelUuid(SERVICE_UUID));
			boolean correctPayload = payload != null;
			Logger.d(TAG, "found " + deviceAddr + "; power: " + power + "; rssi: " + scanResult.getRssi() +
					"; haspayload: " + correctPayload);
			if (payload != null) {

				byte[] ephID = Arrays.copyOfRange(payload, 5, payload.length);
				byte[] zip_byte = Arrays.copyOfRange(payload, 0, 5);
				String zip = new String(zip_byte, "UTF-8");
				String name = new String(ephID, "UTF-8");

				System.out.println(zip);
				System.out.println(name);

				AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
				String zipcoders = appConfigManager.zippy;


				System.out.println(diff_location_ephid.size());

				if (zipcoders != zip) {

				}
				// no duplicate ids in list
				Boolean notInLst = true;
				for (byte[] i : diff_location_ephid) {
					String id = new String(i, "UTF-8");
					System.out.println(id + " " + name + " lol");
					System.out.println(id.equals(name));
					if (id.equals(name)) {
						notInLst = false;
						break;
					}
				}
				if (notInLst) {diff_location_ephid.add(ephID);}

				Logger.i(TAG, "handshake with " + deviceAddr + " (servicedata payload)");
				handshakesForDevice.add(createHandshake(new EphId(payload), scanResult, power));


			}

		} catch (Exception e) {
			Logger.e(TAG, e);
		}
	}
	// ephid is stored thorugh handshake object
	private Handshake createHandshake(EphId ephId, ScanResult scanResult, int power) {
		return new Handshake(-1, System.currentTimeMillis(), ephId, power, scanResult.getRssi(),
				BleCompat.getPrimaryPhy(scanResult), BleCompat.getSecondaryPhy(scanResult),
				scanResult.getTimestampNanos());
	}

	// stop within intervals or manual
	public synchronized void stopScan() {
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
			bleScanner = null;
			BroadcastHelper.sendErrorUpdateBroadcast(context);
			return;
		}
		if (bleScanner != null) {
			Logger.i(TAG, "stopping BLE scanner");
			bleScanner.stopScan(bleScanCallback);
			bleScanner = null;
		}
	}

	public synchronized void stop() {
		stopScan();
		Database database = new Database(context);
		for (Map.Entry<String, List<Handshake>> entry : scanResultMap.entrySet()) {
			String device = entry.getKey();
			List<Handshake> handshakes = scanResultMap.get(device);
			for (Handshake handshake : handshakes) {
				if (handshake.getEphId() != null) {
					database.addHandshake(context, handshake);
				}
			}
		}





		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
					try {
						// converting data for posting
						JSONArray spotted = new JSONArray();
						for (byte[] i : diff_location_ephid) {spotted.put(i);}
						AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
						JSONObject jObj = new JSONObject();
						jObj.put("from_user", appConfigManager.name);
						jObj.put("spotted_users", spotted);
						String jsonStr = jObj.toString();
						System.out.println(jsonStr);
						// ["id1","id2"]
						// "id1, id2"
						////////////////////////////////////////////////////////////////////////////////////////////////////////////////

						// id json
						JSONObject jID = new JSONObject();
						jID.put("userID", appConfigManager.name);
						String jIDStr = jID.toString();

						////////////////////////////////////////////////////////////////////////////////////////////////////////////////

						//// Adding ID to server
						URL url_id = new URL("https://dsc180-decentralized-location.herokuapp.com/locationConsensus/users/");
						HttpURLConnection con = (HttpURLConnection) url_id.openConnection();
						con.setRequestMethod("POST");
						// JSON format data
						con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
						// Set the request body with the JSON data
						byte[] postID = jIDStr.getBytes("UTF-8");
						con.setDoOutput(true);
						OutputStream outputStream = con.getOutputStream();
						outputStream.write(postID);outputStream.flush();outputStream.close();
						// Response from the server
						BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
						String line;
						StringBuilder response = new StringBuilder();
						while ((line = reader.readLine()) != null) {response.append(line);}
						reader.close();
						Log.d("POST Response ID", response.toString());

						////////////////////////////////////////////////////////////////////////////////////////////////////////////////

						//// Adding interactions to server
						URL url = new URL("https://dsc180-decentralized-location.herokuapp.com/locationConsensus/interactions/");
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						connection.setRequestMethod("POST");
						// JSON format data
						connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
						// Set the request body with the JSON data
						//String json = "{\"from_user\":\"1\",\"spotted_users\":\"Andrew, Martin\"}";
						byte[] postData = jsonStr.getBytes("UTF-8");
						connection.setDoOutput(true);
						OutputStream outputStream1 = connection.getOutputStream();
						outputStream1.write(postData);outputStream1.flush();outputStream1.close();
						//System.out.println("code:"+connection.getResponseCode());
						// Response from the server
						BufferedReader reader1 = new BufferedReader(new InputStreamReader(connection.getInputStream()));
						String line1;
						StringBuilder response1 = new StringBuilder();
						while ((line1 = reader1.readLine()) != null) {response.append(line1);}
						reader1.close();
						Log.d("POST Response interactions", response1.toString());

					} catch (Exception e) {
						e.printStackTrace();
					}
			}
		});
		thread.start();
	}

}
