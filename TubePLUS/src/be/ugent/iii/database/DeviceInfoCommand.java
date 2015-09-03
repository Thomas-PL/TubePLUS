package be.ugent.iii.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import be.ugent.iii.application.FrameworkApplication;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Laurenz Ovaere
 */
public class DeviceInfoCommand {

    private static final String OS_ARCH = "os.arch";
    private Context context;

    public DeviceInfoCommand(Activity activity) {
        this.context = activity;
    }

    /**
     * Voeg standaard telefooninformatie toe aan de lokale databank
     */
    public void addDeviceinfoLog() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SQLiteDatabase db = FrameworkApplication.getWriteableFrameworkDatabase();

                ContentValues values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "InstructionSetName");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getInstructionSetName());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "CpuArchitecture");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getCpuArchitecture());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "DeviceManufacturer");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getDeviceManufacturer());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "DeviceModel");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getDeviceModel());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "DeviceBrand");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getDeviceBrand());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "ScreenHeight");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getScreenHeight());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "ScreenWidth");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getScreenWidth());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "TotalInternalMemorySize");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getTotalInternalMemorySize());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

                values = new ContentValues();
                values.put(ParameterDatabaseController.COLUMN_ITEM, "TotalRamMemory");
                values.put(ParameterDatabaseController.COLUMN_VALUE, getTotalRamMemory());
                db.insert(ParameterDatabaseController.DEVICEINFO_TABLE_NAME, null, values);

            }
        }).start();
    }

    private String getInstructionSetName() {
        return Build.CPU_ABI;
    }

    private String getCpuArchitecture() {
        return System.getProperty(OS_ARCH);
    }

    private String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    private String getDeviceModel() {
        return Build.MODEL;
    }

    private String getDeviceBrand() {
        return Build.BRAND;
    }

    private int getScreenWidth() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    private int getScreenHeight() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    /*
     *Eén KByte komt overeen met 1024 MByte, vandaar volgende
     * omzettingsconstante:
     */
    private static final long CONVERT_KB_TO_MB = 1024;

    /*
     * Geheugen-informatie wordt onder Linux bijgehouden in volgend bestand:
     */
    private static final String MEM_INFO_FILE = "/proc/meminfo";

    private long getTotalRamMemory() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(MEM_INFO_FILE));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.startsWith("MemTotal:")) {
                    break;
                }
            }
            if (str.startsWith("MemTotal:")) {
                return ramSizeToLong(str) / CONVERT_KB_TO_MB;
            } else {
                Log.v("DeviceInfoCommand", "Totale hoeveelheid RAM-geheugen kon niet worden opgehaald!");
                return -1;
            }
        } catch (IOException e) {
            Log.v("DeviceInfoCommand", e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        Log.v("DeviceInfoCommand", "Totale hoeveelheid RAM-geheugen kon niet worden opgehaald!");
        return -1;
    }

    private long ramSizeToLong(String ramSize) {
        ramSize = ramSize.substring(ramSize.indexOf(":") + 2);
        ramSize = ramSize.substring(0, ramSize.lastIndexOf(" "));
        return Long.parseLong(ramSize.trim());
    }

    /*
     * E�n byte komt overeen met 1024^2 MByte, vandaar volgende
     * omzettingsconstante:
     */
    private static final long CONVERT_BYTE_TO_MB = 1024 * 1024;

    private long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return (totalBlocks * blockSize) / CONVERT_BYTE_TO_MB; // omzetting naar
        // MB!
    }
}
