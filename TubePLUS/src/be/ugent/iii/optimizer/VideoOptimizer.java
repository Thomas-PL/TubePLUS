package be.ugent.iii.optimizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import be.ugent.iii.activities.OptimizerPrefsActivity;
import be.ugent.iii.activities.FrameworkPrefsActivity;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.controllers.PlayerController;
import be.ugent.iii.database.QuestionsCommand;
import be.ugent.iii.operators.DeviceLoadOperatorThread;
import be.ugent.iii.operators.IFrameworkOperator;
import be.ugent.iii.operators.LocationOperator;
import be.ugent.iii.operators.NetworkGeneration;
import be.ugent.iii.operators.QosOperatorThread;
import be.ugent.iii.service.FrameworkService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

/**
 * Hart van de applicatie. In deze klasse komt alle informatie over het netwerk,
 * de telefoon, de locaties en dergelijke samen, om een geïnformeerde beslissing
 * te kunnen maken welke kwaliteit video er zal aangeboden worden aan de
 * gebruiker.
 *
 * @author Thomas
 */
public class VideoOptimizer {

    private static final String TAG = "VideoOptimizer";

    // Aantal pixels voor populaire kwaliteiten
    private static final int PIXELS_240p = 102240; // 426x240
    private static final int PIXELS_360p = 230400; // 640x360
    private static final int PIXELS_480p = 384000; // 800x480
    private static final int PIXELS_720p = 921600; // 1280x720
    private static final int PIXELS_1080p = 2073600; // 1920x1080

    //Maximum kwaliteit die zal aangeboden worden
    private VideoQuality maxQuality;
    //Als de cpu deze waarde overschrijdt, zal de cpu-kwaliteit aangepast worden
    //in preferences en eventueel dus ook de video-kwaliteit
    private int cpuUpThreshold;
    private int cpuDownThreshold;

    private static final String APP_FILE = "app_preferences";
    private static final String LOCATION_INTERVAL = "gpsInterval";
    private static final String LOCATION_THRESHOLD = "location_speed_threshold";
    public static final String QUALITY_ADAPTION = "location_level_change";
    private static final String CPU_UP_THRESHOLD = "cpu_up_threshold";
    private static final String CPU_DOWN_THRESHOLD = "cpu_down_threshold";
    private static final String CPU_NUMBER_OF_TIMES = "cpu_number_of_times";
    private static final String QUALITY_PERFORMANCE = "quality_performance";
    private static final int timeDelay = 5;

    private final DeviceLoadBuffer deviceLoad;
    private final LocationBuffer locationInfo;
    private final QosBuffer qosInfo;
    private final DeviceInfoBuffer deviceInfo;
    private final FrameworkService frameworkService;
    private final long sessionIdentifier;
    private UIHandler handler;
    private boolean delayChange = false;

    // Kwaliteitsaanpassingen:
    private VideoQuality currentSetting;
    private AnalyseData latestAnalyse = new AnalyseData(true, NetworkGeneration.UNKNOWN, false, false);

    // Map voor kwaliteitsvoorkeuren:
    private HashMap<String, ?> optimizer_preferences;
    private HashMap<String, ?> app_preferences;

    // Operators bijhouden voor aanpassingen & updates:
    private QosOperatorThread qosOperator;
    private DeviceLoadOperatorThread deviceLoadOperator;
    private LocationOperator locationOperator;

    // Opslaan van logging-data:
    private QuestionsCommand loggingDb;

    //Controllers
    private PlayerController spelerController;
    private FrameworkController parameterController;

    //Houdt bij hoeveel keer een bepaalde verandering al is aangekondigd.
    //Om tegen te gaan dat de kwaliteit constant verandert.
    private int switchCounter;

    //Als de youtube-methode gebruikt wordt, zal maar 1 keer de gebruikelijke
    // watervalstructuur moeten doorlopen worden en maar 1 keer de kwaliteit instellen
    private boolean firstTime;

    /**
     * Constructor
     *
     * @param operators
     * @param frameworkService
     */
    public VideoOptimizer(ArrayList<IFrameworkOperator> operators, FrameworkService frameworkService) {
        firstTime = false;
        this.frameworkService = frameworkService;
        handler = new UIHandler();
        loggingDb = new QuestionsCommand();

        //Controllers initialiseren
        spelerController = PlayerController.getInstance();
        parameterController = FrameworkController.getInstance();

        // De intervaltijd wordt tijdelijk zeer kort ingesteld
        // om de initialisatie snel te laten verlopen:
        for (IFrameworkOperator operator : operators) {
            if (operator instanceof QosOperatorThread) {
                qosOperator = (QosOperatorThread) operator;
            } else if (operator instanceof DeviceLoadOperatorThread) {
                deviceLoadOperator = (DeviceLoadOperatorThread) operator;
            } else if (operator instanceof LocationOperator) {
                locationOperator = (LocationOperator) operator;
            }
        }

        //Huidige settings op laagste zodat initialisatie bij slecht netwerk ook snel kan verlopen
        currentSetting = VideoQuality.VIDEO_240p;

        // Voorkeuren ophalen (optimizer):
        SharedPreferences optimizerPrefs = frameworkService.getSharedPreferences(OptimizerPrefsActivity.OPTIMIZER_FILE, Context.MODE_PRIVATE);
        optimizer_preferences = (HashMap<String, ?>) optimizerPrefs.getAll();

        //private voorkeuren ophalen
        SharedPreferences appPrefs = frameworkService.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
        app_preferences = (HashMap<String, ?>) appPrefs.getAll();

        int cpuNumberOfTimes = Integer.parseInt(app_preferences.get(CPU_NUMBER_OF_TIMES).toString());
        cpuUpThreshold = Integer.parseInt(appPrefs.getAll().get(CPU_UP_THRESHOLD).toString());
        cpuDownThreshold = Integer.parseInt(appPrefs.getAll().get(CPU_DOWN_THRESHOLD).toString());

        switchCounter = 0;

        // Optimizer-preferences opslaan voor latere analyse:
        sessionIdentifier = frameworkService.getSessionIdentifier();
        new QuestionsCommand().dumpOptimizerPreferences(sessionIdentifier, optimizer_preferences);

        // Voorkeuren ophalen (framework):
        SharedPreferences frameworkPrefs = frameworkService.getSharedPreferences(FrameworkPrefsActivity.FRAMEWORK_FILE, Context.MODE_PRIVATE);
        int locationInterval = Integer.parseInt(appPrefs.getAll().get(LOCATION_INTERVAL).toString());

        // Bereken maxQuality:
	/*
         * Deze gegevens uit de database halen, kan soms te lang duren! Hierdoor
         * krijg je waarde 0 voor resolutie en wordt altijd de kwaliteit Audio
         * als maximale kwaliteit genomen!!!
         */
        WindowManager windowManager = (WindowManager) frameworkService.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenRes = metrics.widthPixels * metrics.heightPixels;
        Log.v(TAG, "ScreenRes = " + screenRes);
        if (screenRes < PIXELS_240p) {
            maxQuality = VideoQuality.ERROR;
        } else if (screenRes < PIXELS_360p && screenRes >= PIXELS_240p) {
            maxQuality = VideoQuality.VIDEO_240p;
        } else if (screenRes < PIXELS_480p && screenRes >= PIXELS_360p) {
            maxQuality = VideoQuality.VIDEO_360p;
        } else if (screenRes < PIXELS_720p && screenRes >= PIXELS_480p) {
            maxQuality = VideoQuality.VIDEO_480p;
        } else if (screenRes < PIXELS_1080p && screenRes >= PIXELS_720p) {
            maxQuality = VideoQuality.VIDEO_720p;
        } else {
            maxQuality = VideoQuality.VIDEO_1080p;
        }
        Log.v(TAG, "Maximale videokwaliteit: " + maxQuality);
        loggingDb.addOptimizerLog(sessionIdentifier, "Maximale videokwaliteit: " + maxQuality);
        Log.v(TAG, "Start op kwaliteit: " + currentSetting);
        spelerController.setCurrentQuality(currentSetting);
        loggingDb.addOptimizerLog(sessionIdentifier, "Start op kwaliteit: " + currentSetting);

        // Infoklassen aanmaken:
	/*
         * Héél belangrijk dat deze creaties na alle voorgaande code
         * komt!!!! Anders kunner er reeds updates plaatvinden vooraleer
         * bepaalde gegevens beschikbaar zijn (aangezien deze multi-threaded
         * worden opgehaald)
         * 
         * => DeviceInfo als enige is hierop een uitzondering!!! Deze informatie
         * is nodig om maxQuality te berekenen en daarvan zijn toch geen
         * periodieke updates!!!
         */
        deviceLoad = new DeviceLoadBuffer(this, operators, cpuNumberOfTimes);
        parameterController.setDeviceLoad(deviceLoad);
        locationInfo = new LocationBuffer(this, operators, locationInterval);
        parameterController.setLocationInfo(locationInfo);
        qosInfo = new QosBuffer(this, operators); // Geen simulatie!
        parameterController.setQosInfo(qosInfo);
        deviceInfo = new DeviceInfoBuffer();
        parameterController.setDeviceInfo(deviceInfo);

        ScheduleTimerSingleton.getInstance().schedule(new TimerTask() {

            @Override
            public void run() {
                handler.sendEmptyMessage(1);
            }
        }, timeDelay * 1000); // in ms, dus preference *1000!
    }

    private class UIHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    delayChange = true;
                    // De intervaltijd zetten we terug op de ingestelde
                    // waarde vanuit de preferences:
                    if (qosOperator != null) {
                        frameworkService.switchToQosWithoutPolling();
                    }
                    if (deviceLoadOperator != null) {
                        deviceLoadOperator.setPreferenceTijd();
                    }
                    if (locationOperator != null) {
                        locationOperator.setPreferenceTijd();
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }

    /**
     * Tijdens het aanpassen van de kwaliteit kan het gebeuren dat de
     * preferences aangepast werden, maar nog niet in het bestand up-to-date
     * zijn. Deze worden dus herladen.
     */
    private void reloadPreferences() {
        SharedPreferences optimizerPrefs = frameworkService.getSharedPreferences(OptimizerPrefsActivity.OPTIMIZER_FILE, Context.MODE_PRIVATE);
        optimizer_preferences = (HashMap<String, ?>) optimizerPrefs.getAll();
    }

    /*
     * De luisteraars van het framework melden een verandering van een
     * parameter. In deze methode zal gecontroleerd worden welk effect dit heeft
     * op de voorkeurskwaliteit van de videospeler.
     */
    public void notifyChange() {
        //Nodig indien de cpu kwaliteit is aangepast
        reloadPreferences();
        isWifi();
    }

    /**
     * Haal de benodigde informatie uit de preferences.
     *
     * @param preferenceKey
     * @return
     */
    private VideoQuality getQuality(String preferenceKey) {
        // Waarde wordt uit de preferences gehaald en daarna
        // omgezet tot een enum-waarde:
        int intValue;
        try {
            intValue = Integer.parseInt(optimizer_preferences.get(preferenceKey).toString());
        }catch(NullPointerException ex){
            intValue = Integer.parseInt(app_preferences.get(preferenceKey).toString());
        }
        return VideoQuality.fromInt(intValue);
    }

    /*
     hieronder volgt het waterval-systeem waarbij alle parameters in rekening gebracht worden
     om een geïnformeerde beslissing te kunnen nemen omtrent de aangeboden videokwaliteit.
     --------------------------------------------------------------------------------------
     */
    /**
     * Is er wifi of moet er gecontrolleerd worden op mobiele data?
     */
    private void isWifi() {
        if (qosInfo.isWifi()) {
            filterThroughputAdaptation(getQuality("wifi_quality"), ChangeReason.WIFI, new AnalyseData(true, NetworkGeneration.UNKNOWN, false, false));
        } else {
            checkMobileData();
        }
    }

    /**
     * Controle of het toestel aan het roamen is of niet. Neem de aangepaste
     * keuze voor elk type netwerkverbinding. Deze methode wordt zowel
     * opgeroepen bij gebruik van het framework als bij de youtube-methode.
     */
    private void checkMobileData() {
        if (qosInfo.isPhoneActive() && qosInfo.isDataAvailable()) {
            // We controleren generationNumber & mobileSignalLevel
            double generationNumber = qosInfo.getGenerationNumber();
            int mobileSignalLevel = qosInfo.getMobileSignalLevel();
            boolean isRoaming = qosInfo.isRoaming();
            // Moet er rekening gehouden worden met roaming?
            if (optimizer_preferences.get("other_quality_while_roaming") != null
                    && optimizer_preferences.get("other_quality_while_roaming").toString().equals("true") && isRoaming) {
                // Afhankelijk van deze 2 gegevens moeten bepaalde kwaliteiten
                // worden gebruikt: (opgeslagen in optimizer_preferences)
                if (generationNumber == NetworkGeneration.G2.number || mobileSignalLevel == 0) {
                    notifyResult(VideoQuality.ERROR, ChangeReason.MOBILE, new AnalyseData(false, NetworkGeneration.G2, true, false));
                } else if (generationNumber == NetworkGeneration.G2_5.number) {
                    filterLocationAdaption(getQuality("roaming_quality_2.5g"), new AnalyseData(false, NetworkGeneration.G2_5, true, false));
                } else if (generationNumber == NetworkGeneration.G2_75.number) {
                    filterLocationAdaption(getQuality("roaming_quality_2.75g"), new AnalyseData(false, NetworkGeneration.G2_75, true, false));
                } else if (generationNumber == NetworkGeneration.G3.number) {
                    filterLocationAdaption(getQuality("roaming_quality_3g"), new AnalyseData(false, NetworkGeneration.G3, true, false));
                } else if (generationNumber == NetworkGeneration.G3_5.number) {
                    filterLocationAdaption(getQuality("roaming_quality_3.5g"), new AnalyseData(false, NetworkGeneration.G3_5, true, false));
                } else if (generationNumber == NetworkGeneration.G_4.number) {
                    filterLocationAdaption(getQuality("roaming_quality_4g"), new AnalyseData(false, NetworkGeneration.G_4, true, false));
                }
            } else {
                // Afhankelijk van deze 2 gegevens moeten bepaalde kwaliteiten
                // worden gebruikt: (opgeslagen in optimizer_preferences)
                if (generationNumber == NetworkGeneration.G2.number) {
                    notifyResult(VideoQuality.ERROR, ChangeReason.MOBILE, new AnalyseData(false, NetworkGeneration.G2, false, false));
                } else if (generationNumber == NetworkGeneration.G2_5.number) {
                    filterLocationAdaption(getQuality("quality_2.5g"), new AnalyseData(false, NetworkGeneration.G2_5, false, false));
                } else if (generationNumber == NetworkGeneration.G2_75.number) {
                    filterLocationAdaption(getQuality("quality_2.75g"), new AnalyseData(false, NetworkGeneration.G2_75, false, false));
                } else if (generationNumber == NetworkGeneration.G3.number) {
                    filterLocationAdaption(getQuality("quality_3g"), new AnalyseData(false, NetworkGeneration.G3, false, false));
                } else if (generationNumber == NetworkGeneration.G3_5.number) {
                    filterLocationAdaption(getQuality("quality_3.5g"), new AnalyseData(false, NetworkGeneration.G3_5, false, false));
                } else if (generationNumber == NetworkGeneration.G_4.number) {
                    filterLocationAdaption(getQuality("quality_4g"), new AnalyseData(false, NetworkGeneration.G_4, false, false));
                }
            }
        } else {
            notifyResult(VideoQuality.ERROR, ChangeReason.MOBILE, new AnalyseData(false, NetworkGeneration.UNKNOWN, false, false));
        }
    }

    //Telkens de snelheidsdrempel wordt overschreden wordt de kwaliteit eentje lager gebracht 
    /**
     * Neem de locatieupdates in rekening om de kwaliteit eventueel aan te
     * passen. Als de snelheidsdrempel wordt overschreden, zal de kwaliteit een
     * aantal niveau's verlaagd worden, hoeveel hangt af van de instellingen in
     * de preferences. Indien er gebruik gemaakt wordt van de youtube-methode,
     * zal deze methode geen invloed hebben op de kwaliteit, aangezien er bij
     * hun methode hier geen rekening mee wordt gehouden.
     *
     * @param quality
     * @param analyseData
     */
    private void filterLocationAdaption(VideoQuality quality, AnalyseData analyseData) {
        boolean youtubeMethod = spelerController.isUsingYoutubeMethod();
        int thresholdSpeed = Integer.parseInt(app_preferences.get(LOCATION_THRESHOLD).toString());
        if (!youtubeMethod && (locationInfo.getSpeed() > thresholdSpeed)) {
            // Kwaliteitsaanpassing voorzien:
            int qualityAdaption = Integer.parseInt(app_preferences.get(QUALITY_ADAPTION).toString());
            int newQuality = quality.number - qualityAdaption;
            // Kwaliteitgrenzen mogen niet overschreven worden:
            //Op 1 gezet want anders krijgen we Error!
            if (newQuality < 1) {
                newQuality = 1;
            }
            analyseData.setLocationSpeedExceeded(true);
            Toast.makeText(frameworkService, "SPEED EXCEEDED", Toast.LENGTH_SHORT).show();
            filterThroughputAdaptation(VideoQuality.fromInt(newQuality), ChangeReason.LOCATION, analyseData);
        } else // Grens wordt niet overschreven, dus er moet geen aanpassing gebeuren, maar wel nog filter op throughput:
        {
            filterThroughputAdaptation(quality, ChangeReason.MOBILE, analyseData);
        }
    }

    /**
     * De schermgrootte wordt in rekening gebracht. Indien een scherm niet
     * voldoende pixels heeft om HD te tonen zal ook geen HD aangeboden worden.
     *
     * @param quality
     * @return
     */
    private VideoQuality filterScreenSize(VideoQuality quality) {
        if (quality.number > maxQuality.number) {
            return maxQuality;
        } else {
            return quality;
        }
    }

    /**
     * Neem de throughput van een netwerk in rekening. Een wifi-netwerk zal niet
     * altijd even goede resultaten weergeven, dus moet er ook gecontroleerd
     * worden op de throughput.
     *
     * @param quality
     * @param reason
     * @param analyseData
     */
    private void filterThroughputAdaptation(VideoQuality quality, ChangeReason reason, AnalyseData analyseData) {
        long throughput = parameterController.getThroughput(false);
        if (throughput != 0) {
            if (throughput < 250) {
                //audio TODO VOOR DE TOEKOMST
                if (quality.number > VideoQuality.VIDEO_240p.number) {
                    quality = VideoQuality.VIDEO_240p;
                    reason = ChangeReason.THROUGHPUT;
                }
            } else if (throughput < 700) {
                //240
                if (quality.number > VideoQuality.VIDEO_240p.number) {
                    quality = VideoQuality.VIDEO_240p;
                    reason = ChangeReason.THROUGHPUT;
                }
            } else if (throughput < 1250) {
                //360
                if (quality.number > VideoQuality.VIDEO_360p.number) {
                    quality = VideoQuality.VIDEO_360p;
                    reason = ChangeReason.THROUGHPUT;
                }
            } else if (throughput < 2500) {
                //480
                if (quality.number > VideoQuality.VIDEO_480p.number) {
                    quality = VideoQuality.VIDEO_480p;
                    reason = ChangeReason.THROUGHPUT;
                }
            } else if (throughput < 5000) {
                //720
                if (quality.number > VideoQuality.VIDEO_720p.number) {
                    quality = VideoQuality.VIDEO_720p;
                    reason = ChangeReason.THROUGHPUT;
                }
            } else {
                //1080
                quality = VideoQuality.VIDEO_1080p;
                reason = ChangeReason.THROUGHPUT;
            }
        }
        notifyResult(quality, reason, analyseData);
    }

    /**
     * Indien er gebruik gemaakt wordt van de youtube-methode, wordt het
     * waterval-systeem maar 1x doorlopen.
     */
    public void calculateForYoutubeMethod() {
        firstTime = true;
        notifyChange();
    }

    /**
     * Neem de cpu-belasting in rekening. Indien een bepaalde threshold
     * overschreden wordt, zal de kwaliteit van de cpu aangepast worden in de
     * preferences en dit kan invloed hebben op de videokwaliteit. Indien de
     * cpu-belasting constant onder een bepaalde waarde blijft, kan de
     * cpu-kwaliteit terug verhoogd worden in de preferences.
     */
    public void notifyCpuMeasurement() {
        boolean youtubeMethod = spelerController.isUsingYoutubeMethod();

        if (!youtubeMethod) {
            // Bij overschreiden CPU-drempel, kwaliteit verlagen:
            float averagecpu = deviceLoad.getAverageCpuUsage() * 100;

            if (averagecpu > cpuUpThreshold && currentSetting.number > 1) {
                SharedPreferences optimizerPrefs = frameworkService.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = optimizerPrefs.edit();
                int newNumber = currentSetting.number - 1;
                editor.putString("cpu_quality", Integer.toString(newNumber));
                Log.v("VideoOptimizer", "Changing cpu quality--:" + newNumber);
                editor.commit();
                // Verandering onmiddellijk melden om aanpassing door te voeren:
                notifyChange();
            } else {
                //verhoog terug eentje indien mogelijk
                if (averagecpu < cpuDownThreshold && currentSetting.number < maxQuality.number) {
                    SharedPreferences optimizerPrefs = frameworkService.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = optimizerPrefs.edit();
                    int newNumber = currentSetting.number + 1;
                    editor.putString("cpu_quality", Integer.toString(newNumber));
                    Log.v("VideoOptimizer", "Changing cpu quality++:" + newNumber);
                    editor.commit();
                    // Verandering onmiddellijk melden om aanpassing door te voeren:
                    notifyChange();
                }
            }
        }
    }

    /*
     * Na de analyse die is gebeurd door notifyChange komen we hoe dan ook in
     * een blad van de boom (een resultaat) terecht. Hier behandelen we het
     * resultaat:
     */
    private void notifyResult(VideoQuality result, ChangeReason reason, AnalyseData analyseData) {

        //kwaliteit van netwerk instellen, als er al een throughput berekend is, anders wil dit zeggen dat het netwerk heel traag is!
        if (parameterController.getThroughput(false) != 0) {
            parameterController.setNetworkQuality(result);
        } else {
            //ZOU AUDIO MOETEN ZIJN, MAAR AANGEZIEN AUDIO ER NIET INZIT WERD 240 GEKOZEN
            result = VideoQuality.VIDEO_240p;
            reason = ChangeReason.THROUGHPUT;
        }

        spelerController.getBufferedPercentage();

        //Kiest de gebruiker voor kwaliteit of voor performantie?
        int qualityOrPerformance = Integer.parseInt(app_preferences.get(QUALITY_PERFORMANCE).toString());
        if (qualityOrPerformance == 1) {
            //Er werd gekozen door de gebruiker voor kwaliteit. Er zal steeds voor de maximale kwaliteit gekozen worden.
            if (spelerController.getCurrentQuality() != maxQuality) {
                spelerController.setCurrentQuality(maxQuality);
            }
        } else if (!spelerController.isUsingYoutubeMethod()) {
            //Er werd gekozen door de gebruiker voor performantie. Het framework zal zijn werk doen.

            // Als de voorgestelde kwaliteit groter is dan wat het toestel aankan, wordt de kwaliteit verlaagd
            VideoQuality cpuMaxQuality = getQuality("cpu_quality");
            if (result.number > cpuMaxQuality.number) {
                result = cpuMaxQuality;
                reason = ChangeReason.CPU;
            }
            // Maximale resolutie voor het toestel beperken:
            final VideoQuality preferredSetting = filterScreenSize(result);
            Log.v(TAG, "Voorstel switch quality: " + preferredSetting + " (Reason: " + reason + ")");
            Log.v(TAG, "Huidige quality is: " + currentSetting);
            loggingDb.addOptimizerLog(sessionIdentifier, "Voorstel switch quality: " + preferredSetting + " (Reason: " + reason + ")");
            if (preferredSetting != currentSetting) {
                switchCounter++;
                //Om teveel switches tegen te gaan wachten we tot er 5 keer dezelfde verandering aangekondigd werd
                if (switchCounter == 5) {
                    currentSetting = preferredSetting;
                    latestAnalyse = analyseData;
                    Log.v(TAG, "Switch quality doorgevoerd: " + currentSetting + " (Reason: " + reason + ")");
                    loggingDb.addOptimizerLog(sessionIdentifier, "Switch quality doorgevoerd: " + currentSetting + " (Reason: " + reason + ")");
                    spelerController.setCurrentAnalyseData(latestAnalyse);
                    if (currentSetting != VideoQuality.ERROR) {
                        spelerController.setCurrentQuality(currentSetting);
                    }
                }
            } else {
                switchCounter = 0;
                if (!delayChange) {
                    latestAnalyse = analyseData;
                }
            }

        } else {
            //YOUTUBEMETHODE
            if (firstTime) {
                firstTime = false;
                //Youtubemethode werd gebruikt, dus eenmalig de kwaliteit aanpassen
                VideoQuality cpuMaxQuality = getQuality("cpu_quality");
                if (result.number > cpuMaxQuality.number) {
                    result = cpuMaxQuality;
                    reason = ChangeReason.CPU;
                }
                // Maximale resolutie voor het toestel beperken:
                final VideoQuality preferredSetting = filterScreenSize(result);
                if (preferredSetting != currentSetting) {
                    currentSetting = preferredSetting;
                    latestAnalyse = analyseData;
                    Log.v(TAG, "Switch quality doorgevoerd: " + currentSetting + " (Reason: " + reason + ")");
                    loggingDb.addOptimizerLog(sessionIdentifier, "Switch quality doorgevoerd: " + currentSetting + " (Reason: " + reason + ")");
                    spelerController.setCurrentAnalyseData(latestAnalyse);
                    if (currentSetting != VideoQuality.ERROR) {
                        spelerController.setCurrentQuality(currentSetting);
                    }
                } else {
                    switchCounter = 0;
                    if (!delayChange) {
                        latestAnalyse = analyseData;
                    }
                }
            }
        }
    }
}
