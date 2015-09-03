package be.ugent.iii.database;

import be.ugent.iii.application.FrameworkApplication;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * 
 * @author Laurenz Ovaere
 */
public class QosInfoCommand {

	public long addQosLog(ContentValues values) {
		SQLiteDatabase db = FrameworkApplication.getWriteableFrameworkDatabase();

		// Actie doorvoeren:
		long logId = db.insert(ParameterDatabaseController.QOS_TABLE_NAME, null, values);

		return logId;
	}

}
