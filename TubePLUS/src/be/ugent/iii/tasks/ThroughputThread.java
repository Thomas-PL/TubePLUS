/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.tasks;

import android.util.Log;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.controllers.PlayerController;

/**
 *
 * @author Thomas
 */
public class ThroughputThread extends Thread {

    private final PlayerController spelerController;
    private int startpunt;
    private CalculateThroughputTask task;

    public ThroughputThread(int start) {
        spelerController = PlayerController.getInstance();
        startpunt = start;

    }

    @Override
    public void run() {
        Log.v("ThroughputThread", "Thread started");
        try {
            int duration = 0;
            while (duration == 0) {
                duration = spelerController.getDuration();
            }
            startpunt = startpunt * 1000;
            duration = duration * 1000;
            if ((duration - startpunt) > 0) {
                calculateThroughput();

                sleep((duration - startpunt) / 3);
                calculateThroughput();

                sleep((duration - startpunt) / 3);
                calculateThroughput();
            }

            interrupt();
        } catch (InterruptedException ex) {
//            Log.e("ThroughputThread", "Thread stopped");
        }
    }

    private void calculateThroughput() {
        if (task == null || task.isFinished()) {
            task = new CalculateThroughputTask();
            task.execute();
        }
    }
}
