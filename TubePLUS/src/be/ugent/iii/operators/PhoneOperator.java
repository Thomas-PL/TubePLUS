package be.ugent.iii.operators;

import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.database.QosInfoCommand;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.service.FrameworkService;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Laurenz Ovaere
 */
public class PhoneOperator implements IFrameworkOperator {

    private HashMap<String, ?> preferences;
    private FrameworkService frameworkService;
    private PhoneListener phoneListener;
    private TelephonyManager telephonyManager;
    private ConnectivityManager connectivityManager;
    private SignalStrength mSignalStrength;
    private QosInfoCommand qosInfoCommand;
    // Buffer info:
    private boolean dataAvailable = false;
    private boolean phoneInService = false;
    private double networkGeneration = 0;
    private ArrayList<IObserver> observers;

    public PhoneOperator(FrameworkService frameworkService, ArrayList<IObserver> observers) {
        this.frameworkService = frameworkService;
        this.observers = observers;
        this.preferences = frameworkService.getAppPreferences();

        telephonyManager = (TelephonyManager) frameworkService.getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        connectivityManager = (ConnectivityManager) frameworkService.getSystemService(Context.CONNECTIVITY_SERVICE);
        phoneListener = new PhoneListener();

        qosInfoCommand = new QosInfoCommand();
        if (observers == null) {
            observers = new ArrayList<IObserver>();
        }
    }

    @Override
    public void enableOperator() {
        // Registreer luisteraar:
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SERVICE_STATE
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_DATA_ACTIVITY);
    }

    @Override
    public void disableOperator() {
        // Deactiveer luisteraar:
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
    }

    private class PhoneListener extends PhoneStateListener {

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            // Netwerk beschikbaar?
            int state = serviceState.getState();
            switch (state) {
                case ServiceState.STATE_IN_SERVICE:
                    phoneInService = true;
                    break;
                case ServiceState.STATE_EMERGENCY_ONLY:
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    phoneInService = false;
                    break;
            }
            logChanges();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            // Hier kan je opvangen dat de verbinding wordt verbroken of opgezet
            switch (state) {
                case TelephonyManager.DATA_CONNECTED:
                    dataAvailable = true;
                    networkGeneration = NetworkGeneration.ConvertToNetworkGeneration(networkType).number;
                    break;
                case TelephonyManager.DATA_DISCONNECTED:
                    dataAvailable = false;
                    break;
            }
            logChanges();
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSignalStrength = signalStrength;
            logChanges();
        }

        /**
         * Veranderingen wegschrijven naar DB en ook aan abonees melden.
         */
        private void logChanges() {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    // Key-value-map aanmaken:
                    ContentValues values = new ContentValues();

                    values.put(ParameterDatabaseController.COLUMN_SESSION_ID, frameworkService.getSessionIdentifier());
                    values.put(ParameterDatabaseController.COLUMN_TIMESTAMP, FrameworkApplication.getTimestamp());

                    if (preferences.get(QosOperatorThread.items[0]) != null && preferences.get(QosOperatorThread.items[0]).toString().equals("true")) {

                        values.put(ParameterDatabaseController.COLUMN_PHONE_STATE_ACTIVE, phoneInService);

                        values.put(ParameterDatabaseController.COLUMN_MOBILE_DATA_AVAILABLE, dataAvailable);

                        boolean isRoaming = isRoaming();
                        values.put(ParameterDatabaseController.COLUMN_MOBILE_ROAMING, isRoaming);
                    }

                    // Informatie mobiel netwerk:
                    if (preferences.get(QosOperatorThread.items[1]) != null && preferences.get(QosOperatorThread.items[1]).toString().equals("true")
                            && phoneInService && mSignalStrength != null) {

                        if (dataAvailable) {
                            values.put(ParameterDatabaseController.COLUMN_MOBILE_GENERATION, networkGeneration);

                            String connectionTypeDescription;
                            connectionTypeDescription = getNetworkTypeDescription();
                            values.put(ParameterDatabaseController.COLUMN_MOBILE_CONNECTION_TYPE, connectionTypeDescription);
                        }

                        if (isGsmNetwork()) {
                            int gsmLevel = getGsmLevel();
                            values.put(ParameterDatabaseController.COLUMN_MOBILE_SIGNAL_LEVEL, gsmLevel);

                            int asu = mSignalStrength.getGsmSignalStrength();
                            values.put(ParameterDatabaseController.COLUMN_MOBILE_ASU, asu);

                            int ber = mSignalStrength.getGsmBitErrorRate();
                            values.put(ParameterDatabaseController.COLUMN_MOBILE_BER, ber);

                        } else {
                            int signalLevel = 0;
                            if (networkGeneration > 2) {
                                signalLevel = getEvdoLevel();
                            } else {
                                signalLevel = getCdmaLevel();
                            }
                            values.put(ParameterDatabaseController.COLUMN_MOBILE_SIGNAL_LEVEL, signalLevel);
                        }
                    }

                    // Voeg logging toe aan databank:
                    qosInfoCommand.addQosLog(values);
                    notifyObservers(values);
                }
            }).start();
        }

        private int getGsmLevel() {
            if (mSignalStrength != null) {
                int asu = mSignalStrength.getGsmSignalStrength();
                int level = 0;
				// ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
                // asu = 0 (-113dB or less) is very weak
                // signal, its better to show 0 bars to the user in such cases.
                // asu = 99 is a special case, where the signal strength is
                // unknown.
                if (asu <= 2 || asu == 99) {
                    level = 0;
                } else if (asu >= 12) {
                    level = 4;
                } else if (asu >= 8) {
                    level = 3;
                } else if (asu >= 5) {
                    level = 2;
                } else {
                    level = 1;
                }
                return level;
            } else {
                return -1;
            }
        }

        private int getCdmaLevel() {
            if (mSignalStrength != null) {
                final int cdmaDbm = mSignalStrength.getCdmaDbm();
                final int cdmaEcio = mSignalStrength.getCdmaEcio();
                int levelDbm = 0;
                int levelEcio = 0;

                if (cdmaDbm >= -75) {
                    levelDbm = 4;
                } else if (cdmaDbm >= -85) {
                    levelDbm = 3;
                } else if (cdmaDbm >= -95) {
                    levelDbm = 2;
                } else if (cdmaDbm >= -100) {
                    levelDbm = 1;
                } else {
                    levelDbm = 0;
                }

                // Ec/Io are in dB*10
                if (cdmaEcio >= -90) {
                    levelEcio = 4;
                } else if (cdmaEcio >= -110) {
                    levelEcio = 3;
                } else if (cdmaEcio >= -130) {
                    levelEcio = 2;
                } else if (cdmaEcio >= -150) {
                    levelEcio = 1;
                } else {
                    levelEcio = 0;
                }

                return (levelDbm < levelEcio) ? levelDbm : levelEcio;
            } else {
                return -1;
            }
        }

        private int getEvdoLevel() {
            if (mSignalStrength != null) {
                int evdoDbm = mSignalStrength.getEvdoDbm();
                int evdoSnr = mSignalStrength.getEvdoSnr();
                int levelEvdoDbm = 0;
                int levelEvdoSnr = 0;

                if (evdoDbm >= -65) {
                    levelEvdoDbm = 4;
                } else if (evdoDbm >= -75) {
                    levelEvdoDbm = 3;
                } else if (evdoDbm >= -90) {
                    levelEvdoDbm = 2;
                } else if (evdoDbm >= -105) {
                    levelEvdoDbm = 1;
                } else {
                    levelEvdoDbm = 0;
                }

                if (evdoSnr >= 7) {
                    levelEvdoSnr = 4;
                } else if (evdoSnr >= 5) {
                    levelEvdoSnr = 3;
                } else if (evdoSnr >= 3) {
                    levelEvdoSnr = 2;
                } else if (evdoSnr >= 1) {
                    levelEvdoSnr = 1;
                } else {
                    levelEvdoSnr = 0;
                }

                return (levelEvdoDbm < levelEvdoSnr) ? levelEvdoDbm : levelEvdoSnr;
            } else {
                return -1;
            }
        }

        private boolean isRoaming() {
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isRoaming()) {
                return true;
            } else {
                return false;
            }
        }

        private String getNetworkTypeDescription() {
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            if (activeNetwork != null) {
                return activeNetwork.getSubtypeName();
            } else {
                return null;
            }
        }

        public boolean isGsmNetwork() {
            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                return true;
            } else {
                return false;
            }
        }

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

}
