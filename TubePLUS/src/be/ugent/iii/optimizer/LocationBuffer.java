package be.ugent.iii.optimizer;

import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.observer.IObservable;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.operators.IFrameworkOperator;
import be.ugent.iii.operators.LocationOperator;
import android.content.ContentValues;
import android.util.Log;
import java.util.ArrayList;
import java.util.TimerTask;

/**
 * Klassie die dient als tussenstuk tussen de locationOperator en de videoOptimizer.
 * Deze klasse dient als observer op die operator en zal de updatewaarden
 * periodiek doorspelen naar de optimizer.
 * @author Thomas
 */
public class LocationBuffer implements IObserver {

    private static final String TAG = "LocationBuffer";
    private VideoOptimizer videoOptimizer;
    private long delayTime; // in ms!!!
    private TimerTask currentTask;

    // Bufferinfomatie voor deze klasse:
    private double speed = 0;

    public LocationBuffer(VideoOptimizer videoOptimizer, ArrayList<IFrameworkOperator> operators, int locationInterval) {
        this.videoOptimizer = videoOptimizer;
        // Registreren als luisteraar bij het framework:
        for (IObservable operator : operators) {
            if (operator instanceof LocationOperator) {
                operator.registerObserver(this);
            }
        }

        // Delaytime uit voorkeuren van framework halen.
        delayTime = locationInterval * 1000; // locationInterval is in seconden,
        // x1000 om ms te bekomen!
    }

	// Locatiegegevens worden sowieso allemaal opgehaald,
    // wanneer een update wordt verstuurd. We moeten
    // dus niet controleren of deze wel in de contentValues zitten!
    @Override
    public void update(ContentValues contentValues) {
        // We stoppen de timer als hij de snelheid op nul zou brengen:
        if (currentTask != null) {
            currentTask.cancel();
        }

        speed = contentValues.getAsDouble(ParameterDatabaseController.COLUMN_SPEED);
        Log.v(TAG, "speed = " + speed);

		// Wanneer de gebruiker plots stopt, dan zijn er geen locatie-updates
        // meer. De snelheid moet echter worden aangepast naar nul.
        // Dit gebeurt nadat de tijd is verstreken waarop normaalgezien
        // (bij het voortbewegen) een nieuwe locatie-update moet volgen.
        // Als die update uitblijft, dan wordt de snelheid terug op nul
        // geplaatst.
        ScheduleTimerSingleton.getInstance().schedule(currentTask = new TimerTask() {

            @Override
            public void run() {
                speed = 0;
                videoOptimizer.notifyChange();
                currentTask = null;
            }
        }, delayTime + 500); // 500 ms vertragingstijd.

        videoOptimizer.notifyChange();
    }

    public double getSpeed() {
        return speed * 3.6; // in km/h
    }

}
