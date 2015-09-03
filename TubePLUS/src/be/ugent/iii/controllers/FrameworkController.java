package be.ugent.iii.controllers;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import be.ugent.iii.activities.FrameworkActivity;
import be.ugent.iii.operators.LocationOperator;
import be.ugent.iii.optimizer.DeviceInfoBuffer;
import be.ugent.iii.optimizer.DeviceLoadBuffer;
import be.ugent.iii.optimizer.LocationBuffer;
import be.ugent.iii.optimizer.QosBuffer;
import be.ugent.iii.optimizer.VideoQuality;
import be.ugent.iii.service.FrameworkService;
import be.ugent.iii.tasks.CalculateThroughputTask;

/**
 * Algemene singleton klasse met informatie over het framework.
 *
 * @author Thomas
 */
public class FrameworkController {
    
    private long throughput;
    private static FrameworkController instance;
    private FrameworkActivity context;
    private LocationOperator lo;
    private WebView homeView;
    private FrameworkService service;
    private int networkQuality;    
    private DeviceLoadBuffer deviceLoad;
    private LocationBuffer locationInfo;
    private QosBuffer qosInfo;
    private DeviceInfoBuffer deviceInfo;

    /**
     * Singleton pattern
     *
     * @return controller
     */
    public static FrameworkController getInstance() {
        if (instance == null) {
            instance = new FrameworkController();
        }
        return instance;
        
    }

    /**
     * Private constructor
     */
    private FrameworkController() {
        throughput = 0;
        networkQuality = 0;
    }

    /**
     * Geeft de loadbuffer terug die informatie bevat over de belasting van het
     * toestel.
     *
     * @return DeviceLoadBuffer
     */
    public DeviceLoadBuffer getDeviceLoad() {
        return deviceLoad;
    }

    /**
     * Zet de loadbuffer die informatie bevat over de belasting van het toestel.
     *
     * @param deviceLoad
     */
    public void setDeviceLoad(DeviceLoadBuffer deviceLoad) {
        this.deviceLoad = deviceLoad;
    }

    /**
     * Geeft de locationbuffer terug die informatie bevat over locatiebepaling
     * van het toestel.
     *
     * @return LocationBuffer
     */
    public LocationBuffer getLocationInfo() {
        return locationInfo;
    }

    /**
     * Zet de locationbuffer die informatie bevat over locatiebepaling van het
     * toestel.
     *
     * @param locationInfo
     */
    public void setLocationInfo(LocationBuffer locationInfo) {
        this.locationInfo = locationInfo;
    }

    /**
     * Geeft de qosbuffer terug die informatie bevat over de quality of service
     * parameters van het toestel.
     *
     * @return QosBuffer
     */
    public QosBuffer getQosInfo() {
        return qosInfo;
    }

    /**
     * Zet de qosbuffer die informatie bevat over de quality of service
     * parameters van het toestel.
     *
     * @param qosInfo
     */
    public void setQosInfo(QosBuffer qosInfo) {
        this.qosInfo = qosInfo;
    }

    /**
     * Geeft de DeviceInfoBuffer terug die algemene informatie bevat over het
     * toestel.
     *
     * @return DeviceInfoBuffer
     */
    public DeviceInfoBuffer getDeviceInfo() {
        return deviceInfo;
    }

    /**
     * Zet de DeviceInfoBuffer die algemene informatie bevat over het toestel.
     *
     * @param deviceInfo
     */
    public void setDeviceInfo(DeviceInfoBuffer deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    /**
     * Zet de frameworkService, zodat andere klassen deze kunnen opvragen
     *
     * @param s
     */
    public void setService(FrameworkService s) {
        service = s;
    }

    /**
     * Geeft de service terug
     *
     * @return FrameworkService
     */
    public FrameworkService getService() {
        return service;
    }

    /**
     * Geeft de webview terug die de homepage bevat
     *
     * @return WebView
     */
    public WebView getHomeView() {
        return homeView;
    }

    /**
     * Zet de webview die de homepage bevat
     *
     * @param view
     */
    public void setHomeView(WebView view) {
        homeView = view;
    }

    /**
     * Geef de actief gezette context/activity terug
     *
     * @return Activity
     */
    public FrameworkActivity geefContext() {
        return context;
    }

    /**
     * Maak een bepaalde context/activity actief
     *
     * @param c
     */
    public void zetContext(FrameworkActivity c) {
        this.context = c;
    }

    /**
     * Sla de throughput op en bepaal aan de hand ervan de netwerkkwaliteit
     *
     * @param put
     */
    public void setThroughput(long put) {
        this.throughput = put;
        //Zodat juiste thumbnails geladen worden
        if (throughput < 700) {
            setNetworkQuality(VideoQuality.VIDEO_240p);
        } else {
            if (throughput < 2500) {
                setNetworkQuality(VideoQuality.VIDEO_360p);
            } else {
                if (throughput < 5000) {
                    setNetworkQuality(VideoQuality.VIDEO_720p);
                } else {
                    setNetworkQuality(VideoQuality.VIDEO_1080p);
                }
            }
            
        }
        Log.v("NetworkParameterController", "Throughput changed " + throughput);
    }
    
    /**
     * Return de throughput.
     * Als de parameter berekenNu true is, zal deze eerst berekend worden alvorens 
     * deze terug te geven.
     * @param berekenNu
     * @return 
     */
    public long getThroughput(boolean berekenNu) {
        if (berekenNu) {
            CalculateThroughputTask task = new CalculateThroughputTask();
            task.execute();
            while (!task.isFinished()) {
            }
        }        
        return throughput;
    }
    
    /**
     * Controle of er effectief verbinding is met het internet.
     * @return boolean
     */
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null;
    }
    
    /**
     * Zet de locationOperator, voor de enableGPSactivity
     * @param l 
     */
    public void setLocationOperator(LocationOperator l) {
        this.lo = l;
    }
    
    /**
     * Geef de locationOperator weer, voor de enableGPSActivity
     * @return LocationOperator
     */
    public LocationOperator getLocationOperator() {
        return lo;
    }
    
    /**
     * Zet de netwerkkwaliteit
     * @param q 
     */
    public void setNetworkQuality(VideoQuality q) {
        if (q.number <= VideoQuality.VIDEO_240p.number) {
            networkQuality = 0;
        } else {
            if (q.number <= VideoQuality.VIDEO_480p.number) {
                networkQuality = 1;
            } else {
                networkQuality = 2;
            }
        }
    }
    
    /**
     * Geef de netwerkkwaliteit terug, zodat er kan gekozen worden welke kwaliteit thumbnails er getoond worden
     * in de webview.
     * @return integer networkQuality
     */
    @JavascriptInterface
    public int getNetworkQuality() {
        return networkQuality;
    }
}
