package be.ugent.iii.operators;

import be.ugent.iii.service.FrameworkService;
import java.util.HashMap;

import android.util.Log;
import be.ugent.iii.controllers.PlayerController;

/**
 * 
 * @author Laurenz Ovaere
 */
public abstract class AbstractOperatorThread extends Thread implements IFrameworkOperator {

	protected boolean continu;
	private int intervalTijd; // in seconden
	private int preferenceTijd; // in seconden
	protected FrameworkService frameworkService;
        protected PlayerController spelerController;
	protected HashMap<String, ?> preferences;

	public AbstractOperatorThread(int intervalTijd, FrameworkService frameworkService) {
		super();
		this.continu = true;
		this.preferenceTijd = intervalTijd;
		/*
		 * Tijdelijk hoge waarde voor de initialisatie. Daarna wordt de tijd van
		 * de preferences overgenomen.
		 */
		this.intervalTijd = 1;
		this.frameworkService = frameworkService;
		this.preferences = frameworkService.getAppPreferences();
                spelerController = PlayerController.getInstance();
	}

	@Override
	public void run() {
		super.run();
		// Controleer of het ophaalproces nog mag doorgaan:
		while (continu) {
			// Volgend tijdstip bepalen waarop de controle en het ophalen
			// gebeurt:
			long endTime = System.currentTimeMillis() + intervalTijd * 1000;

			// Werk uitvoeren:
			executeAction();

			// Als het werk minder tijd in beslag heeft genomen,
			// eventueel wachten vooraleer terug uit te voeren:
			while (System.currentTimeMillis() < endTime && continu) {
				synchronized (this) {
					try {
						wait(endTime - System.currentTimeMillis());
					} catch (Exception e) {
					}
				}
			}
		}
	}

	public void enableOperator() {
		this.start();
	}

	public void disableOperator() {
		Log.v("WorkerThread", "Disable thread!");
		continu = false;
	}

	public void setPreferenceTijd() {
		this.intervalTijd = preferenceTijd;
	}

	protected abstract void executeAction();

}
