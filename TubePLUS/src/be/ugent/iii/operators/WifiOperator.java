package be.ugent.iii.operators;

import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.controllers.WifiController;
import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.database.QosInfoCommand;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.service.FrameworkService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Thomas
 */
public class WifiOperator extends BroadcastReceiver implements IFrameworkOperator {

    private HashMap<String, ?> preferences;
    private int lastRssiValue;
    private boolean wifiIsConnected;
    private IntentFilter filter;
    private WifiController wifiHelper;
    private FrameworkService frameworkService;
    private QosInfoCommand qosInfoCommand;
    private ArrayList<IObserver> observers;

    public WifiOperator(FrameworkService frameworkService, ArrayList<IObserver> observers) {
        this.frameworkService = frameworkService;
        this.observers = observers;
        this.preferences = frameworkService.getAppPreferences();
        this.filter = new IntentFilter();
        wifiHelper = WifiController.getInstance(frameworkService);
        qosInfoCommand = new QosInfoCommand();
        if (observers == null) {
            observers = new ArrayList<IObserver>();
        }
    }
    
    @Override
    public void enableOperator() {       
        // Waarden initialiseren:
        wifiIsConnected = wifiHelper.isWifi();
        lastRssiValue = wifiHelper.getWifiRssi();
        // Registreren voor broadcasts via intentfilter:;
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        frameworkService.registerReceiver(this, filter);
    }

    @Override
    public void disableOperator() {
        frameworkService.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) || action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            updateWifi(intent);
        }
    }

    private void updateWifi(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

            final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

            if (networkInfo != null && networkInfo.isConnected()) {
                wifiIsConnected = true;
                logChanges();
            } else {
                wifiIsConnected = false;
                logChanges();
            }

        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            lastRssiValue = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            logChanges();
        }
    }

    private void logChanges() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // Key-value-map aanmaken:
                ContentValues values = new ContentValues();

                values.put(ParameterDatabaseController.COLUMN_SESSION_ID, frameworkService.getSessionIdentifier());
                values.put(ParameterDatabaseController.COLUMN_TIMESTAMP, FrameworkApplication.getTimestamp());

                if (wifiIsConnected) {

                    if (preferences.get(QosOperatorThread.items[0]) != null && preferences.get(QosOperatorThread.items[0]).toString().equals("true")) {
                        values.put(ParameterDatabaseController.COLUMN_WIFI_STATE_ACTIVE, true);
                    }

                    if (preferences.get(QosOperatorThread.items[2]) != null && preferences.get(QosOperatorThread.items[2]).toString().equals("true")) {
                        int rssi = lastRssiValue;
                        int asu = (rssi + 113) / 2;
                        int signalLevel = WifiManager.calculateSignalLevel(rssi, 4);
                        int linkSpeed = wifiHelper.getWifiLinkSpeed();

                        values.put(ParameterDatabaseController.COLUMN_WIFI_SIGNAL_LEVEL, signalLevel);
                        values.put(ParameterDatabaseController.COLUMN_WIFI_RSSI, lastRssiValue);
                        values.put(ParameterDatabaseController.COLUMN_WIFI_ASU, asu);
                        values.put(ParameterDatabaseController.COLUMN_WIFI_LINKSPEED, linkSpeed);

                    }
                } else {
                    if (preferences.get(QosOperatorThread.items[0]) != null && preferences.get(QosOperatorThread.items[0]).toString().equals("true")) {
                        values.put(ParameterDatabaseController.COLUMN_WIFI_STATE_ACTIVE, false);
                    }
                }

                // Voeg logging toe aan databank:
                qosInfoCommand.addQosLog(values);
                notifyObservers(values);

            }
        }).start();
    }

    @Override
    public void registerObserver(IObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(ContentValues contentValues) {
        for (IObserver observer : observers) {
            observer.update(contentValues);
        }
    }

}
