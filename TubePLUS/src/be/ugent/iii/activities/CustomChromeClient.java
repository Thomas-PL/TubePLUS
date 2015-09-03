/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.activities;

import android.app.Activity;
import static android.content.ContentValues.TAG;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.VideoView;
import be.ugent.iii.controllers.PlayerController;
import java.text.MessageFormat.Field;

/**
 *
 * @author Thomas
 */
public class CustomChromeClient extends WebChromeClient implements MediaPlayer.OnBufferingUpdateListener {

    private Activity ac;

    public CustomChromeClient(Activity a) {
        ac = a;
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback); //To change body of generated methods, choose Tools | Templates.
        Log.e("customchromeclient", "onshowcustomview");
        if (view instanceof FrameLayout) {
            Log.e("customchromeclient", "framelayout");
            final FrameLayout frame = (FrameLayout) view;
            if (frame.getFocusedChild() instanceof VideoView) {
                    // get video view
                Log.e("customchromeclient", "videoview");
                VideoView video = (VideoView) frame.getFocusedChild();

            }
        }
    }

    public void onBufferingUpdate(MediaPlayer mp, int i) {
        Log.e("customchromeclient", "onbufferingupdate");
        ((AudioManager) ac.getSystemService(
                Context.AUDIO_SERVICE)).abandonAudioFocus(PlayerController.getInstance());
    }

}
