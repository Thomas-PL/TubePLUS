package be.ugent.iii.database;

import be.ugent.iii.application.FrameworkApplication;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Thomas
 */
public class QuestionsCommand {

    /**
     * Voeg een antwoord toe aan de lokale databank.
     * @param sid
     * @param deviceID
     * @param qid
     * @param listType
     * @param description
     * @param type
     * @param answer
     * @param method 
     */
    public void addAnswer(final long sid, final String deviceID, final int qid, final String listType, final String description, final String type, final String answer, final String method) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SQLiteDatabase db = FrameworkApplication.getWriteableQuestionsDatabase();
                // Key-values map aanmaken:
                ContentValues values = new ContentValues();
                values.put(QuestionDatabaseController.COLUMN_TIMESTAMP,""+System.currentTimeMillis());
                values.put(QuestionDatabaseController.COLUMN_DEVICE, deviceID);
                values.put(QuestionDatabaseController.COLUMN_SID, sid);
                values.put(QuestionDatabaseController.COLUMN_QUESTION_ID, qid);
                values.put(QuestionDatabaseController.COLUMN_QUESTION_LIST, listType);
                values.put(QuestionDatabaseController.COLUMN_DESCRIPTION, description);
                values.put(QuestionDatabaseController.COLUMN_TYPE, type);
                values.put(QuestionDatabaseController.COLUMN_ANSWER, answer);
                values.put(QuestionDatabaseController.COLUMN_METHOD, method);

                // Toevoegen aan database:
                db.insert(QuestionDatabaseController.ANSWERS_TABLE_NAME, null, values);
            }
        }).start();
    }

    /**
     * Optimizer preferences wegschrijven
     * @param sid
     * @param currentPreferences 
     */
    public void dumpOptimizerPreferences(final long sid, final HashMap<String, ?> currentPreferences) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SQLiteDatabase db = FrameworkApplication.getWriteableQuestionsDatabase();

                for (String key : currentPreferences.keySet()) {
                    // Key-values map aanmaken:
                    ContentValues values = new ContentValues();
                    values.put(QuestionDatabaseController.COLUMN_SID, sid);
                    values.put(QuestionDatabaseController.COLUMN_SETTING, key);
                    values.put(QuestionDatabaseController.COLUMN_VALUE, currentPreferences.get(key).toString());

                    // Toevoegen aan database:
                    db.insert(QuestionDatabaseController.PREFERENCES_TABLE_NAME, null, values);
                }
            }
        }).start();
    }

    /**
     * Voeg een optimizer-log toe aan de lokale databank
     * @param sid
     * @param message 
     */
    public void addOptimizerLog(final long sid, final String message) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SQLiteDatabase db = FrameworkApplication.getWriteableQuestionsDatabase();

                // Key-values map aanmaken:
                ContentValues values = new ContentValues();
                values.put(QuestionDatabaseController.COLUMN_SID, sid);
                values.put(QuestionDatabaseController.COLUMN_TIMESTAMP, FrameworkApplication.getTimestamp());
                values.put(QuestionDatabaseController.COLUMN_OPTIMIZER_DECISIONS, message);

                // Toevoegen aan database:
                db.insert(QuestionDatabaseController.OPTIMIZER_TABLE_NAME, null, values);
            }
        }).start();
    }

    public void removeOldSessions(int numberOfSessions, long sid) {
        new RemoveThread(numberOfSessions, sid).start();
    }

    /**
     * Verwijder lokale questions omdat deze weggeschreven zijn naar een externe databank.
     * @param ids 
     */
    public void removeQuestions(List<Integer> ids) {
        SQLiteDatabase db = FrameworkApplication.getWriteableQuestionsDatabase();
        for (int id : ids) {
            db.delete(QuestionDatabaseController.ANSWERS_TABLE_NAME, QuestionDatabaseController.COLUMN_ID + "=" + id, null);
        }
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
                SQLiteDatabase db = FrameworkApplication.getWriteableQuestionsDatabase();
                db.delete(QuestionDatabaseController.ANSWERS_TABLE_NAME, "sid <= " + thresholdValue, null);
                db.delete(QuestionDatabaseController.PREFERENCES_TABLE_NAME, "sid <= " + thresholdValue, null);
                db.delete(QuestionDatabaseController.OPTIMIZER_TABLE_NAME, "sid <= " + thresholdValue, null);
            }
        }
    }

}
