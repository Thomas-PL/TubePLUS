/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.tasks;

import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import com.jcraft.jsch.Session;

/**
 *
 * @author Thomas
 */
public abstract class SSHTask extends AsyncTask<Void, Void, Void> {

    protected static int lport;
    protected static String rhost;
    protected static int rport;
    protected static final String driver = "com.mysql.jdbc.Driver";
    protected static final String db = "qoelog";
    protected static final String user = "thomas";
    protected static final String passwd = "thomas";
    protected static final int port = 2222;
    protected static final String host = "wicaweb5.intec.ugent.be";
    protected Session session;
    protected Cursor cursor;
    protected boolean finished;

    public boolean isFinished() {
        return finished;
    }

    @Override
    protected Void doInBackground(Void... paramss) {
        Log.v("SSHTask", "trying to connect SSH...");
        connect();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.v("SSHTASK", "DONE PUSHING");
        session.disconnect();
        finished = true;
    }

    protected void connect() {
        connectSSH();
        executeSQL();
    }
    
    protected abstract void executeSQL();
    
    protected abstract void connectSSH();

}
