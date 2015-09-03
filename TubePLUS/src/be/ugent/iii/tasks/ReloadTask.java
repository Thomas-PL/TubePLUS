/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.youtube.R;

/**
 *
 * @author Thomas
 */
public class ReloadTask extends AsyncTask<WebView, Void, Void> {

    private Activity context;
    private ProgressBar spinner;
    private WebView view;

    @Override
    protected void onPreExecute() {

        context = FrameworkController.getInstance().geefContext();
        view = (WebView) context.findViewById(R.id.webview);
        spinner = (ProgressBar) context.findViewById(R.id.main_progressBar);
        spinner.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(WebView... paramss) {
        Log.e("RELOAD", "Trying to reload...");
        view.loadUrl("javascript:showConnectionError()");
        view.loadUrl("javascript:showReload()");

        while (true) {
            view.loadUrl("file:///android_asset/index.html");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                //Als taak interrupted is, is dit omdat ze gecancelled werd en de reload dus gelukt is
                break;
            }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        Log.v("ReloadTask", "Cancelled");
        if (spinner != null) {
            spinner.setVisibility(View.GONE);
        }
        view.loadUrl("javascript:checkConnection()");
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.e("RELOAD", "DONE");
        spinner.setVisibility(View.GONE);
    }
}
