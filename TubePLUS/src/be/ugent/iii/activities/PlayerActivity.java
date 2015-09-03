package be.ugent.iii.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import static be.ugent.iii.activities.FrameworkActivity.APP_FILE;
import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.controllers.PlayerController;
import be.ugent.iii.controllers.QuestionController;
import be.ugent.iii.database.ComplaintCommand;
import be.ugent.iii.optimizer.AnalyseData;
import be.ugent.iii.optimizer.VideoOptimizer;
import be.ugent.iii.questions.ChoiceQuestion;
import be.ugent.iii.questions.ChoiceQuestionActivity;
import be.ugent.iii.questions.QuestionList;
import be.ugent.iii.questions.RatingQuestion;
import be.ugent.iii.questions.RatingQuestionActivity;
import be.ugent.iii.service.FrameworkService;
import be.ugent.iii.service.FrameworkService.FrameworkBinder;
import be.ugent.iii.tasks.PushComplaintTask;
import be.ugent.iii.tasks.SoundTask;
import be.ugent.iii.tasks.ThroughputThread;
import be.ugent.iii.tasks.TimeOutTask;
import be.ugent.iii.youtube.R;
import com.github.amlcurran.showcaseview.ShowcaseManager;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

/**
 * Activity voor de videospeler
 *
 * @author Thomas
 */
public class PlayerActivity extends FrameworkActivity {

    private static final String TAG = "PlayerActivity";

    private String video_id;

    //Rode knop
    private Button floatButton;
    private boolean moving;
    private float oldX, oldY;

    // FrameworkService:
    private ServiceConnection frameworkConnection;

    //TODO: niet elke keer questionList tonen
    private boolean showQuestionList;

    //Om tegen te gaan dat questions nog getoond worden
    private boolean kill = false;
    private WebView webView;

    //Dynamic sound task
    private SoundTask soundTask;

    //Optimizer voor het bepalen van de kwaliteit van de speler
    private VideoOptimizer optimizer;

    private int complaintCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        complaintCount = 0;

        //Zodat de controller aan de huidige speleractivity kan
        spelerController.zetContext(this);

        //Als speler naar achtergrond wordt verwezen door gebruiker, blijft
        //deze toch zichtbaar met icoontje en kan de gebruiker er naar terugkeren
        networkController.getService().setToForeground();

        //Start de throughputThread die op 3 punten in elk filmpje de TP zal berekenen
        tpThread = new ThroughputThread(0);
        tpThread.start();

        //Beslissen of ons framework zal gebruikt worden, of de originele youtube manier.
        // Dit is nodig om te onderzoeken of onze methode beter werkt dan het origineel
        spelerController.setUsingYoutubeMethod();

        //Voor het gemak wordt de speler enkel in landscape getoond
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getActionBar().hide();

        //voor rode knop
        moving = false;

        //Initialiseer de webview
        Intent intent = getIntent();
        video_id = intent.getStringExtra("ID");

        webView = (WebView) findViewById(R.id.playerview);

        PlayerController.getInstance().setSpelerView(webView);
        //Start timeout om ervoor te zorgen dat het filmpje niet oneindig blijft laden
        timeout.execute(getTimeoutValue());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setPluginState(PluginState.ON);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(this, "android_player");
        webView.addJavascriptInterface(spelerController, "player_controller");
        webView.loadUrl("file:///android_asset/player.html");

        //Rode knop
        floatButton = (Button) findViewById(R.id.floating_button);

        // Controleren of speler voor eerste maal wordt gestart via app_preferences,
        // indien wel, wordt de tutorial getoond
        SharedPreferences appPrefs = getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
        if (appPrefs.getAll().get(PLAYER_INFO) == null || appPrefs.getAll().get(PLAYER_INFO).toString().equals("false")) {
            SharedPreferences.Editor editor = appPrefs.edit();
            editor.putBoolean(PLAYER_INFO, true);
            editor.commit();

            ShowcaseManager tutorial = new ShowcaseManager();
            tutorial.setContext(this);
            ViewTarget target = new ViewTarget(floatButton);
            tutorial.addStub(target, R.string.rodeknop, R.string.rode_tut);

            target = new ViewTarget(R.id.player_progressBar, this);
            tutorial.addStub(target, R.string.speler, R.string.speler_tut);

            tutorial.showNext();
        }

        final PlayerActivity pa = this;

        //Tegengaan dat verslepen niet als klik geregistreerd wordt en omgekeerd.
        // Sleep wordt pas geregistreerd vanaf er een bepaalde afstand versleept werd,
        // Anders wordt dit gezien als een klik
        floatButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!moving) {
                    oldX = floatButton.getX();
                    oldY = floatButton.getY();
                }

                float newx = floatButton.getX() - (45 - event.getX());
                float newy = floatButton.getY() - (30 - event.getY());
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(oldX - newx) > 45 || Math.abs(oldY - newy) > 30) {
                            moving = true;
                            if (newx > 0 && newx + 90 < getwidth()) {
                                if (newy > 0 && newy + 60 < getheight()) {
                                    floatButton.setX(newx);
                                    floatButton.setY(newy);
                                }
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        if (Math.abs(oldX - newx) < 45 && Math.abs(oldY - newy) < 30 && !moving) {
                            if (complaintCount < 5) {
                                ComplaintCommand c = new ComplaintCommand();
                                //in commentaar want is nog bugged
                                c.addComplaint();
                                Toast.makeText(pa, "Klacht geregistreerd", Toast.LENGTH_SHORT).show();
                                complaintCount++;
                            } else {
                                Toast.makeText(pa, "Max aantal klachten bereikt", Toast.LENGTH_SHORT).show();
                            }
                        }
                        moving = false;
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        //Ervoor zorgen dat de speler voorrang krijgt, maar als er bv een telefoontje
        // of smsje binnenkomt dat die voorrang krijgt.
        if (!Build.MANUFACTURER.equals("samsung")) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(spelerController, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.e("PlayerActivity", "Could not get audiofocus");
            }
        }
        //Scan voor backgroundnoise en pas volume er aan aan
        soundTask = new SoundTask(5, networkController.getService());
        soundTask.enableOperator();
    }

    @Override
    protected void onStart() {
        Log.v("PlayerActivity", "onStart called");
        super.onStart();

        if (frameworkConnection == null) {
            // Binden aan framework om luisteraars te ondersteunen:
            frameworkConnection = new ServiceConnection() {

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    // SessionIdentifier ophalen:
                    sessionIdentifier = ((FrameworkBinder) service).getSessionIdentifier();
                    optimizer = ((FrameworkBinder) service).getOptimizer();
                    if (spelerController.isUsingYoutubeMethod()) {
                        optimizer.calculateForYoutubeMethod();
                    }
                }
            };

            // onBind op FrameworkService:
            Intent frameworkIntent = new Intent(this, FrameworkService.class);
            bindService(frameworkIntent, frameworkConnection, 0);
        }
    }

    /**
     * Start de vragenlijst na het bekijken van een filmpje
     *
     * @param questionList
     * @param isComplete
     */
    private void startQuestionActivity(QuestionList questionList, boolean isComplete) {
        Log.v("Deserializer", "Aantal vragen = " + questionList.getQuestions().size());
        // SessionIdentifier doorgeven aan QuestionHandler:
        questionController.setSessionIdentifier(sessionIdentifier);
        // Intent aanmaken:
        Intent survey = null;
        if (questionList.getQuestions().get(0) instanceof RatingQuestion) {
            survey = new Intent(this, RatingQuestionActivity.class);
        } else if (questionList.getQuestions().get(0) instanceof ChoiceQuestion) {
            survey = new Intent(this, ChoiceQuestionActivity.class);
        }

        // Activity starten:
        if (survey != null) {
            AnalyseData currentAnalyseData = spelerController.getCurrentAnalyseData();
            questionList.setAnalyseData(currentAnalyseData);
            survey.putExtra(QuestionController.QUESTION_NUMBER, 0);
            survey.putExtra(QuestionController.IS_COMPLETE, isComplete);
            startActivity(survey);
        }
        finish();
        // Framework stoppen:
        stopService(new Intent(this, FrameworkService.class));
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause opgeroepen");
        super.onPause();
        ((AudioManager) getSystemService(
                Context.AUDIO_SERVICE)).requestAudioFocus(
                        new OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int focusChange) {
                            }
                        }, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Als gebruiker in een filmpje zoekt naar een ander tijdstip, wordt de
     * throughput-thread gereset op dat punt, zodat de tijdstippen voor de
     * throughput te berekenen aangepast worden.
     *
     * @param start
     */
    @JavascriptInterface
    public void resetTPThread(int start) {
        tpThread.interrupt();
        tpThread = new ThroughputThread(start);
        tpThread.start();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * De activity wordt destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        networkController.getService().stopForeground();
        //Stop de dynamische audio
        soundTask.disableOperator();
        //Stop de throughputtrhead, moest deze nog draaien
        tpThread.interrupt();

        //unbind de activity van de service
        unbindService(frameworkConnection);

        //Als de activity niet gekilled werd samen met de rest van de app, mogen de vragen worden opgestart
        if (!kill && timeout.isCancelled() && !spelerController.kill()) {
            //Soms blijven de vragen hangen, om dit tegen te gaan wordt ook hier een timeouttask gestart
            timeout = new TimeOutTask();
            timeout.execute(getTimeoutValue());
            spelerController.stopVideo();
            //Sensormonitoring stoppen
            sensorManager.unregisterListener(brightness);
            sensorManager.unregisterListener(proximity);

            // De video-applicatie wordt gepauzeerd of gestopt,
            // dus wordt de vragenlijst voorgelegd aan de gebruiker.
            double duration = spelerController.getDuration() * 1.0;
            double currentVideoposition = spelerController.getCurrentPosition() * 1.0;

            //Webview is om een of andere reden al destroyed: currentVideoPosition == -2
            if (currentVideoposition != -2) {
                int counter = 0;
                //Soms is de aanvraag via javascript wat vertraagd, dus wordt er hier wat gewacht tot er antwoord is
                while (currentVideoposition == -1 && counter < 2000) {
                    currentVideoposition = spelerController.getCurrentPosition();
                    counter++;
                }
                showQuestionList = spelerController.isStarted();
                // Als video wordt gepauzeerd wanneer de initialisatie
                // nog niet is volbracht, kan duration -1 zijn!
                if (duration != -1 && showQuestionList) {
                    // Zoek de eerste gepaste vragenlijst op basis van het
                    // percentage dat werd bekeken. Werd de video bijna helemaal bekeken, dan wordt er een
                    // andere vragenlijst voorgelegd.
                    int percentage = (int) ((currentVideoposition / (duration)) * 100);

                    int maxPercentage = questionController.getIncompleteList().getUpperLimit();
                    timeout.cancel(true);
                    if (percentage <= maxPercentage) {
                        // Onvolledige lijst weergeven:
                        QuestionList questionList = questionController.getIncompleteList();
                        startQuestionActivity(questionList, false);
                    } else {
                        // Volledige lijst weergeven:
                        QuestionList questionList = questionController.getCompleteList();
                        startQuestionActivity(questionList, true);
                    }
                }
            }
        }
        

        //Ontkoppel alles van de webview om errors tegen te gaan
        FrameLayout _layout = (FrameLayout) findViewById(R.id.fullscreen_custom_content);
        _layout.removeView(webView);
        webView.goBack();
        webView.removeAllViews();
        webView.clearCache(true);
        webView.clearAnimation();
        webView.clearHistory();
        webView.destroy();

        spelerController.setSpelerView(null);
        spelerController.clearQualityList();
        timeout.cancel(true);

        spelerController.setStarted(false);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(spelerController);

        //Complaints van het filmpje doorsturen naar externe database.
        //Dit wordt niet tijdens het filmpje gedaan, maar nu pas, om het netwerk niet extra te belasten tijdens het streamen
        PushComplaintTask complaintTask = new PushComplaintTask();
        complaintTask.execute();
    }

    /**
     * geeft de video ID terug naar de webview, om aan de youtube data api het
     * juiste filmpje op te vragen
     *
     * @return
     */
    @JavascriptInterface
    public String getUrl() {
        return video_id;
    }

    /**
     * Kill de activity want playback is niet gelukt.
     */
    @JavascriptInterface
    public void destroy() {
        Toast.makeText(this, "Playback failed", Toast.LENGTH_SHORT).show();
        kill = true;
        this.finish();
    }

    /**
     * Stop de timeout-task want het laden van de speler is gelukt.
     */
    @JavascriptInterface
    public void stopSpinner() {

        timeout.cancel(true);
    }
}
