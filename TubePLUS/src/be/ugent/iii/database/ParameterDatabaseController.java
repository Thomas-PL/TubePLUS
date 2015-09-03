package be.ugent.iii.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Controller klasse voor de lokale QoS parameters
 * @author Thomas
 */
public class ParameterDatabaseController extends SQLiteOpenHelper {

    public static final String SESSIONS_TABLE_NAME = "sessions";
    public static final String DEVICELOAD_TABLE_NAME = "deviceload";
    public static final String LOCATION_TABLE_NAME = "locations";
    public static final String DEVICEINFO_TABLE_NAME = "deviceinfo";
    public static final String QOS_TABLE_NAME = "qos";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "loggings.db";

    // Sessions table:
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_DEVICE_ID = "device_id";
    public static final String COLUMN_ANDROID_RELEASE = "android_release";
    public static final String COLUMN_ANDROID_SDK = "android_sdk";
    public static final String COLUMN_INTERNAL_MEM_FREE = "internal_memory_free";
    public static final String COLUMN_SDCARD_IS_PRESENT = "sdcard_is_present";
    public static final String COLUMN_SDCARD_SIZE = "sdcard_size";
    public static final String COLUMN_SDCARD_FREE = "sdcard_free";

    // Deviceload table:	
    public static final String COLUMN_SESSION_ID = "sid";
    public static final String COLUMN_LOW_ON_MEMORY = "low_on_memory";
    public static final String COLUMN_AVAILABLE_MEMORY = "available_memory";
    public static final String COLUMN_CPU_USAGE = "cpu_usage";
    public static final String COLUMN_BATTERY_LEVEL = "battery_level";
    public static final String COLUMN_BATTERY_CHARGING = "battery_ischarging";

    // Locations table:
    public static final String COLUMN_LONGITUDE = "long";
    public static final String COLUMN_LATITUDE = "lat";
    public static final String COLUMN_SPEED = "speed";

    // Deviceinfo table:	
    public static final String COLUMN_ITEM = "item";
    public static final String COLUMN_VALUE = "value";

    // QoS table:
    public static final String COLUMN_WIFI_STATE_ACTIVE = "wifi_state_active";
    public static final String COLUMN_PHONE_STATE_ACTIVE = "phone_state_active";
    public static final String COLUMN_MOBILE_DATA_AVAILABLE = "mobile_data_available";
    // Gegevens over mobiele dataverbinding:
    public static final String COLUMN_MOBILE_ROAMING = "mobile_is_roaming";
    public static final String COLUMN_MOBILE_GENERATION = "mobile_generation";
    public static final String COLUMN_MOBILE_CONNECTION_TYPE = "mobile_connection_type";
    public static final String COLUMN_MOBILE_SIGNAL_LEVEL = "mobile_signal_level";
    public static final String COLUMN_MOBILE_ASU = "mobile_asu";
    public static final String COLUMN_MOBILE_BER = "mobile_ber"; // bit-error-rate
    // Gegevens over WiFi-verbinding:
    public static final String COLUMN_WIFI_SIGNAL_LEVEL = "wifi_signal_level";
    public static final String COLUMN_WIFI_RSSI = "wifi_rssi";
    public static final String COLUMN_WIFI_ASU = "wifi_asu";
    public static final String COLUMN_WIFI_LINKSPEED = "wifi_linkspeed";

    //Algemene netwerkinfo
    public static final String COLUMN_THROUGHPUT = "throughput";

    public ParameterDatabaseController(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + SESSIONS_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_TIMESTAMP + " datetime not null, "
                + COLUMN_DEVICE_ID + " text not null, "
                + COLUMN_ANDROID_RELEASE + " text, "
                + COLUMN_ANDROID_SDK + " integer, "
                + COLUMN_INTERNAL_MEM_FREE + " integer, "
                + COLUMN_SDCARD_IS_PRESENT + " integer, "
                + COLUMN_SDCARD_SIZE + " integer, "
                + COLUMN_SDCARD_FREE + " integer"
                + ");");

        db.execSQL("create table " + DEVICELOAD_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_TIMESTAMP + " datetime not null, "
                + COLUMN_SESSION_ID + " integer not null, "
                + COLUMN_LOW_ON_MEMORY + " integer, "
                + COLUMN_AVAILABLE_MEMORY + " integer, "
                + COLUMN_CPU_USAGE + " integer, "
                + COLUMN_BATTERY_LEVEL + " integer,"
                + COLUMN_BATTERY_CHARGING + " integer"
                + ");");

        db.execSQL("create table " + LOCATION_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_TIMESTAMP + " datetime not null, "
                + COLUMN_SESSION_ID + " integer not null, "
                + COLUMN_LONGITUDE + " real, "
                + COLUMN_LATITUDE + " real, "
                + COLUMN_SPEED + " real "
                + ");");

        db.execSQL("create table " + DEVICEINFO_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_ITEM + " text not null, "
                + COLUMN_VALUE + " text "
                + ");");

        db.execSQL("create table " + QOS_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_TIMESTAMP + " datetime not null, "
                + COLUMN_SESSION_ID + " integer not null, "
                + COLUMN_WIFI_STATE_ACTIVE + " integer, "
                + COLUMN_PHONE_STATE_ACTIVE + " integer, "
                + COLUMN_MOBILE_DATA_AVAILABLE + " integer, "
                + COLUMN_MOBILE_ROAMING + " integer, "
                + COLUMN_MOBILE_GENERATION + " real, "
                + COLUMN_MOBILE_CONNECTION_TYPE + " text, "
                + COLUMN_MOBILE_SIGNAL_LEVEL + " integer, "
                + COLUMN_MOBILE_ASU + " integer, "
                + COLUMN_MOBILE_BER + " integer, "
                + COLUMN_WIFI_SIGNAL_LEVEL + " integer, "
                + COLUMN_WIFI_RSSI + " integer, "
                + COLUMN_WIFI_ASU + " integer, "
                + COLUMN_WIFI_LINKSPEED + " integer, "
                + COLUMN_THROUGHPUT + " integer "
                + ");");

        Log.v("Databases", DATABASE_NAME + " aangemaakt!");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.v("Databases", DATABASE_NAME + " upgrade naar nieuwe versie!");
        db.execSQL("DROP TABLE IF EXISTS " + SESSIONS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DEVICELOAD_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LOCATION_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DEVICEINFO_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + QOS_TABLE_NAME);
        onCreate(db);
    }
}
