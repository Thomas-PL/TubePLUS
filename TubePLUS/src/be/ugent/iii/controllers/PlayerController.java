package be.ugent.iii.controllers;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import be.ugent.iii.activities.PlayerActivity;
import be.ugent.iii.optimizer.AnalyseData;
import be.ugent.iii.optimizer.VideoQuality;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SpelerController met algemene informatie voor de videospeler
 *
 * @author Thomas
 */
public class PlayerController implements OnAudioFocusChangeListener {

    private static PlayerController instance;
    private WebView spelerView;
    private List<VideoQuality> qualities;
    private VideoQuality currentQuality;
    private float bufferedPercentage;
    private AnalyseData currentAnalyseData;
    private int duration;
    private int currentPosition;
    private PlayerActivity context;
    private boolean started;

    //Moet de speleractivity gekilled worden zonder vragen te stellen?
    private boolean kill;

    private String videoName;
    private int volume;

    //Wordt de youtube-methodiek gebruikt of ons framework?
    private boolean usingYoutubeMethod;

    //Houdt bij of de batterij bijna leeg is.
    private boolean batteryLow;

    /**
     * Singleton pattern
     *
     * @return
     */
    public static PlayerController getInstance() {
        if (instance == null) {

            instance = new PlayerController();
        }
        return instance;
    }

    /**
     * private constructor
     */
    private PlayerController() {
        qualities = new ArrayList<VideoQuality>();
        bufferedPercentage = 0;
        currentPosition = -1;
        videoName = "None selected";
        usingYoutubeMethod = false;
        batteryLow = false;
    }

    /**
     * Zet de huidige kwaliteit voor de videospeler. Als er nog geen kwaliteiten
     * in de lijst zitten, dan is er nog geen speleractivity gestart en is het
     * framwork toch al voorstellen voor kwaliteit aan het doen. Om geen fouten
     * te werpen, wordt de kwaliteit nog niet aangepast en enkel opgeslagen.
     *
     * Indien er wel al een actieve spelerview is, wordt de nieuwe kwaliteit
     * doorgestuurd naar de webview.
     *
     * @param q
     */
    public void setCurrentQuality(VideoQuality q) {
        if (!batteryLow) {
            currentQuality = q;
            if (!qualities.isEmpty()) {
                switchQuality(q, true);
            }
        }
    }

    /**
     * Wordt de youtube-methodiek(true) gebruikt of ons framework(false)?
     *
     * @return
     */
    public boolean isUsingYoutubeMethod() {
        return usingYoutubeMethod;
    }

    /**
     * Een randomgenerator beslist of ons framework gebruikt wordt of de
     * youtube-methodiek.
     */
    public void setUsingYoutubeMethod() {
        this.usingYoutubeMethod = Math.random() < 0.5;
        if (usingYoutubeMethod) {
            Log.e("SpelerController", "using youtube method");
        }
    }

    /**
     * Sla de spelercontext op van de playeractivity.
     *
     * @param c
     */
    public void zetContext(PlayerActivity c) {
        context = c;
    }

    /**
     * Geef de spelercontext terug.
     *
     * @return playerActivity
     */
    public PlayerActivity getSpelerActivity() {
        return context;
    }

    /**
     * Geeft de huidige speelkwaliteit terug. Als er nog geen kwaliteiten in de
     * qualities list zitten, is de speleractivity nog niet gestart en zijn de
     * correcte kwaliteiten nog niet ingeladen door de data-api. Indien er wel
     * al inzitten, zal eerst nog eens switchQuality opgeroepen worden zodat
     * zeker de correcte kwaliteit weergegeven wordt. Het kan namelijk zijn dat
     * het framework voorstelt 480p te gebruiken, terwijl de video maximaal 360p
     * aanbiedt. De switchquality methode zal dan 360p activeren.
     *
     * @return
     */
    public VideoQuality getCurrentQuality() {
        //om zeker te zijn dat de kwaliteit die gevraagd wordt aangeboden wordt door het filmpje
        if (!qualities.isEmpty()) {
            switchQuality(currentQuality, false);
        }
        return currentQuality;
    }

    /**
     * Geeft de spelerWebView terug
     *
     * @return
     */
    public WebView getSpelerView() {
        return spelerView;
    }

    /**
     * Zet de actieve spelerWebView
     *
     * @param view
     */
    public void setSpelerView(WebView view) {
        spelerView = view;
    }

    /**
     * Geef het percentage video terug dat al in de buffer is ingeladen.
     *
     *
     * DIT WERKT NIET CORRECT
     *
     * @return
     */
    public float getBufferedPercentage() {
        if (spelerView != null) {
            spelerView.loadUrl("javascript:setBufferedPercentage()");
        }

        return bufferedPercentage;
    }

    /**
     * Maak de lijst met kwaliteiten leeg.
     */
    public void clearQualityList() {
        qualities.clear();
        currentPosition = -1;
        duration = 0;
    }

    /**
     * Verander de kwaliteit naar de gewenste meegegeven als parameter. Deze
     * kwaliteit wordt ook gepushed naar de videospeler in de webview, indien
     * nodig. Er wordt gecontroleerd of de gewenste kwaliteit effectief wordt
     * aangeboden door de video. Indien dit niet het geval is, wordt de
     * dichtstbijzijnde lagere kwaliteit aangeboden. Als alles faalt, zal de
     * default kwaliteit gekozen worden.
     *
     * @param q
     * @param hasToPush
     */
    public void switchQuality(VideoQuality q, boolean hasToPush) {
        if (hasToPush) {
            Log.v("SpelerController", "switching quality to " + q.name());
        }
        String quality;
//        if (q == VideoQuality.AUDIO) {
//            quality = "auto";
//            currentQuality = VideoQuality.VIDEO_240p;
//        } else {
        quality = q.toString().substring(6);

        //controle of de gewenste kwaliteit wel aangeboden wordt door het filmpje
        if (!qualities.contains(q)) {
            Log.e("SpelerController", "DOES NOT CONTAIN: " + q.toString());
            int number = q.number--;
            //zoek de dichts bijzijnde kwaliteit die wel aangeboden wordt
            while (number > 1 && !qualities.contains(VideoQuality.fromInt(number))) {
                number--;
            }
//                if (VideoQuality.fromInt(number) != VideoQuality.AUDIO) {
            quality = VideoQuality.fromInt(number).toString().substring(6);
            currentQuality = VideoQuality.fromQualityString(quality);
            //Als er iets fout liep zal de auto kwaliteit gekozen worden
            if (currentQuality == VideoQuality.ERROR) {
                quality = "auto";
                currentQuality = VideoQuality.VIDEO_240p;
            }
        }
        if (spelerView != null && hasToPush) {
            spelerView.loadUrl("javascript:changeQuality()");
        }
    }

    /**
     * Zet de laatste analysedata, zodat deze kan opgevraagd worden bij
     * klachten.
     *
     * @param a
     */
    public void setCurrentAnalyseData(AnalyseData a) {
        currentAnalyseData = a;
    }

    /**
     * Geef de laaste analyse terug, zodat deze kan meegegeven worden bij een
     * klacht.
     *
     * @return
     */
    public AnalyseData getCurrentAnalyseData() {
        return currentAnalyseData;
    }

    /**
     * Geef de duur van het huidige filmpje terug
     *
     * @return
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Als er een actieve videospeler is, wordt de huidige positie in het
     * filmpje teruggegeven
     *
     * @return
     */
    public int getCurrentPosition() {
        if (spelerView == null) {
            return -2;
        }
        if (spelerView != null) {
            spelerView.loadUrl("javascript:setCurrentPosition()");
        }
        return currentPosition;
    }

    public void stopVideo() {
        if (spelerView != null) {
            spelerView.loadUrl("javascript:stopVideo()");
        }
    }

    public void pauseVideo() {
        if (spelerView != null) {
            spelerView.loadUrl("javascript:pauseVideo()");
        }
    }

    private void restoreVolume() {
        //spelerView.loadUrl("javascript:restoreVolume()");
    }

    private void storeVolume() {
        //spelerView.loadUrl("javascript:storeVolume()");
    }

    /**
     * Verander het volume van de speler
     *
     * @param v
     */
    public void setVolume(int v) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
        Log.v("Spelercontroller", "Volume set to " + v);
        volume = v;
        //spelerView.loadUrl("javascript:setVolume("+v+")");
    }

    /**
     * Demp het volume.
     */
    public void mute() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        //spelerView.loadUrl("javascript:mute()");
    }

    /**
     * Ontdemp het volume.
     */
    public void unMute() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        //spelerView.loadUrl("javascript:unMute()");
    }

    /**
     * Moet de speleractivity gekilled worden?
     *
     * @return
     */
    public boolean kill() {
        return kill;
    }

    /**
     * Zet of de speleractivity moet gekilled worden
     */
    public void setKill() {
        kill = true;
    }

    /**
     * Als de videospeler audiofocus verliest, kan dit om verschillende redenen
     * zijn. Als de focus maar even weg is, zal het volume nog zachtjes
     * verderspelen. Verlies de speler compleet focus zal de speler stoppen met
     * afspelen.
     *
     * @param focusChange
     */
    public void onAudioFocusChange(int focusChange) {
        Log.e("PlayerController", "focuschange: " + focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback can only be acquired by user touching the screen, we can only unmute the player again            
                restoreVolume();
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback
                stopVideo();
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(this);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                pauseVideo();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                storeVolume();
                setVolume(10);
                break;
        }
    }

    /**
     * Zet de naam van de video die afgespeeld wordt. Dit wordt gebruikt bij de
     * foregroundservice.
     *
     * @param name
     */
    public void setVideoName(String name) {
        videoName = name;
    }

    /**
     * Geef de naam van de video die afgespeeld wordt. Dit wordt gebruikt bij de
     * foregroundservice.
     *
     * @return
     */
    public String getVideoName() {
        return videoName;
    }

    /**
     * Wordt opgeroepen vanuit de webview om het gebufferde percentage weer te
     * geven.
     *
     * @param p
     */
    @JavascriptInterface
    public void setBufferedPercentage(float p) {
        bufferedPercentage = p;
    }

    /**
     * Wordt opgeroepen vanuit de webview. Elke kwaliteit die aangeboden wordt
     * door een filmpje zal op deze manier toegevoegd worden.
     *
     * @param quality
     */
    @JavascriptInterface
    public void addQuality(String quality) {
        VideoQuality q = VideoQuality.fromQualityString(quality);
        if (!qualities.contains(q)) {
            qualities.add(q);
            Log.v("SpelerController", "Quality found: " + quality);
        }
    }

    /**
     * Geeft de huidige speelkwaliteit terug op een manier dat de youtube-speler
     * weet welke hij moet afspelen.
     *
     * @return
     */
    @JavascriptInterface
    public String getPlayQuality() {
//        if (getCurrentQuality() == VideoQuality.AUDIO) {
//            return "auto";
//        } else {
        return getCurrentQuality().toString().substring(6);
//        }
    }

    /**
     * Zet de duur van het filmpje. Deze komt binnen in een specifiek formaat,
     * maar wordt hier omgevormd naar seconden.
     *
     * @param d
     */
    @JavascriptInterface
    public void setDuration(String d) {
        int index = d.indexOf("M");
        String minutes = d.substring(0, index);
        String seconds = d.substring(index + 1, d.length() - 1);
        duration = (Integer.parseInt(minutes) * 60) + Integer.parseInt(seconds);
    }

    /**
     * Omdat de youtube-speler buggy werkt na veranderen van kwaliteit, de
     * playback stopt namelijk zomaar bij seekto() als de buffer nog niet gevuld
     * is, en het buffered event wordt verkeerd getriggered, moeten we zelf de
     * applicatie even laten slapen zodat er wat in de buffer zit alvorens de
     * speler te laten zoeken naar het punt waar de kwaliteit veranderd werd.
     * Hoe lang de applicatie slaapt, hangt af van de netwerkkwaliteit. Eens de
     * applicatie voorbij de slaapperiode is, zal de videospeler getriggered
     * worden te zoeken naar het punt waar de kwaliteit veranderd was en vanaf
     * daar verder spelen.
     */
    @JavascriptInterface
    public void startTimeOut() {
        mute();
        long tp = FrameworkController.getInstance().getThroughput(false);

        //TODO: mocht het nog lukken om buffer te weten te komen zou het veel mooier zijn het onderstaande daarvan te laten afhangen 
        //slaaptijd laten afhanven van throughput, maar moet eerst onderzocht worden hoe lang het ongeveer duurt tot de speler terug speelt
        try {
            if (tp < 100) {
                Thread.sleep(10000);
            } else if (tp < 300) {
                Thread.sleep(7500);
            } else if (tp < 500) {
                Thread.sleep(6000);
            } else if (tp < 750) {
                Thread.sleep(4500);
            } else if (tp < 1000) {
                Thread.sleep(3500);
            } else {
                Thread.sleep(2500);
            }

            spelerView.loadUrl("javascript:seekTo()");
        } catch (InterruptedException ex) {
            Logger.getLogger(PlayerController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            unMute();
        }
    }

    /**
     * Zet de positie waar de speler zat in het filmpje.
     *
     * @param p
     */
    @JavascriptInterface
    public void setCurrentPosition(int p) {
        currentPosition = p;
    }

    public boolean isStarted() {
        return started;
    }

    @JavascriptInterface
    public void setStarted(boolean started) {
        this.started = started;
    }

    public void setBatteryLow(boolean b) {
        batteryLow = b;
        if (batteryLow) {
            Toast.makeText(context, "Batterij bijna leeg, kwaliteit wordt verlaagd", Toast.LENGTH_LONG).show();
            switchQuality(VideoQuality.VIDEO_240p, true);
        }
    }

}
