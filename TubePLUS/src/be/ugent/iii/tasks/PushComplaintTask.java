/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.tasks;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.database.ComplaintCommand;
import be.ugent.iii.database.ComplaintDatabaseController;
import static be.ugent.iii.tasks.SSHTask.user;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import java.net.UnknownHostException;
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
public class PushComplaintTask extends SSHTask {

    private ComplaintCommand command;

    @Override
    protected void onPreExecute() {
        finished = false;
        command = new ComplaintCommand();
        SQLiteDatabase database = FrameworkApplication.getReadableComplaintDatabase();
        cursor = database.query(ComplaintDatabaseController.COMPLAINT_TABLE_NAME, ComplaintDatabaseController.getColumNames(), null, null, null, null, null);
    }

    protected void executeSQL() {
        Connection con = null;
        Statement stmt = null;
        String url = "jdbc:mysql://" + rhost + ":" + lport + "/";

        List<Integer> toDeleteLocal = new ArrayList<Integer>();

        try {
            Log.v("SSHClient", "trying to connect MYSQL for COMPLAINTS...");
            Class.forName(driver);
            con = DriverManager.getConnection(url + db, user, passwd);
            Log.v("SSHClient", "Connection to MYSQL success!");
            stmt = con.createStatement();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(ComplaintDatabaseController.COLUMN_ID));
                long time = Long.parseLong(cursor.getString(cursor.getColumnIndex(ComplaintDatabaseController.COLUMN_TIMESTAMP)));
                String device = cursor.getString(cursor.getColumnIndex(ComplaintDatabaseController.COLUMN_DEVICE));
                int sessionID = cursor.getInt(cursor.getColumnIndex(ComplaintDatabaseController.COLUMN_SESSION_ID));
                String parameter = cursor.getString(cursor.getColumnIndex(ComplaintDatabaseController.COLUMN_PARAMETER));
                String parameter_value = cursor.getString(cursor.getColumnIndex(ComplaintDatabaseController.COLUMN_PARAMETER_VALUE));
                String method = cursor.getString(cursor.getColumnIndex(ComplaintDatabaseController.COLUMN_METHOD));

                Timestamp stamp = new Timestamp(time);

                String insert = "INSERT INTO `complaints` (`device`, `session_id`, `timestamp`, `parameter`, `parameter_value`, `method`) VALUES ('" + device + "', '" + sessionID + "', '" + stamp + "', '" + parameter + "', '" + parameter_value + "', '" + method + "');";
                int result = stmt.executeUpdate(insert);
                if (result > 0) {
                    toDeleteLocal.add(id);
                }
            }
        }
        catch (ClassNotFoundException e) {
            Log.e("SSHClient", "CLASS NOT FOUND when trying to push complaints");
        } catch (SQLException e) {
            Log.e("SSHClient", "SQL EXCEPTION when trying to push complaints");
        }finally {
            command.removeComplaints(toDeleteLocal);

            try {
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
        Log.v("SSHClient", "trying to connect SSH for COMPLAINTS...");
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
            Log.v("SSHClient", "CONNECTED for COMPLAINTS!");

            lport = 2222;
            rhost = "localhost";
            rport = 3306;
            int assinged_port = session.setPortForwardingL(lport, rhost, rport);
            Log.v("SSHCLIENT", "COMPLAINTS localhost:" + assinged_port + " -> " + rhost + ":" + rport);
        } catch (JSchException e) {
            Log.e("SSHClient", "JSchException " + e.getLocalizedMessage());
        }
    }
}
