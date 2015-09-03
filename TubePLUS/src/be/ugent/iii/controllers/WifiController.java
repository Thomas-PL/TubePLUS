package be.ugent.iii.controllers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

/**
 *
 * @author Thomas
 */
public class WifiController {

    private ConnectivityManager connectivityManager;
    private Context context;
    private static WifiController instance;
    
    public static WifiController getInstance(Context c){
        if(instance == null){
            instance = new WifiController(c);
        }
        return instance;        
    }

    private WifiController(Context c) {
        this.context = c;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        checkIfWifiEnabled();
    }

    public boolean isWifi() {
        android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        } else {
            return false;
        }
    }

    // RSSI = Received signal strength indication
    public int getWifiRssi() {
        if (isWifi()) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return wifiManager.getConnectionInfo().getRssi();
        } else {
            return -1;
        }
    }

    // Linkspeed in Mbps (LINK_SPEED_UNITS)
    public int getWifiLinkSpeed() {
        if (isWifi()) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            return wifiManager.getConnectionInfo().getLinkSpeed();
        } else {
            return -1;
        }
    }

    public boolean checkIfAvailable() {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!manager.isWifiEnabled()) {
            return false;
        }
        return true;
    }

    private void checkIfWifiEnabled() {
        if (!checkIfAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if(context == null){
                Log.e("WifiController", "context is null");
            }
            builder.setMessage("Your WiFi module is disabled. Would you like to enable it ?")
                    .setCancelable(false)
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    Intent myIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                    context.startActivity(myIntent);
                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton("No",
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    dialog.dismiss();
                                }
                            })
                    .show();
        }
    }

}
