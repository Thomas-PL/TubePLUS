package be.ugent.iii.operators;

import be.ugent.iii.application.FrameworkApplication;
import be.ugent.iii.controllers.WifiController;
import be.ugent.iii.database.ParameterDatabaseController;
import be.ugent.iii.database.QosInfoCommand;
import be.ugent.iii.observer.IObserver;
import be.ugent.iii.service.FrameworkService;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import be.ugent.iii.controllers.FrameworkController;
import java.util.ArrayList;

/**
 * 
 * @author Laurenz Ovaere
 */
public class QosOperatorThread extends AbstractOperatorThread implements IFrameworkOperator {

	public static final String[] items = { "logConnectionType", "getMobileSignalInfo", "getWifiSignalInfo" };
	private ConnectivityManager connectivityManager;
	private WifiController wifiHelper;
	private PhonePollingListener phonePollingListener;
	private QosInfoCommand qosInfoCommand;
	private ArrayList<IObserver> observers;

	public QosOperatorThread(int intervalTijd, FrameworkService frameworkService) {
		super(intervalTijd, frameworkService);
		connectivityManager = (ConnectivityManager) frameworkService.getSystemService(Context.CONNECTIVITY_SERVICE);
		phonePollingListener = new PhonePollingListener(frameworkService);
		wifiHelper = WifiController.getInstance(FrameworkController.getInstance().geefContext());
		qosInfoCommand = new QosInfoCommand();
		observers = new ArrayList<IObserver>();
	}

	@Override
	public void enableOperator() {
		phonePollingListener.enableListener();
		super.enableOperator();
	}

	@Override
	public void disableOperator() {
		phonePollingListener.disableListener();
		super.disableOperator();
	}

	@Override
	protected void executeAction() {
		// Key-value-map aanmaken:
		ContentValues values = new ContentValues();

		values.put(ParameterDatabaseController.COLUMN_SESSION_ID, super.frameworkService.getSessionIdentifier());
		values.put(ParameterDatabaseController.COLUMN_TIMESTAMP, FrameworkApplication.getTimestamp());

		if (preferences.get(items[0]) != null && super.preferences.get(items[0]).toString().equals("true")) {
			boolean isWifi = wifiHelper.isWifi();
			values.put(ParameterDatabaseController.COLUMN_WIFI_STATE_ACTIVE, isWifi);

			boolean phoneActive = phonePollingListener.isPhoneInService();
			values.put(ParameterDatabaseController.COLUMN_PHONE_STATE_ACTIVE, phoneActive);

			boolean dataAvailable = dataAvailable();
			values.put(ParameterDatabaseController.COLUMN_MOBILE_DATA_AVAILABLE, dataAvailable);

			boolean isRoaming = isRoaming();
			values.put(ParameterDatabaseController.COLUMN_MOBILE_ROAMING, isRoaming);

		}

		// Informatie mobiel netwerk:
		if (preferences.get(items[1]) != null && super.preferences.get(items[1]).toString().equals("true") && phonePollingListener.isPhoneInService()) {

			double generationNumber = 0;
			if (dataAvailable()) {
				generationNumber = getNetworkGeneration();
				values.put(ParameterDatabaseController.COLUMN_MOBILE_GENERATION, generationNumber);

				String connectionTypeDescription;
				connectionTypeDescription = getNetworkTypeDescription();
				values.put(ParameterDatabaseController.COLUMN_MOBILE_CONNECTION_TYPE, connectionTypeDescription);
			}

			if (phonePollingListener.isGsmNetwork()) {
				int signalLevel = phonePollingListener.getGsmLevel();
				values.put(ParameterDatabaseController.COLUMN_MOBILE_SIGNAL_LEVEL, signalLevel);

				int asu = phonePollingListener.getGsmAsuLevel();
				values.put(ParameterDatabaseController.COLUMN_MOBILE_ASU, asu);

				int ber = phonePollingListener.getGsmBitErrorRate();
				values.put(ParameterDatabaseController.COLUMN_MOBILE_BER, ber);

			} else {
				int signalLevel = 0;
				if (generationNumber > 2)
					signalLevel = phonePollingListener.getEvdoLevel();
				else
					phonePollingListener.getCdmaLevel();
				values.put(ParameterDatabaseController.COLUMN_MOBILE_SIGNAL_LEVEL, signalLevel);
			}
		}

		// Informatie WiFi:
		if (preferences.get(items[2]) != null && super.preferences.get(items[2]).toString().equals("true") && wifiHelper.isWifi()) {

			int rssi = wifiHelper.getWifiRssi();
			int asu = (rssi + 113) / 2;
			int signalLevel = WifiManager.calculateSignalLevel(rssi, 4);
			int linkSpeed = wifiHelper.getWifiLinkSpeed();

			values.put(ParameterDatabaseController.COLUMN_WIFI_SIGNAL_LEVEL, signalLevel);
			values.put(ParameterDatabaseController.COLUMN_WIFI_RSSI, rssi);
			values.put(ParameterDatabaseController.COLUMN_WIFI_ASU, asu);
			values.put(ParameterDatabaseController.COLUMN_WIFI_LINKSPEED, linkSpeed);
		}
                
                // Informatie throughput
//                int tp = (int)NetworkParameterController.getInstance().getThroughput();
//                values.put(ParameterDatabaseController.COLUMN_THROUGHPUT, tp);
                

		// Voeg logging toe aan databank:
		qosInfoCommand.addQosLog(values);
		notifyObservers(values);
	}

	private boolean isRoaming() {
		android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isRoaming()) {
			return true;
		} else {
			return false;
		}
	}

	private double getNetworkGeneration() {
		// Wordt enkel en alleen teruggegeven als we niet met WiFi, maar
		// met een mobiele dataverbinding verbonden zijn:
		TelephonyManager telephonyManager = (TelephonyManager) super.frameworkService.getSystemService(Context.TELEPHONY_SERVICE);
		if (phonePollingListener.isPhoneInService() && dataAvailable()) {
			NetworkGeneration gen = NetworkGeneration.ConvertToNetworkGeneration(telephonyManager.getNetworkType());
			return gen.number;
		} else {
			return NetworkGeneration.UNKNOWN.number;
		}
	}

	private boolean dataAvailable() {
		TelephonyManager telephonyManager = (TelephonyManager) super.frameworkService.getSystemService(Context.TELEPHONY_SERVICE);
		int dataState = telephonyManager.getDataState();
		if (dataState == TelephonyManager.DATA_CONNECTED || dataState == TelephonyManager.DATA_CONNECTING) {
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

	@Override
	public void registerObserver(IObserver observer) {
		observers.add(observer);
	}

	private void notifyObservers(ContentValues contentValues) {
		for (IObserver observer : observers) {
			observer.update(contentValues);
		}
	}

	public ArrayList<IObserver> getObservers() {
		return observers;
	}

}
