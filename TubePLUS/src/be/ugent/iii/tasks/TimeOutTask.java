/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.tasks;

import android.os.AsyncTask;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import be.ugent.iii.activities.FrameworkActivity;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.controllers.PlayerController;
import be.ugent.iii.youtube.R;

/**
 *
 * @author Thomas
 */
public class TimeOutTask extends AsyncTask<Integer, Void, Void> {

    private FrameworkActivity context;
    private ProgressBar spinner;
    private WebView view;

    @Override
    protected void onPreExecute() {
        if (PlayerController.getInstance().getSpelerView() == null) {
            context = FrameworkController.getInstance().geefContext();
            spinner = (ProgressBar) context.findViewById(R.id.main_progressBar);
            view = (WebView) context.findViewById(R.id.webview);
        } else {
            context = PlayerController.getInstance().getSpelerActivity();
            spinner = (ProgressBar) context.findViewById(R.id.player_progressBar);
            view = (WebView) context.findViewById(R.id.playerview);
        }
        spinner.setVisibility(View.VISIBLE);

    }

    @Override
    protected Void doInBackground(Integer... params) {
        int time = params[0];
        try {
            Thread.sleep(time*1000);
        } catch (InterruptedException ex) {
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        if(spinner != null)
            spinner.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPostExecute(Void result) {
        spinner.setVisibility(View.INVISIBLE);
        view.loadUrl("javascript:showConnectionError()");
        if (PlayerController.getInstance().getSpelerView() == null) {
            view.loadUrl("javascript:tryReload()");
        }
        else
            view.loadUrl("javascript:destroyBecauseOfTimeout()");
    }

}
