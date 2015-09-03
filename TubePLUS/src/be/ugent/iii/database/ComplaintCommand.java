package be.ugent.iii.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.telephony.TelephonyManager;
import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.controllers.PlayerController;
import be.ugent.iii.optimizer.AnalyseData;
import be.ugent.iii.optimizer.DeviceInfoBuffer;
import be.ugent.iii.optimizer.DeviceLoadBuffer;
import be.ugent.iii.optimizer.LocationBuffer;
import be.ugent.iii.optimizer.QosBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klasse die de klachten bijhoudt die de gebruiker registreerde gedurende het
 * bekijken van een filmpje.
 *
 * @author Thomas
 */
public class ComplaintCommand {

    private HashMap<String, String> parameters;
    private final FrameworkController networkController;
    private final PlayerController spelerController;
    private TelephonyManager telephonyManager;

    public ComplaintCommand() {
        //Alle parameters die weggeschreven moeten worden
        parameters = new HashMap<String, String>();
        //Controllers
        networkController = FrameworkController.getInstance();
        spelerController = PlayerController.getInstance();
        telephonyManager = (TelephonyManager) networkController.geefContext().getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Verwijder de gewenste complaints uit de lokale database
     *
     * @param ids
     */
    public void removeComplaints(List<Integer> ids) {
        SQLiteDatabase db = FrameworkApplication.getWriteableComplaintDatabase();
        for (int id : ids) {
            db.delete(ComplaintDatabaseController.COMPLAINT_TABLE_NAME, ComplaintDatabaseController.COLUMN_ID + "=" + id, null);
        }
    }

    /**
     * Voeg een complaint toe aan de lokale databank. Alle nodige parameters
     * worden opgehaald en weggeschreven.
     */
    public void addComplaint() {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = FrameworkApplication.getWriteableComplaintDatabase();

                getAllParams();

                String deviceID = telephonyManager.getDeviceId();
                String sessionID = "" + networkController.getService().getSessionIdentifier();
                String method;
                if (PlayerController.getInstance().isUsingYoutubeMethod()) {
                    method = "You";
                } else {
                    method = "me";
                }
                for (String key : parameters.keySet()) {
                    // Key-value-map aanmaken:
                    ContentValues values = new ContentValues();
                    values.put(ComplaintDatabaseController.COLUMN_TIMESTAMP, "" + System.currentTimeMillis());
                    values.put(ComplaintDatabaseController.COLUMN_DEVICE, deviceID);
                    values.put(ComplaintDatabaseController.COLUMN_SESSION_ID, sessionID);
                    values.put(ComplaintDatabaseController.COLUMN_PARAMETER, key);
                    values.put(ComplaintDatabaseController.COLUMN_PARAMETER_VALUE, parameters.get(key));
                    values.put(ComplaintDatabaseController.COLUMN_METHOD, method);

                    db.insert(ComplaintDatabaseController.COMPLAINT_TABLE_NAME, null, values);
                }
            }
        }).start();
    }

    /**
     * Vul de parameters hashmap met de nodige parameters.
     */
    private void getAllParams() {
        parameters.clear();
        //Deviceparameters
        DeviceInfoBuffer deviceInfo = networkController.getDeviceInfo();
        addToParams("screenHeight", "" + deviceInfo.getScreenHeight());
        addToParams("screenWidth", "" + deviceInfo.getScreenWidth());
        addToParams("internalMemorySize", "" + deviceInfo.getTotalInternalMemorySize());
        addToParams("RAM", "" + deviceInfo.getTotalRamMemory());
        addToParams("Android_release", Build.VERSION.RELEASE);
        addToParams("Android_SDK", "" + Build.VERSION.SDK_INT);

        //Networkparameters
        QosBuffer qos = networkController.getQosInfo();
        Map<String, String> v = qos.getValues();
        for (String key : v.keySet()) {
            addToParams(key, v.get(key));
        }

        //Locationparameters
        LocationBuffer lb = networkController.getLocationInfo();
        addToParams("speed", "" + lb.getSpeed());

        //Toestelbelasting
        DeviceLoadBuffer dl = networkController.getDeviceLoad();
        addToParams("averageCPU", "" + dl.getAverageCpuUsage());
        addToParams("isLowOnMemory", "" + dl.isLowOnMemory());

        //Speler
        addToParams("VideoQuality", spelerController.getPlayQuality());
        AnalyseData data = spelerController.getCurrentAnalyseData();
        //Kan null zijn door youtube-methode te gebruiken
        if (data != null) {
            addToParams("analyseData_mobileNetworkGeneration", "" + data.getMobileNetworkGeneration());
            addToParams("analyseData_locationSpeedExceeded", "" + data.isLocationSpeedExceeded());
        }
        
        //Indien een goede telefoon gebruikt werd zal de buffer ook opgeslaan worden
        addToParams("Bufferedpercentage", "" + spelerController.getBufferedPercentage()*100);
    }

    /**
     * Voeg een parameter toe aan de hashmap.
     *
     * @param key
     * @param value
     */
    private void addToParams(String key, String value) {
        parameters.put(key, value);
    }
}
