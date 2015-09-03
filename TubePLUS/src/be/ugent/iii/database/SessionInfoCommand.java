package be.ugent.iii.database;

import be.ugent.iii.application.FrameworkApplication;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;
import be.ugent.iii.activities.MainActivity;
import java.io.File;

/**
 * 
 * @author Laurenz Ovaere
 */
public class SessionInfoCommand {

	private TelephonyManager telephonyManager;

	public SessionInfoCommand(MainActivity activity) {
		telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
	}

	public long newSession() {
		SQLiteDatabase db = FrameworkApplication.getWriteableFrameworkDatabase();

		// Key-value-map aanmaken:
		ContentValues values = new ContentValues();
		values.put(ParameterDatabaseController.COLUMN_TIMESTAMP, FrameworkApplication.getTimestamp());
		values.put(ParameterDatabaseController.COLUMN_DEVICE_ID, telephonyManager.getDeviceId());
		values.put(ParameterDatabaseController.COLUMN_ANDROID_RELEASE, Build.VERSION.RELEASE);
		values.put(ParameterDatabaseController.COLUMN_ANDROID_SDK, Build.VERSION.SDK_INT);
		values.put(ParameterDatabaseController.COLUMN_INTERNAL_MEM_FREE, getAvailableInternalMemorySize());
		values.put(ParameterDatabaseController.COLUMN_SDCARD_IS_PRESENT, externalMemoryIsAvailable());
		values.put(ParameterDatabaseController.COLUMN_SDCARD_SIZE, getTotalExternalMemorySize());
		values.put(ParameterDatabaseController.COLUMN_SDCARD_FREE, getAvailableExternalMemorySize());

                long sessionId = 0;
		// Actie doorvoeren:
                if(db == null)
                    Log.e("DB", "DB IS NULL");
                else
                {
                    sessionId = db.insert(ParameterDatabaseController.SESSIONS_TABLE_NAME, null, values);
                }

		return sessionId;
	}

	public void removeOldSessions(int numberOfSessions, long sid) {
		new RemoveThread(numberOfSessions, sid).start();
	}

	private class RemoveThread extends Thread {

		private int numberOfSessions;
		private long sid;

		public RemoveThread(int numberOfSessions, long sid) {
			super();
			this.numberOfSessions = numberOfSessions;
			this.sid = sid;
		}

		@Override
		public void run() {
			long thresholdValue = sid - numberOfSessions;

			if (thresholdValue > 0) {
				// Verwijderen de oudere gegevens:
				SQLiteDatabase db = FrameworkApplication.getWriteableFrameworkDatabase();
				db.delete(ParameterDatabaseController.SESSIONS_TABLE_NAME, "_id <= " + thresholdValue, null);
				db.delete(ParameterDatabaseController.DEVICELOAD_TABLE_NAME, "sid <= " + thresholdValue, null);
				db.delete(ParameterDatabaseController.LOCATION_TABLE_NAME, "sid <= " + thresholdValue, null);
				db.delete(ParameterDatabaseController.QOS_TABLE_NAME, "sid <= " + thresholdValue, null);

			}
		}
	}

	/*
	 * E�n byte komt overeen met 1024^2 MByte, vandaar volgende
	 * omzettingsconstante:
	 */
	private static final long CONVERT_BYTE_TO_MB = 1024 * 1024;

	private long getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return (availableBlocks * blockSize) / CONVERT_BYTE_TO_MB; // omzetting naar MB!
	}

	private boolean externalMemoryIsAvailable() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	private long getTotalExternalMemorySize() {
		if (externalMemoryIsAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long totalBlocks = stat.getBlockCount();
			return (totalBlocks * blockSize) / CONVERT_BYTE_TO_MB; // omzetting naar MB!
		} else {
			return 0;
		}
	}

	private long getAvailableExternalMemorySize() {
		if (externalMemoryIsAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			return (availableBlocks * blockSize) / CONVERT_BYTE_TO_MB; // omzetting
																		// naar
																		// MB!
		} else {
			return 0;
		}
	}

}
