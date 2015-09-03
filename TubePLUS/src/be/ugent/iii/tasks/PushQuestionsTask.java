package be.ugent.iii.tasks;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.database.QuestionDatabaseController;
import be.ugent.iii.database.QuestionsCommand;
import static be.ugent.iii.tasks.SSHTask.user;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Thomas
 */
public class PushQuestionsTask extends SSHTask {

    private QuestionsCommand command;

    @Override
    protected void onPreExecute() {
        
        finished = false;
        command = new QuestionsCommand();
        SQLiteDatabase database = FrameworkApplication.getReadableQuestionsDatabase();
        cursor = database.query(QuestionDatabaseController.ANSWERS_TABLE_NAME, QuestionDatabaseController.getColumNames(), null, null, null, null, null);
    }

    protected void executeSQL() {
        Connection con = null;
        Statement stmt = null;
        String url = "jdbc:mysql://" + rhost + ":" + lport + "/";

        List<Integer> toDeleteLocal = new ArrayList<Integer>();

        try {
            Log.v("SSHClient", "trying to connect MYSQL for QUESTIONS...");
            Class.forName(driver);
            con = DriverManager.getConnection(url + db, user, passwd);
            Log.v("SSHClient", "Connection to MYSQL success for QUESTIONS!");
            stmt = con.createStatement();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_ID));
                String deviceID = cursor.getString(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_DEVICE));
                long time = Long.parseLong(cursor.getString(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_TIMESTAMP)));
                int sid = cursor.getInt(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_SID));
                int qid = cursor.getInt(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_QUESTION_ID));
                String list = cursor.getString(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_QUESTION_LIST));
                String description = "";//cursor.getString(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_DESCRIPTION));
                String type = cursor.getString(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_TYPE));
                String answer = cursor.getString(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_ANSWER));
                String method = cursor.getString(cursor.getColumnIndex(QuestionDatabaseController.COLUMN_METHOD));

                Timestamp stamp = new Timestamp(time);

                String insert = "INSERT INTO `questions` (`timestamp`, `device`, `sid`, `qid`, `list`, `description`, `type`, `answer`, `method`) VALUES ('" + stamp + "', '" + deviceID + "', '" + sid + "', '" + qid + "', '" + list + "', '" + description + "', '" + type + "', '" + answer + "', '" + method + "');";
                int result = stmt.executeUpdate(insert);
                if (result > 0) {
                    toDeleteLocal.add(id);
                }
            }
        } catch (ClassNotFoundException e) {
            Log.e("SSHClient", "CLASS NOT FOUND when trying to push Questions");
        } catch (SQLException e) {
            Log.e("SSHClient", "SQL EXCEPTION when trying to push Questions");
        } finally {
            command.removeQuestions(toDeleteLocal);
            try {
                //finally block used to close resources
                session.delPortForwardingL(lport);
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                Log.e("SSHClient", "Error closing connection");
            } catch (JSchException ex) {
                Log.e("SSHClient", "Error removing portforwarding");
            }
        }
    }

    protected void connectSSH() {
        Log.v("SSHClient", "trying to connect SSH for QUESTIONS...");
        //try {
        JSch jsch = new JSch();
        Properties props = new Properties();
        props.put("StrictHostKeyChecking", "no");

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("compression.s2c", "zlib,none");
        config.put("compression.c2s", "zlib,none");

        try {
            session = jsch.getSession(user, host, port);
            session.setConfig(config);
            session.setPassword(passwd);
            session.connect();
            Log.v("SSHClient", "CONNECTED for QUESTIONS!");

            lport = 4321;
            rhost = "localhost";
            rport = 3306;
            int assinged_port = session.setPortForwardingL(lport, rhost, rport);
            Log.v("SSHCLIENT", "QUESTIONS localhost:" + assinged_port + " -> " + rhost + ":" + rport);
        } catch (JSchException e) {
            Log.e("SSHClient", "JSchException " + e.getLocalizedMessage());
        }
    }
}
