/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.tasks;

import android.util.Log;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.operators.AbstractOperatorThread;
import be.ugent.iii.sensors.SoundMeter;
import be.ugent.iii.service.FrameworkService;

/**
 *
 * @author Thomas
 */
public class SoundTask extends AbstractOperatorThread {

    private final SoundMeter sm;
    private static final String DYNAMIC_VOLUME_ON = "dynamicVolume";

    public SoundTask(int intervalTijd, FrameworkService frameworkService) {
        super(intervalTijd, frameworkService);
        sm = new SoundMeter();
        if (preferences.get(DYNAMIC_VOLUME_ON) != null && preferences.get(DYNAMIC_VOLUME_ON).toString().equals("true")) {
            sm.start();
        }
    }

    @Override
    protected void executeAction() {
        if (preferences.get(DYNAMIC_VOLUME_ON) != null && preferences.get(DYNAMIC_VOLUME_ON).toString().equals("true")) {
            double db = sm.getDB();
            Log.e("SoundTask", "DB is: " + db);
            if (db < 80) {
                spelerController.setVolume(2);
            } else if (db < 100) {
                spelerController.setVolume(5);
            } else if (db < 120) {
                spelerController.setVolume(8);
            } else if (db < 150) {
                spelerController.setVolume(12);
            } else {
                spelerController.setVolume(15);
            }
        }
    }

    public void registerObserver(IObserver observer) {
    }

    @Override
    public void disableOperator() {
        Log.v("WorkerThread", "Disable thread!");
        continu = false;
        sm.stop();
    }

}
