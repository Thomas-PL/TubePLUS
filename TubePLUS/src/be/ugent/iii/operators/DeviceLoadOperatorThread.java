package be.ugent.iii.operators;

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.database.DeviceLoadCommand;
import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.service.FrameworkService;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * 
 * @author Thomas
 */
public class DeviceLoadOperatorThread extends AbstractOperatorThread implements IFrameworkOperator {

	private static final String CPU_USAGE_FILE = "/proc/stat";
	private static final String[] items = { "isLowOnMemory", "getCpuUsage", "getBatteryLevel" };
	private DeviceLoadCommand deviceLoadCommand;
	private ArrayList<IObserver> observers;

        /**
         * Constructor
         * @param intervalTijd
         * @param frameworkService 
         */
	public DeviceLoadOperatorThread(int intervalTijd, FrameworkService frameworkService) {
		super(intervalTijd, frameworkService);
		deviceLoadCommand = new DeviceLoadCommand();
		observers = new ArrayList<IObserver>();
	}

        /**
         * Methode die wordt opgeroepen vanuit de run methode van de thread.
         * Haalt gegevens op in verband met toestelbelasting en laat deze weten aan de
         * geaboneerde klassen.
         */
	@Override
	protected void executeAction() {
		// Key-value-map aanmaken:
		ContentValues values = new ContentValues();

		values.put(ParameterDatabaseController.COLUMN_SESSION_ID, super.frameworkService.getSessionIdentifier());
		values.put(ParameterDatabaseController.COLUMN_TIMESTAMP, FrameworkApplication.getTimestamp());

		if (super.preferences.get(items[0]) != null && super.preferences.get(items[0]).toString().equals("true")) {
			boolean lowOnMemory = isLowOnMemory();
			values.put(ParameterDatabaseController.COLUMN_LOW_ON_MEMORY, lowOnMemory);
			long availableMemory = getAvailableRamMemory();
			values.put(ParameterDatabaseController.COLUMN_AVAILABLE_MEMORY, availableMemory);
		}

		if (super.preferences.get(items[1]) != null && super.preferences.get(items[1]).toString().equals("true")) {
			float cpuUsage = getCpuUsage();
			values.put(ParameterDatabaseController.COLUMN_CPU_USAGE, cpuUsage);
		}

		if (super.preferences.get(items[2]) != null && super.preferences.get(items[2]).toString().equals("true")) {
			int batteryLevel = getBatteryLevel();
			boolean isCharging = isCharging();
			values.put(ParameterDatabaseController.COLUMN_BATTERY_LEVEL, batteryLevel);
			values.put(ParameterDatabaseController.COLUMN_BATTERY_CHARGING, isCharging);
		}

		// Voeg logging toe aan databank:
		deviceLoadCommand.addDeviceloadLog(values);
		notifyObservers(values);
	}

	private float getCpuUsage() {
		try {
			RandomAccessFile reader = new RandomAccessFile(CPU_USAGE_FILE, "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");

			long idle1 = Long.parseLong(toks[5]);
			long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			try {
				Thread.sleep(360);
			} catch (Exception e) {
			}

			reader.seek(0);
			load = reader.readLine();
			reader.close();

			toks = load.split(" ");

			long idle2 = Long.parseLong(toks[5]);
			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			return (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return 0;
	}

	private int getBatteryLevel() {
		Intent batteryIntent = super.frameworkService.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		int rawlevel = batteryIntent.getIntExtra("level", -1);
		double scale = batteryIntent.getIntExtra("scale", -1);
		double level = -1;
		if (rawlevel >= 0 && scale > 0) {
			level = (rawlevel / scale) * 100;
		}
		return (int) level;
	}

	private boolean isCharging() {
		Intent batteryIntent = super.frameworkService.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
		return isCharging;
	}

	private boolean isLowOnMemory() {
		ActivityManager activityManager = (ActivityManager) super.frameworkService.getSystemService(Context.ACTIVITY_SERVICE);
		android.app.ActivityManager.MemoryInfo outInfo = new android.app.ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(outInfo);
		return outInfo.lowMemory;
	}

	/*
	 * Een byte komt overeen met 1024^2 MByte, vandaar volgende
	 * omzettingsconstante:
	 */
	private static final long CONVERT_BYTE_TO_MB = 1024 * 1024;

	private long getAvailableRamMemory() {
		ActivityManager activityManager = (ActivityManager) super.frameworkService.getSystemService(Context.ACTIVITY_SERVICE);
		android.app.ActivityManager.MemoryInfo outInfo = new android.app.ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(outInfo);
		return outInfo.availMem / CONVERT_BYTE_TO_MB;
	}

	@Override
	public void registerObserver(IObserver observer) {
		observers.add(observer);
	}

	private void notifyObservers(ContentValues contentValues) {
		for (IObserver observer : observers) {
			observer.update(contentValues);
		}
	}

}
