/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import static be.ugent.iii.application.FrameworkApplication.PREFERENCES_PARAM;
import static be.ugent.iii.application.FrameworkApplication.SESSION_ID_PARAM;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.controllers.PlayerController;
import be.ugent.iii.controllers.QuestionController;
import be.ugent.iii.sensors.LightSensor;
import be.ugent.iii.sensors.ProximitySensor;
import be.ugent.iii.service.FrameworkService;
import be.ugent.iii.tasks.ThroughputThread;
import be.ugent.iii.tasks.TimeOutTask;
import be.ugent.iii.youtube.R;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Thomas
 */
public abstract class FrameworkActivity extends Activity {

    private final static String TAG = "FrameworkActivity";

    protected long sessionIdentifier;
    protected static final String APP_FILE = "app_preferences";
    protected static final String FRAMEWORK_PREFERENCES = "framework_preferences";
    protected static final String DEVICE_INFO = "deviceinfo_retrieved";
    protected static final String PLAYER_INFO = "playerinfo_retrieved";
    protected static final String DYNAMIC_LIGHT = "dynamicLight";
    protected static final String STORAGE_INTERVAL = "storageInterval";

    //Sensoren
    protected LightSensor brightness;
    protected ProximitySensor proximity;
    protected SensorManager sensorManager;

    //Controllers
    protected FrameworkController networkController;
    protected PlayerController spelerController;
    protected QuestionController questionController;

    //Worker threads
    protected TimeOutTask timeout = new TimeOutTask();
    protected ThroughputThread tpThread;

    //Shared preferences
    private HashMap<String, ?> preferences;

    /**
     * Called when the activity is first created.
     *
     * @param icicle
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        PreferenceManager.setDefaultValues(this, APP_FILE, Context.MODE_PRIVATE, R.xml.app_preferences, true);
        // Controleer frameworksettings
        PreferenceManager.setDefaultValues(this, FrameworkPrefsActivity.FRAMEWORK_FILE, Context.MODE_PRIVATE, R.xml.framework_preferences, true);

        // Controleer optimizer settings:
        PreferenceManager.setDefaultValues(this, OptimizerPrefsActivity.OPTIMIZER_FILE, Context.MODE_PRIVATE, R.xml.optimizer_preferences, true);
        networkController = FrameworkController.getInstance();
        spelerController = PlayerController.getInstance();
        questionController = QuestionController.getInstance(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

    }

    @Override
    public void onResume() {
        super.onResume();
        activateSensors();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Sensormonitoring stoppen
        resetSensors();
    }

    private void resetSensors() {
        if (brightness != null) {
            sensorManager.unregisterListener(brightness);
        }
        if (proximity != null) {
            sensorManager.unregisterListener(proximity);
        }
        brightness = null;
        proximity = null;
    }

    /**
     * Activeer de gewenste sensoren, als deze aanwezig zijn
     */
    public void activateSensors() {
        resetSensors();
        if (brightness == null && proximity == null) {
            List<Sensor> sensorlist = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor s : sensorlist) {
                switch (s.getType()) {
                    case Sensor.TYPE_LIGHT:
                        SharedPreferences appPrefs = getSharedPreferences(FRAMEWORK_PREFERENCES, Context.MODE_PRIVATE);
                        if (appPrefs.getAll().get(DYNAMIC_LIGHT) == null) {
                            SharedPreferences.Editor editor = appPrefs.edit();
                            editor.putBoolean(DYNAMIC_LIGHT, false);
                            editor.commit();
                        }
                        if (appPrefs.getAll().get(DYNAMIC_LIGHT).toString().equals("true")) {
                            brightness = new LightSensor(this, sensorManager);
                        }
                        break;

                    case Sensor.TYPE_PROXIMITY:
                        proximity = new ProximitySensor(this, sensorManager);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Berekent dynamisch de timeout-waarden voor het framework, afhankelijk van
     * de netwerkkwaliteit. Deze timeout-waarden worden gebruikt om ervoor te
     * zorgen dat de speler of het mainscreen niet blijft hangen in laad-modus.
     *
     * @return timeoutValue
     */
    protected int getTimeoutValue() {
        int t;

        int quality = networkController.getNetworkQuality();
        switch (quality) {
            case 0:
                //LD
                t = 30;
                break;
            case 1:
                //MD
                t = 20;
                break;
            case 2:
                //HD
                t = 15;
                break;
            default:
                t = 30;
                break;
        }
        return t;
    }

    /**
     * Start de service met de gewenste identifier en geef de sharedpreferences
     * mee
     */
    protected void startFrameworkService() {
//        // Preferences ophalen:
//        SharedPreferences sharedPrefs = getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
//        preferences = (HashMap<String, ?>) sharedPrefs.getAll();
//        Log.v(TAG, "Aantal preferences in activity = " + preferences.size());

        // Framework starten:
        Intent serviceIntent = new Intent(this, FrameworkService.class);
//        serviceIntent.putExtra(PREFERENCES_PARAM, preferences);
        serviceIntent.setFlags(Service.START_REDELIVER_INTENT);
        // Sessie-id meegeven aan framework:
        serviceIntent.putExtra(SESSION_ID_PARAM, sessionIdentifier);
        startService(serviceIntent);
    }

    /**
     * Stop de frameworkservice
     */
    protected void stopFrameworkService() {
        // Framework stoppen:
        stopService(new Intent(this, FrameworkService.class));
    }

    /**
     * Is de service aan het draaien? Geeft true terug als deze draait, anders
     * false
     *
     * @return boolean
     */
    protected boolean serviceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FrameworkService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deze methode wordt opgeroepen na het vragen aan de gebruiker of er een
     * device-admin mag toegevoegd worden deze feature wordt gebruikt voor het
     * scherm te locken via de proximitysensor. Als er al ooit gevraagd was om
     * een device-admin toe te voegen en de gebruiker wou dit niet, wordt het
     * toch niet meer gevraagd omdat dat zo werd opgeslagen in de preferences.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //requestcode == 0 is bij proximitysensor
        SharedPreferences sharedPrefs = getSharedPreferences(FrameworkPrefsActivity.FRAMEWORK_FILE, Context.MODE_PRIVATE);
        preferences = (HashMap<String, ?>) sharedPrefs.getAll();
        if (preferences.get("deviceAdmin") != null && preferences.get("deviceAdmin").toString().equals("false")) {
            if (requestCode == 0) {
                SharedPreferences optimizerPrefs = networkController.getService().getSharedPreferences(FrameworkPrefsActivity.FRAMEWORK_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = optimizerPrefs.edit();
                editor.putBoolean("deviceAdmin", true);
                editor.commit();
                preferences = (HashMap<String, ?>) sharedPrefs.getAll();
            }
        }
    }

    /**
     * Geeft de breedte van het scherm terug. Wordt gebruikt voor de webviews.
     *
     * @return breedte van het scherm
     */
    @JavascriptInterface
    public int getwidth() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    /**
     * Geeft de hoogte van het scherm terug. Wordt gebruikt voor de webviews.
     *
     * @return hoogte van het scherm
     */
    @JavascriptInterface
    public int getheight() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    /**
     * Geef een boodschap weer die gegooid werd vanuit de webviews.
     *
     * @param text
     */
    @JavascriptInterface
    public void setToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Log een boodschap vanuit de webviews.
     *
     * @param text
     */
    @JavascriptInterface
    public void log(String text) {
        Log.d("YOUTUBESPELER", text);
    }

    /**
     * Stop de spinners en timeout tasks die gestart werden zodat webview niet
     * te lang blijft hangen in laad-modus.
     */
    @JavascriptInterface
    public abstract void stopSpinner();

}
