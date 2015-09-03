package be.ugent.iii.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import be.ugent.iii.activities.PlayerActivity;
import be.ugent.iii.activities.FrameworkPrefsActivity;
import static be.ugent.iii.application.FrameworkApplication.SESSION_ID_PARAM;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.controllers.PlayerController;
import be.ugent.iii.receivers.FrameworkReceiver;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.operators.DeviceLoadOperatorThread;
import be.ugent.iii.operators.IFrameworkOperator;
import be.ugent.iii.operators.LocationOperator;
import be.ugent.iii.operators.PhoneOperator;
import be.ugent.iii.operators.QosOperatorThread;
import be.ugent.iii.operators.WifiOperator;
import be.ugent.iii.optimizer.VideoOptimizer;
import be.ugent.iii.youtube.R;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Laurenz Ovaere
 */
public class FrameworkService extends Service {

    private ArrayList<IFrameworkOperator> operators;
    private HashMap<String, ?> preferences;
    private long sessionIdentifier;

    private static final String APP_FILE = "app_preferences";
    private static final String QOS_INTERVAL = "qosInterval";
    private static final String LOCATION_INTERVAL = "gpsInterval";
    private static final String DEVICE_INTERVAL = "deviceInterval";
    private static final String QOS_ON = "registerQosUpdates";
    //public static final String QOS_POLLING = "pollingIsEnabled";
    private static final String LOCATION_ON = "registerLocationUpdates";
    private static final String DEVICE_ON = "registerLoadUpdates";

    // VideoOptimizer:
    private VideoOptimizer videoOptimizer;

    // ParameterController
    private FrameworkController parameterController;

    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        operators = new ArrayList<IFrameworkOperator>();
        Log.v("FrameworkService", "service.onCreate()");
        parameterController = FrameworkController.getInstance();
        parameterController.setService(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Preferences opslaan:
//        preferences = (HashMap<String, ?>) intent.getSerializableExtra(PREFERENCES_PARAM);
        // Sessionidentifier opslaan:
        sessionIdentifier = intent.getLongExtra(SESSION_ID_PARAM, -1);
        if (sessionIdentifier == -1) {
            Log.v("FrameworkService", "Geen sessionIdentifier ontvangen!");
        } else {
            Log.v("FrameworkService", "Service gestart met id = " + sessionIdentifier);
        }
        
        SharedPreferences sharedPrefs = getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
        preferences = (HashMap<String, ?>) sharedPrefs.getAll();

        // Intervaltijden ophalen:
        int qosInterval = Integer.parseInt(preferences.get(QOS_INTERVAL).toString());
        int locationInterval = Integer.parseInt(preferences.get(LOCATION_INTERVAL).toString());
        int deviceInterval = Integer.parseInt(preferences.get(DEVICE_INTERVAL).toString());
        Log.v("FrameworkService", "Qos Interval = " + qosInterval);
        Log.v("FrameworkService", "Location Interval = " + locationInterval);
        Log.v("FrameworkService", "Device Interval = " + deviceInterval);
        
        Log.e("service", "FIRST TIME: registerLocationUpdates is " + preferences.get(LOCATION_ON).toString());
        
//        for(String key : preferences.keySet()){
//            Log.e("service","key: " + key + ", " + preferences.get(key));
//        }
//        for(String key : preferences.keySet()){
//            Log.e("service","SECOND TIME key: " + key + ", " + preferences.get(key));
//        }
        
        // Operators aanmaken:
        if (preferences.get(QOS_ON) != null && preferences.get(QOS_ON).toString().equals("true")) {
            Log.e("service","adding QoSOperator");
            operators.add(new QosOperatorThread(qosInterval, this));
        }
        //Log.e("service", "registerLocationUpdates is " + preferences.get(LOCATION_ON).toString());
        if (preferences.get(LOCATION_ON) != null && preferences.get(LOCATION_ON).toString().equals("true")) {
            Log.e("service","adding locationoperator");
            operators.add(new LocationOperator(locationInterval, this));
        }
//        Log.e("service", "registerLoadUpdates is " + preferences.get(DEVICE_ON).toString());
        if (preferences.get(DEVICE_ON) != null && preferences.get(DEVICE_ON).toString().equals("true")) {
            Log.e("service","adding DeviceLoad");
            operators.add(new DeviceLoadOperatorThread(deviceInterval, this));
        }

        // Operators starten:
        for (IFrameworkOperator operator : operators) {
            Log.e("Service", "enabeling operator " + operator.toString());
            operator.enableOperator();
        }

        // Optimizer starten:
        videoOptimizer = new VideoOptimizer(operators, this);

        //Registreer broadcastreceiver voor bij het unpluggen van headset
        receiver = new FrameworkReceiver();
        registerReceiver(receiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_LOW));
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_OKAY));

//        return START_STICKY;
        return super.onStartCommand(intent, flags, startId);
    }

    private final IBinder mBinder = new FrameworkBinder();

    public class FrameworkBinder extends Binder {

        public VideoOptimizer getOptimizer() {
            return videoOptimizer;
        }

        public long getSessionIdentifier() {
            return sessionIdentifier;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Via deze Binder-interface kunnen cliënt
        // communiceren met deze interface!
        // (nodig om luisteraars te koppelen)
        Log.v("FrameworkService", "service.onBind()");
//        setToForeground();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("FrameworkService", "service.onUnbind()");
//        stopForeground();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("FrameworkService", "service.onDestroy()");
        // Alle operators deactiveren:
        for (IFrameworkOperator operator : operators) {
            operator.disableOperator();
        }

        try {
            //Als deze niet unregistered wordt, dan blijft de hij luisteren en zal de app crashen zelfs na het afsluiten ervan
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException ex) {
            //receiver was niet geregistreerd om de een of andere reden
        }
    }

    public HashMap<String, ?> getAppPreferences() {
        SharedPreferences sharedPrefs = getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
        preferences = (HashMap<String, ?>) sharedPrefs.getAll();
        return preferences;
    }
    
    public HashMap<String, ?> getFrameworkPreferences(){
        SharedPreferences sharedPrefs = getSharedPreferences(FrameworkPrefsActivity.FRAMEWORK_FILE, Context.MODE_PRIVATE);
        HashMap<String, ?> frameworkPreferences = (HashMap<String, ?>) sharedPrefs.getAll();
        return frameworkPreferences;
    }

    public long getSessionIdentifier() {
        return sessionIdentifier;
    }

    public void switchToQosWithoutPolling() {
        // De observers zullen toegevoegd worden aan de
        // operators die niet van polling gebruik maken.
        ArrayList<IObserver> observers = null;
        IFrameworkOperator qosWithPolling = null;
        for (IFrameworkOperator operator : operators) {
            if (operator instanceof QosOperatorThread) {
                qosWithPolling = operator;
            }
        }
        // Enkel wanneer dergelijke operator actief is:
        if (qosWithPolling != null) {
            Log.v("FrameworkService", "switching to qos without polling");
            observers = ((QosOperatorThread) qosWithPolling).getObservers();
            qosWithPolling.disableOperator();
            operators.remove(qosWithPolling);
            // Nu nieuwe operators instellen:
            WifiOperator wifiOperator = new WifiOperator(this, observers);
            PhoneOperator phoneOperator = new PhoneOperator(this, observers);
            operators.add(wifiOperator);
            operators.add(phoneOperator);
            // Operators aanzetten:
            wifiOperator.enableOperator();
            phoneOperator.enableOperator();
        }
    }

    public void setToForeground() {
        String videoName = PlayerController.getInstance().getVideoName();

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(),
                PlayerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification();
        notification.tickerText = "Playing " + videoName;
        notification.icon = R.drawable.play_icon;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(getApplicationContext(), "TubePLUS", "Playing: " + videoName, pi);

        startForeground((int) sessionIdentifier, notification);
    }

    public void stopForeground() {
        stopForeground(true);
    }
}
