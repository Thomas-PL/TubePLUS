package be.ugent.iii.optimizer;

import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.observer.IObservable;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.operators.DeviceLoadOperatorThread;
import be.ugent.iii.operators.IFrameworkOperator;
import android.content.ContentValues;
import android.util.Log;
import java.util.ArrayList;

/**
 * Klassie die dient als tussenstuk tussen de DeviceLoadOperatorThread en de videoOptimizer.
 * Deze klasse dient als observer op die operator en zal de updatewaarden
 * periodiek doorspelen naar de optimizer.
 * @author Thomas
 */
public class DeviceLoadBuffer implements IObserver {

    private VideoOptimizer videoOptimizer;
    private static final String TAG = "DeviceLoadBuffer";

    /*
     * Aangezien de variaties van CPU-gebruik zeer groot zijn, bepalen we het
     * gemiddelde over x aantal keer. Dit is een betere indicatie dan een
     * momentopname.
     */
    private int numberOfTimes;
    private int currentNumber = 0;
    private float totalCpuUsage = 0;
    // Bufferinfomatie voor deze klasse:
    private boolean lowOnMemory = false;
    private float averageCpuUsage = 0;

    /**
     * Constructor.
     * Registreer de klasse als observer aan de deviceLoadOperatorThread.
     * @param videoOptimizer
     * @param operators
     * @param numberOfTimes 
     */
    public DeviceLoadBuffer(VideoOptimizer videoOptimizer, ArrayList<IFrameworkOperator> operators, int numberOfTimes) {
        this.videoOptimizer = videoOptimizer;
        this.numberOfTimes = numberOfTimes;

        // Registreren als luisteraar bij het framework:
        for (IObservable operator : operators) {
            if (operator instanceof DeviceLoadOperatorThread) {
                operator.registerObserver(this);
            }
        }
    }

    /*
     * LowOnMemory zorgt niet voor een andere videokwaliteit, aangezien een andere
     * videokwaliteit heel weining impact heeft op het geheugengebruik. Het is
     * enkel om een melding te geven aan de gebruiker.
     */
    /**
     * Update de huidige waarden aan de hand van de gekregen contentValues van de observable.
     * Meld deze aanpassingen aan de optimizer.
     * @param contentValues 
     */
    @Override
    public void update(ContentValues contentValues) {
		// Aangezien het ophalen van bepaalde parameters kan
        // worden uitgeschakeld, moeten we dit telkens controleren!
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_LOW_ON_MEMORY)) {
            lowOnMemory = contentValues.getAsBoolean(ParameterDatabaseController.COLUMN_LOW_ON_MEMORY);
            Log.v(TAG, "isLowOnMemory = " + lowOnMemory);
        }
        if (contentValues.containsKey(ParameterDatabaseController.COLUMN_CPU_USAGE)) {
            totalCpuUsage += contentValues.getAsFloat(ParameterDatabaseController.COLUMN_CPU_USAGE);
        }

        // Wijziging melden als het aantal keer is verstreken:
        currentNumber++;
        if (currentNumber == numberOfTimes) {
            averageCpuUsage = totalCpuUsage / numberOfTimes;
            totalCpuUsage = 0;
            currentNumber = 0; // Herinstellen

            videoOptimizer.notifyCpuMeasurement();
        }
    }

    public boolean isLowOnMemory() {
        return lowOnMemory;
    }

    public float getAverageCpuUsage() {
        return averageCpuUsage;
    }
}
