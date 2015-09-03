package be.ugent.iii.optimizer;

import android.content.ContentValues;
import android.util.Log;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.observer.IObservable;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.operators.IFrameworkOperator;
import be.ugent.iii.operators.NetworkGeneration;
import be.ugent.iii.operators.PhoneOperator;
import be.ugent.iii.operators.QosOperatorThread;
import be.ugent.iii.operators.WifiOperator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Klassie die dient als tussenstuk tussen de QoSOperatorThread en de videoOptimizer.
 * Deze klasse dient als observer op die operator en zal de updatewaarden
 * periodiek doorspelen naar de optimizer.
 * @author Thomas
 */
public class QosBuffer implements IObserver {

    private static final String TAG = "QosBuffer";
    private VideoOptimizer videoOptimizer;

    // Bufferinfomatie voor deze klasse:
    private boolean isWifi = false;
    private boolean phoneActive = false;
    private boolean dataAvailable = false;
    private boolean isRoaming = false;
    private double generationNumber = NetworkGeneration.UNKNOWN.number;
    private int mobileSignalLevel = 0;
    private int wifiSignalLevel = 0;
    private int linkSpeed = 0;

    public QosBuffer(VideoOptimizer videoOptimizer, ArrayList<IFrameworkOperator> operators) {
        this.videoOptimizer = videoOptimizer;

        // Registreren als luisteraar bij het framework:
        for (IObservable operator : operators) {
            if (operator instanceof PhoneOperator || operator instanceof QosOperatorThread || operator instanceof WifiOperator) {
                operator.registerObserver(this);
            }
        }
    }
    
    /**
     * Methode om informatie door te spelen naar complaints.
     * @return 
     */
    public Map<String, String> getValues(){
        Map<String,String> values = new HashMap<String, String>();
        
        values.put("wifi", ""+isWifi());
        values.put("phoneActive", ""+isPhoneActive());
        values.put("dataAvailable", ""+isDataAvailable());
        values.put("isRoaming", ""+isRoaming());
        values.put("generationNumber", ""+getGenerationNumber());
        values.put("mobileSignalLevel", ""+getMobileSignalLevel());
        values.put("wifiSignalLevel", ""+getWifiSignalLevel());
        values.put("linkSpeed", ""+getLinkSpeed());
        
        long tp = FrameworkController.getInstance().getThroughput(true);
        
        values.put("throughput", ""+tp);
        
        return values;
    }

    @Override
    public void update(ContentValues contentValues) {
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_WIFI_STATE_ACTIVE)) {
            isWifi = contentValues.getAsBoolean(ParameterDatabaseController.COLUMN_WIFI_STATE_ACTIVE);
            Log.v(TAG, "Wifi active = " + isWifi);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_PHONE_STATE_ACTIVE)) {
            phoneActive = contentValues.getAsBoolean(ParameterDatabaseController.COLUMN_PHONE_STATE_ACTIVE);
            Log.v(TAG, "Phone active = " + phoneActive);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_MOBILE_DATA_AVAILABLE)) {
            dataAvailable = contentValues.getAsBoolean(ParameterDatabaseController.COLUMN_MOBILE_DATA_AVAILABLE);
            Log.v(TAG, "Data available = " + dataAvailable);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_MOBILE_ROAMING)) {
            isRoaming = contentValues.getAsBoolean(ParameterDatabaseController.COLUMN_MOBILE_ROAMING);
            Log.v(TAG, "isRoaming = " + isRoaming);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_MOBILE_GENERATION)) {
            generationNumber = contentValues.getAsDouble(ParameterDatabaseController.COLUMN_MOBILE_GENERATION);
            Log.v(TAG, "generationNumber = " + generationNumber);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_MOBILE_SIGNAL_LEVEL)) {
            mobileSignalLevel = contentValues.getAsInteger(ParameterDatabaseController.COLUMN_MOBILE_SIGNAL_LEVEL);
            Log.v(TAG, "gsmSignalLevel = " + mobileSignalLevel);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_WIFI_SIGNAL_LEVEL)) {
            wifiSignalLevel = contentValues.getAsInteger(ParameterDatabaseController.COLUMN_WIFI_SIGNAL_LEVEL);
            Log.v(TAG, "WifiSignalLevel = " + wifiSignalLevel);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_WIFI_LINKSPEED)) {
            linkSpeed = contentValues.getAsInteger(ParameterDatabaseController.COLUMN_WIFI_LINKSPEED);
            Log.v(TAG, "WifiLinkspeed = " + linkSpeed + " Mbps");
        }

        videoOptimizer.notifyChange();
    }

    public boolean isWifi() {
        return isWifi;
    }

    public boolean isPhoneActive() {
        return phoneActive;
    }

    public boolean isDataAvailable() {
        return dataAvailable;
    }

    public boolean isRoaming() {
        return isRoaming;
    }

    public double getGenerationNumber() {
        return generationNumber;
    }

    public int getMobileSignalLevel() {
        return mobileSignalLevel;
    }

    public int getWifiSignalLevel() {
        return wifiSignalLevel;
    }

    public int getLinkSpeed() {
        return linkSpeed;
    }

}
