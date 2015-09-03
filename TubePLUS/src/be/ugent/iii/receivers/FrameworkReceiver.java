package be.ugent.iii.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.controllers.PlayerController;
import be.ugent.iii.service.FrameworkService;
import java.util.HashMap;

/**
 * Broadcastreceiver klasse die checkt of de headset wordt uitgetrokken of niet.
 * @author Thomas
 */
public class FrameworkReceiver extends BroadcastReceiver {

    private final PlayerController spelerController;
    private final HashMap<String, ?> preferences;
    private final FrameworkService service;
    private static final String UNPLUG_ON = "plugHeadset";

    /**
     * Constructor
     */
    public FrameworkReceiver() {
        service = FrameworkController.getInstance().getService();
        spelerController = PlayerController.getInstance();
        preferences = service.getFrameworkPreferences();
    }

    /**
     * Als de headset wordt uitgetrokken, wordt deze methode getriggered.
     * Indien in de settings werd gespecifieerd dat de playback moet stoppen, zal dit ook gebeuren.
     * @param cntxt
     * @param intent 
     */
    @Override
    public void onReceive(Context cntxt, Intent intent) {
        if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            if (preferences.get(UNPLUG_ON) != null && preferences.get(UNPLUG_ON).toString().equals("true")) {
                spelerController.pauseVideo();
            }
        }
        
        if(intent.getAction().equals(Intent.ACTION_BATTERY_LOW)){
            spelerController.setBatteryLow(true);
        }
        
        if(intent.getAction().equals(Intent.ACTION_BATTERY_OKAY)){
            spelerController.setBatteryLow(false);
        }
    }

}
