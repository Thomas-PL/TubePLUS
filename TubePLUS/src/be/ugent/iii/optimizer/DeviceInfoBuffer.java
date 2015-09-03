package be.ugent.iii.optimizer;

import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.database.ParameterDatabaseController;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 *
 * @author Laurenz Ovaere
 */

/*
 * DeviceInfo bevat hardware-informatie over het toestel. Dit wordt slechts
 * eenmalig opgehaald en is voor de rest altijd aanwezig in de database. Deze
 * klasse implementeert dus niet de IObservable-interface.
 * 
 * Dit is enkel en alleen een buffer om niet bij elke actie opnieuw de
 * informatie uit de database te moeten halen. Door deze klasse blijft deze info
 * beschikbaar in het geheugen.
 */
public class DeviceInfoBuffer {

    private static final String TAG = "DeviceInfoBuffer";

    // Bufferinfomatie voor deze klasse:
    private int screenHeight;
    private int screenWidth;
    private int totalInternalMemorySize;
    private int totalRamMemory;

    public DeviceInfoBuffer() {
        // De gegevens worden eenmalig uit de database gehaald:
        loadDeviceInfo();
    }

    private void loadDeviceInfo() {
        SQLiteDatabase db = FrameworkApplication.getWriteableFrameworkDatabase();
        String[] columns = {ParameterDatabaseController.COLUMN_ITEM, ParameterDatabaseController.COLUMN_VALUE};
        String selection = "item=? OR item=? OR item=? OR item=?";
        String[] selectionArgs = {"ScreenHeight", "ScreenWidth", "TotalInternalMemorySize", "TotalRamMemory"};

        Cursor cursor = db.query(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, columns, selection, selectionArgs, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String item = cursor.getString(cursor.getColumnIndex(ParameterDatabaseController.COLUMN_ITEM));
                String value = cursor.getString(cursor.getColumnIndex(ParameterDatabaseController.COLUMN_VALUE));
                if (item.equals("ScreenHeight")) {
                    screenHeight = Integer.parseInt(value);
                    Log.v(TAG, "ScreenHeight = " + screenHeight);
                } else if (item.equals("ScreenWidth")) {
                    screenWidth = Integer.parseInt(value);
                    Log.v(TAG, "ScreenWidth = " + screenWidth);
                } else if (item.equals("TotalInternalMemorySize")) {
                    totalInternalMemorySize = Integer.parseInt(value);
                    Log.v(TAG, "TotalInternalMemorySize = " + totalInternalMemorySize);
                } else if (item.equals("TotalRamMemory")) {
                    totalRamMemory = Integer.parseInt(value);
                    Log.v(TAG, "TotalRamMemory = " + totalRamMemory);
                }
                cursor.moveToNext();
            }
        }

        cursor.close();
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getTotalInternalMemorySize() {
        return totalInternalMemorySize;
    }

    public int getTotalRamMemory() {
        return totalRamMemory;
    }
}
