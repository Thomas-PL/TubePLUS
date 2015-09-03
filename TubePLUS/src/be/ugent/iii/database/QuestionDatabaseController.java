package be.ugent.iii.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Controller klasse voor de lokale questionsdatabase
 * @author Thomas
 */
public class QuestionDatabaseController extends SQLiteOpenHelper {

    public static final String ANSWERS_TABLE_NAME = "answers";
    public static final String PREFERENCES_TABLE_NAME = "preferences";
    public static final String OPTIMIZER_TABLE_NAME = "optimizer";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "questions.db";

    // Answers table:
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DEVICE = "device";
    public static final String COLUMN_METHOD = "method";
    public static final String COLUMN_SID = "sid";
    public static final String COLUMN_QUESTION_ID = "qid";
    public static final String COLUMN_QUESTION_LIST = "list";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_ANSWER = "answer";

    // Preferences table:
    public static final String COLUMN_SETTING = "setting";
    public static final String COLUMN_VALUE = "value";

    // Optimizer table:
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_OPTIMIZER_DECISIONS = "decisions";

    /**
     * Geeft alle kolomnamen terug in arrayvorm, wordt gebruikt om in een cursor alle velden op te vragen.
     * @return array van kolommen
     */
    public static String[] getColumNames() {
        String[] names = new String[10];
        names[0] = COLUMN_ID;  
        names[1] = COLUMN_DEVICE;
        names[2] = COLUMN_TIMESTAMP;
        names[3] = COLUMN_SID;
        names[4] = COLUMN_QUESTION_ID;
        names[5] = COLUMN_QUESTION_LIST;
        names[6] = COLUMN_DESCRIPTION;
        names[7] = COLUMN_TYPE;
        names[8] = COLUMN_ANSWER;
        names[9] = COLUMN_METHOD;
        
        return names;
    }

    /**
     * Constructor
     * @param context 
     */
    public QuestionDatabaseController(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Maak de database
     * @param db 
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + ANSWERS_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_DEVICE + " text not null, "
                + COLUMN_TIMESTAMP + " text not null, "
                + COLUMN_SID + " integer not null, "
                + COLUMN_QUESTION_ID + " integer not null, "
                + COLUMN_QUESTION_LIST + " text, "
                + COLUMN_DESCRIPTION + " text, "
                + COLUMN_TYPE + " text, "
                + COLUMN_ANSWER + " text, "
                + COLUMN_METHOD + " text"
                + ");");

        db.execSQL("create table " + PREFERENCES_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_SID + " integer not null, "
                + COLUMN_SETTING + " text, "
                + COLUMN_VALUE + " text"
                + ");");

        db.execSQL("create table " + OPTIMIZER_TABLE_NAME + " ("
                + COLUMN_ID + " integer primary key autoincrement, "
                + COLUMN_TIMESTAMP + " datetime not null, "
                + COLUMN_SID + " integer not null, "
                + COLUMN_OPTIMIZER_DECISIONS + " text"
                + ");");

        Log.v("Databases", DATABASE_NAME + " aangemaakt!");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.v("Databases", DATABASE_NAME + " upgrade naar nieuwe versie!");
        db.execSQL("DROP TABLE IF EXISTS " + ANSWERS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PREFERENCES_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OPTIMIZER_TABLE_NAME);
        onCreate(db);
    }

}
