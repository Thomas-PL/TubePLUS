package be.ugent.iii.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Controllerklasse voor interactie met de lokale complaintdatabase
 * @author Thomas
 */
public class ComplaintDatabaseController extends SQLiteOpenHelper{
    
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "complaints.db";
    
    public static final String COMPLAINT_TABLE_NAME = "complaints";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DEVICE = "device";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_PARAMETER = "parameter";
    public static final String COLUMN_PARAMETER_VALUE = "parameter_value";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_METHOD = "method";
    
    /**
     * Constructor
     * @param context 
     */
    public ComplaintDatabaseController(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    /**
     * Geeft alle kolomnamen terug in arrayvorm, wordt gebruikt om in een cursor alle velden op te vragen.
     * @return array van kolommen
     */
    public static String[] getColumNames(){
        String[] names = new String[7];
        names[0] = COLUMN_DEVICE;
        names[1] = COLUMN_TIMESTAMP;
        names[2] = COLUMN_SESSION_ID;
        names[3] = COLUMN_PARAMETER;
        names[4] = COLUMN_PARAMETER_VALUE;
        names[5] = COLUMN_ID;
        names[6] = COLUMN_METHOD;
        return names;
    }

    /**
     * Maak de databank
     * @param db 
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + COMPLAINT_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_TIMESTAMP + " text not null, "
                + COLUMN_DEVICE + " text not null, "
                + COLUMN_SESSION_ID + " integer not null, "
                + COLUMN_PARAMETER + " text, "
                + COLUMN_PARAMETER_VALUE + " text, "
                + COLUMN_METHOD + " text"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        Log.v("Databases", DATABASE_NAME + " upgrade naar nieuwe versie!");
        db.execSQL("DROP TABLE IF EXISTS " + COMPLAINT_TABLE_NAME);
        onCreate(db);
    }    
}
