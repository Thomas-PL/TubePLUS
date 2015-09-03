package be.ugent.iii.optimizer;

import java.util.Timer;

/**
 * 
 * @author Laurenz Ovaere
 */
public class ScheduleTimerSingleton {

	private static Timer delayTimer;

	private ScheduleTimerSingleton() {
	}

	public static synchronized Timer getInstance() {
		if (delayTimer == null) {
			delayTimer = new Timer();
		}
		return delayTimer;
	}

	public static void removeTasks() {
		if (delayTimer != null) {
			delayTimer.cancel();
			delayTimer = null;
		}
	}
}
