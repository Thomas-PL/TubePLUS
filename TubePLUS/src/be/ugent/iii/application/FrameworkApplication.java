package be.ugent.iii.application;

import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.database.QuestionDatabaseController;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import be.ugent.iii.database.ComplaintDatabaseController;
import java.io.File;

/**
 * Application class waarin alle databanken kunnen worden opgevraagd vanuit global state.
 * @author Thomas
 */
public class FrameworkApplication extends Application {
    
    public static final String PREFERENCES_PARAM = "preferences";
    public static final String SESSION_ID_PARAM = "sessionid";

    private static final String DATATIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    // Database voor loggings:
    private ParameterDatabaseController parameterController;
    private static SQLiteDatabase writeableFrameworkDatabase;
    private static SQLiteDatabase readableFrameworkDatabase;
    // Database voor vragenlijsten:
    private QuestionDatabaseController questionController;
    private static SQLiteDatabase writeableQuestionsDatabase;
    private static SQLiteDatabase readableQuestionsDatabase;
    //Database voor Complaints
    private ComplaintDatabaseController complaintController;
    private static SQLiteDatabase writeableComplaintDatabase;
    private static SQLiteDatabase readableComplaintDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        parameterController = new ParameterDatabaseController(getApplicationContext());
        writeableFrameworkDatabase = parameterController.getWritableDatabase();
        readableFrameworkDatabase = parameterController.getReadableDatabase();

        questionController = new QuestionDatabaseController(getApplicationContext());
        writeableQuestionsDatabase = questionController.getWritableDatabase();
        readableQuestionsDatabase = questionController.getReadableDatabase();

        complaintController = new ComplaintDatabaseController(getApplicationContext());
        writeableComplaintDatabase = complaintController.getWritableDatabase();
        readableComplaintDatabase = complaintController.getReadableDatabase();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (writeableFrameworkDatabase != null) {
            writeableFrameworkDatabase.close();
        }
        if (writeableQuestionsDatabase != null) {
            writeableQuestionsDatabase.close();
        }
        if (writeableComplaintDatabase != null) {
            writeableComplaintDatabase.close();
        }
    }

    public static SQLiteDatabase getWriteableFrameworkDatabase() {
        return writeableFrameworkDatabase;
    }

    public static SQLiteDatabase getReadableFrameworkDatabase() {
        return readableFrameworkDatabase;
    }

    public static SQLiteDatabase getWriteableQuestionsDatabase() {
        return writeableQuestionsDatabase;
    }

    public static SQLiteDatabase getReadableQuestionsDatabase() {
        return readableQuestionsDatabase;
    }

    public static SQLiteDatabase getWriteableComplaintDatabase() {
        return writeableComplaintDatabase;
    }

    public static SQLiteDatabase getReadableComplaintDatabase() {
        return readableComplaintDatabase;
    }

    public static String getTimestamp() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATATIME_FORMAT, Locale.FRENCH);
        return sdf.format(calendar.getTime());
    }
}
