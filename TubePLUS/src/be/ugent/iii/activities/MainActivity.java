package be.ugent.iii.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.SearchView;
import be.ugent.iii.database.DeviceInfoCommand;
import be.ugent.iii.database.QuestionsCommand;
import be.ugent.iii.database.SessionInfoCommand;
import be.ugent.iii.optimizer.ScheduleTimerSingleton;
import be.ugent.iii.tasks.CalculateThroughputTask;
import be.ugent.iii.tasks.ReloadTask;
import be.ugent.iii.tasks.TimeOutTask;
import be.ugent.iii.youtube.R;

import com.crashlytics.android.Crashlytics;
import com.github.amlcurran.showcaseview.ShowcaseManager;
import com.github.amlcurran.showcaseview.targets.ActionItemTarget;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class MainActivity extends FrameworkActivity {

    private static final String TAG = "MainActivity";

    //Search widget
    private SearchView searchView;

    //Task voor het proberen herladen van de webview
    private ReloadTask reload;
    private WebView webView;
    private boolean hasNextPage, hasPreviousPage, firstTime;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
		// Logging:
        Log.v(TAG, "onCreate()");

        setContentView(R.layout.main);

        webView = (WebView) findViewById(R.id.webview);
        searchView = new SearchView(this);

        //Enkel de eerste keer dat er connectie wordt gedetecteerd mogen de gesturedetectors toegevoegd worden
        firstTime = true;

        networkController.zetContext(this);

        //Throughput berekenen
        CalculateThroughputTask task = new CalculateThroughputTask();
        task.execute();

        // Nieuwe identifier ophalen:
        SessionInfoCommand infoCommand = new SessionInfoCommand(this);
        sessionIdentifier = infoCommand.newSession();

        // Controleren of applicatie voor eerste maal wordt gestart via app_preferences,
        // indien wel, schrijf device info naa lokale databank en wordt de tutorial getoond
        SharedPreferences appPrefs = getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
        if (appPrefs.getAll().get(DEVICE_INFO) == null || appPrefs.getAll().get(DEVICE_INFO).toString().equals("false")) {
            new DeviceInfoCommand(this).addDeviceinfoLog();
            SharedPreferences.Editor editor = appPrefs.edit();
            editor.putBoolean(DEVICE_INFO, true);
            editor.commit();

            ShowcaseManager tutorial = new ShowcaseManager();

            ActionItemTarget searchTarget = new ActionItemTarget(this, R.id.action_search);
            ViewTarget vt = new ViewTarget(this.findViewById(R.id.main_progressBar));
            ActionViewTarget homeTarget = new ActionViewTarget(this, ActionViewTarget.Type.HOME);
            ActionViewTarget settingsTarget = new ActionViewTarget(this, ActionViewTarget.Type.OVERFLOW);

            tutorial.setContext(this);
            tutorial.addStub(vt, R.string.welkom, R.string.intro);
            tutorial.addStub(homeTarget, R.string.home, R.string.home_tut);
            tutorial.addStub(searchTarget, R.string.zoek, R.string.zoek_tut);
            tutorial.addStub(settingsTarget, R.string.instellingen, R.string.instellingen_tut);
            tutorial.addStub(vt, R.string.interactie, R.string.main_tut);
            tutorial.showNext();
        }

        // Oude loggingsinfo verwijderen, info ouder dan 100 identifiers geleden wordt weggegooid
        int storageInterval = 100;
        infoCommand.removeOldSessions(storageInterval, sessionIdentifier);
        new QuestionsCommand().removeOldSessions(storageInterval, sessionIdentifier);

        //Teller starten zodat na een bepaalde laadtijd een foutmelding gegeven wordt
        timeout.execute(getTimeoutValue());

        //Set-up van de webview voor algemene youtube-interactie
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "Android");
        webView.addJavascriptInterface(networkController, "network_controller");
        networkController.setHomeView(webView);

        checkNetworkConnection();
    }

    /**
     * controleer of er effectief connectie met het internet is. Indien ja en
     * het is de eerste keer bij deze connectie dat er controle is, laad dan de
     * homepage en activeer de gesturedetectors. Als het niet de eerste keer is,
     * dan moet er niets gebeuren.
     *
     * Indien er geen connectie is, start de reload methode.
     */
    @JavascriptInterface
    public void checkNetworkConnection() {

        if (networkController.isNetworkConnected()) {
            if (firstTime) {
                webView.loadUrl("file:///android_asset/index.html");
                loadGestureDetector();
                firstTime = false;
            }
        } else {
            firstTime = true;
            timeout.cancel(true);
            tryReload();
        }
    }

    /**
     * Gesturedetector om tussen pagina's thumbnails te swipen. Bij swipe naar
     * links worden de vorige thumbnails geladen, naar rechts de volgende.
     */
    private void loadGestureDetector() {
        webView.setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector gdt = new GestureDetector(MainActivity.this, new GestureListener());

            public boolean onTouch(View view, MotionEvent me) {
                return gdt.onTouchEvent(me);
            }

            final class GestureListener extends SimpleOnGestureListener {

                private static final int SWIPE_MIN_DISTANCE = 100;
                private static final int SWIPE_THRESHOLD_VELOCITY = 80;
                private static final int SWIPE_THRESHOLD_VERTICAL = 80;

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    super.onFling(e1, e2, velocityX, velocityY);
                    if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                            && Math.abs(e1.getY() - e2.getY()) < SWIPE_THRESHOLD_VERTICAL) {
                        onRightToLeft();
                        return true;
                    } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                            && Math.abs(e1.getY() - e2.getY()) < SWIPE_THRESHOLD_VERTICAL) {
                        onLeftToRight();
                        return true;
                    }

                    return false;
                }

                public void onRightToLeft() {
                    Log.v("SWIPE", "RIGHT TO LEFT");
                    if (hasNextPage) {
                        timeout = new TimeOutTask();
                        timeout.execute(getTimeoutValue());
                        webView.loadUrl("javascript:nextPage()");
                    }
                }

                public void onLeftToRight() {
                    Log.v("SWIPE", "LEFT TO RIGHT");
                    if (hasPreviousPage) {
                        timeout = new TimeOutTask();
                        timeout.execute(getTimeoutValue());
                        webView.loadUrl("javascript:previousPage()");
                    }
                }
            }
        });
    }

    /**
     * Inflate de actionbar. De search-widget wordt toegevoegd.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Bepaalt wat er gebeurd bij het klikken op een bepaald icoontje in de
     * actionbar.
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                return true;
            case R.id.action_framework_settings:
                stopFrameworkService();
                ScheduleTimerSingleton.removeTasks();
                startActivity(new Intent(this, FrameworkPrefsActivity.class));
                return true;
            case R.id.action_optimizer_settings:
                stopFrameworkService();
                ScheduleTimerSingleton.removeTasks();
                startActivity(new Intent(this, OptimizerPrefsActivity.class));
            default:
                timeout.cancel(true);
                timeout = new TimeOutTask();
                timeout.execute(getTimeoutValue());
                webView.loadUrl("file:///android_asset/index.html");
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart() met id = " + sessionIdentifier);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            //Zeker zijn dat de vragenactivity niet gestart wordt bij ondestroy.
            spelerController.setKill();
            //Zeker zijn dat alle threads gestopt zijn
            stopSpinner();
            Log.v(TAG, "onDestroy()");
            stopFrameworkService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v(TAG, "onRestart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onresume");
        if (!serviceIsRunning()) {
            Log.v(TAG, "Service was NOT running");
            // Service werd gestopt. We starten deze
            // terug met een nieuw sessie-id:
            // Nieuwe identifier ophalen:
            SessionInfoCommand infoCommand = new SessionInfoCommand(this);
            sessionIdentifier = infoCommand.newSession();
            startFrameworkService();
        } else {
            Log.v(TAG, "Service was running");
        }
        //controle voor bij opstarten app
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        checkNetworkConnection();
    }

    /**
     * Methode wordt opgeroepen bij het klikken op een thumbail in de webview.
     * De videospeler zal gelaunched worden.
     *
     * @param url
     * @param title
     */
    @JavascriptInterface
    public void triggerActivity(String url, String title) {
        spelerController.setVideoName(title);
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("ID", url);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        this.startActivity(i);
    }

    /**
     * Stop de workertasks.
     */
    @JavascriptInterface
    public void stopSpinner() {
        timeout.cancel(true);
        if (reload != null) {
            reload.cancel(true);
        }
    }

    /**
     * Er is een fout gebeurd bij het laden van de homepage. Probeer deze te
     * herladen.
     */
    @JavascriptInterface
    public void tryReload() {
        webView.loadUrl("javascript:showConnectionError()");
        webView.loadUrl("javascript:showReload()");
        //Als de vorige nog niet gecancelled is, is die nog aan het lopen!
        if (reload == null || reload.isCancelled()) {
            reload = new ReloadTask();
            reload.execute(webView);
        }
    }

    /**
     * Gebruikt om te weten of er nog naar rechts kan geswiped worden en of er
     * dus nog volgende thumbnails zijn.
     *
     * @param b
     */
    @JavascriptInterface
    public void setHasNextPage(boolean b) {
        hasNextPage = b;
    }

    /**
     * Gebruikt om te weten of er nog naar links kan geswiped worden en of er
     * dus nog vorige thumbnails zijn.
     *
     * @param b
     */
    @JavascriptInterface
    public void setHasPreviousPage(boolean b) {
        hasPreviousPage = b;
    }
}
