package be.ugent.iii.operators;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import be.ugent.iii.activities.EnableGPSActivity;
import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.database.LocationInfoCommand;
import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.service.FrameworkService;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Klasse die instaat voor het ophalen van informatie over locatie.
 * Deze info zal gemeld worden aan de abonees.
 * @author Thomas
 */
public class LocationOperator implements IFrameworkOperator {

    private HashMap<String, ?> preferences;
    private int intervalTijd; // in seconden
    private int preferenceTijd; // in seconden
    private FrameworkService frameworkService;
    private LocationManager locationManager;
    private LocationListener listener;
    private LocationInfoCommand locationInfoCommand;
    private static final String[] items = {"gpsProvider", "registerLocationUpdates"};
    private static final String MINIMUM_DISTANCE = "gpsDistance";
    private ArrayList<IObserver> observers;

    private boolean dialog;

    /**
     * Constructor.
     * @param intervalTijd
     * @param frameworkService 
     */
    public LocationOperator(int intervalTijd, FrameworkService frameworkService) {
        this.preferenceTijd = intervalTijd;
        this.frameworkService = frameworkService;
        this.preferences = frameworkService.getAppPreferences();
        
        Log.e("LocationOperator", "preferences size " + preferences.size());

        FrameworkController.getInstance().setLocationOperator(this);
        // Tijdelijk hoge waarde voor intitialisatie:
        this.intervalTijd = 1;

        locationManager = (LocationManager) frameworkService.getBaseContext().getSystemService(Context.LOCATION_SERVICE);

        locationInfoCommand = new LocationInfoCommand();
        observers = new ArrayList<IObserver>();

        dialog = false;
    }

    /**
     * Controle welke locatieprovider er kan gebruikt worden.
     * @return 
     */
    public boolean checkIfIsAvailable() {
        String provider = preferences.get(items[0]).toString();
        if (provider.equals(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.e("locationoperator","GPS_PROVIDER");
            return true;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.e("locationoperator","NETWORK_PROVIDER");
            return true;
        } else if (provider.equals(LocationManager.PASSIVE_PROVIDER) && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            Log.e("locationoperator","PASSIEVE_PROVIDER");
            
            return true;
        } else {
            Log.e("locationoperator","NO GPS");
            return false;
        }
    }

    /**
     * Schakel de operator in.
     * Eerst volgt een controle of de gps module wel aan staat. Indien dit niet het geval is,
     * wordt de gebruiker gevraagd of deze moet ingeschakeld worden.
     */
    @Override
    public void enableOperator() {
        // Controleer of locatie-updates gevraagd zijn:
        if (preferences.get(items[1]) != null && preferences.get(items[1]).toString().equals("true")) {
            // GPS setup:
            String provider = preferences.get(items[0]).toString();
            if (checkIfIsAvailable()) {
                Log.v("LocationOperator", provider + " enabled");
                registerLocationUpdates(provider);
            } else {
                Log.v("LocationOperator", "Gevraagde GPS-provider niet beschikbaar (" + provider + ")");
                if (!dialog) {
                    dialog = true;
                    final Context c = FrameworkController.getInstance().geefContext();
                    AlertDialog.Builder builder = new AlertDialog.Builder(c);
                    builder.setMessage("Your GPS module is disabled. Would you like to enable it ?")
                            .setCancelable(false)
                            .setPositiveButton("Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                int id) {
                                            Intent myIntent = new Intent(c, EnableGPSActivity.class);
                                            c.startActivity(myIntent);
                                            dialog.dismiss();
                                        }
                                    })
                            .setNegativeButton("No",
                                    new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog,
                                                int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .show();
                }
            }
        }
    }

    /**
     * Eens gevraagd moet het de volgende keer niet meer gevraagd worden.
     */
    public void disableDialog() {
        dialog = false;
    }

    @Override
    public void disableOperator() {
        if (listener != null) {
            locationManager.removeUpdates(listener);
        }
    }

    private Location previous;

    /**
     * zorg ervoor dat updates over locatie worden geregistreerd.
     * @param provider 
     */
    private void registerLocationUpdates(String provider) {
        Log.v("LocationOperator", "registerLocationupdates");
        int minimumDistance = Integer.parseInt(preferences.get(MINIMUM_DISTANCE).toString());
        // Interval wordt vermenigvuldigd met 1000, aangezien de waarde
        // wordt verkregen in seconden, maar de functie verwacht
        // milliseconden!!!
        locationManager.requestLocationUpdates(provider, intervalTijd * 1000, minimumDistance, listener = new LocationListener() {

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onLocationChanged(Location location) {
                float speed = 0;
                if (previous != null) {
                    long distance = calculateDistance(previous.getLatitude(), previous.getLongitude(), location.getLatitude(), location.getLongitude());
                    speed = distance / ((location.getTime() - previous.getTime()) / 1000);
                }

                // Databasetransactie mag niet uitgevoerd worden
                // vanaf de main-thread!
                previous = location;
                //in m/s
                location.setSpeed(speed);
                new LocationThread(location).start();
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }
        });
    }

    public void setPreferenceTijd() {
        this.intervalTijd = preferenceTijd;
        disableOperator();
        enableOperator();
    }

    @Override
    public void registerObserver(IObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(ContentValues contentValues) {
        for (IObserver observer : observers) {
            observer.update(contentValues);
        }
    }

    /**
     * Bereken de afgelegde afstand tussen 2 metingen.
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return 
     */
    private static long calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        long distanceInMeters = Math.round(6371000 * c);
        return distanceInMeters;
    }

    /**
     * Telkens een verandering in locatie werd waargenomen, zal deze thread
     * alle informatie ophalen en deze laten weten aan zijn abonees.
     * De info zal ook weggeschreven worden naar de lokale databank, via
     * de locationInfoCommand.
     */
    class LocationThread extends Thread {

        private Location location;

        public LocationThread(Location location) {
            this.location = location;
        }

        @Override
        public void run() {
            Looper.prepare();
            ContentValues values = new ContentValues();
            values.put(ParameterDatabaseController.COLUMN_SESSION_ID, frameworkService.getSessionIdentifier());
            values.put(ParameterDatabaseController.COLUMN_TIMESTAMP, FrameworkApplication.getTimestamp());
            values.put(ParameterDatabaseController.COLUMN_LONGITUDE, location.getLongitude());
            values.put(ParameterDatabaseController.COLUMN_LATITUDE, location.getLatitude());
            values.put(ParameterDatabaseController.COLUMN_SPEED, location.getSpeed());
            locationInfoCommand.addLocationLog(values);
            notifyObservers(values);
        }
    }
}
