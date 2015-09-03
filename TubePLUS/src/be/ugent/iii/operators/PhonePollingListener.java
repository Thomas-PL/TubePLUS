package be.ugent.iii.operators;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

/**
 * 
 * @author Laurenz Ovaere
 */

// Deze klasse zal worden gebruikt om de signaalsterkte op
// te vragen via polling. Hiervoor wordt een eventueel
// nieuwe waarde (verkregen door de luisteraar) gewoon bijgehouden.
public class PhonePollingListener extends PhoneStateListener {

	private SignalStrength mSignalStrength;
	private boolean phoneInService = false;
	private TelephonyManager telephonyManager;

	public PhonePollingListener(Context context) {
		telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}

	public void enableListener() {
		// Registreer luisteraar:
		telephonyManager.listen(this, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
	}

	public void disableListener() {
		// Deactiveer luisteraar:
		telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
	}

	@Override
	public void onSignalStrengthsChanged(SignalStrength signalStrength) {
		// De signaalsterkte
		mSignalStrength = signalStrength;
	}

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
	}

	public boolean isPhoneInService() {
		return phoneInService;
	}

	public int getGsmBitErrorRate() {
		if (mSignalStrength != null) {
			return mSignalStrength.getGsmBitErrorRate();
		} else {
			return -1;
		}
	}

	public int getGsmAsuLevel() {
		if (mSignalStrength != null) {
			return mSignalStrength.getGsmSignalStrength();
		} else {
			return -1;
		}
	}

	public int getGsmLevel() {
		if (mSignalStrength != null) {
			int asu = mSignalStrength.getGsmSignalStrength();
			int level = 0;
			// ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
			// asu = 0 (-113dB or less) is very weak
			// signal, its better to show 0 bars to the user in such cases.
			// asu = 99 is a special case, where the signal strength is
			// unknown.
			if (asu <= 2 || asu == 99)
				level = 0;
			else if (asu >= 12)
				level = 4;
			else if (asu >= 8)
				level = 3;
			else if (asu >= 5)
				level = 2;
			else
				level = 1;
			return level;
		} else {
			return -1;
		}
	}

	public int getCdmaLevel() {
		if (mSignalStrength != null) {
			final int cdmaDbm = mSignalStrength.getCdmaDbm();
			final int cdmaEcio = mSignalStrength.getCdmaEcio();
			int levelDbm = 0;
			int levelEcio = 0;

			if (cdmaDbm >= -75)
				levelDbm = 4;
			else if (cdmaDbm >= -85)
				levelDbm = 3;
			else if (cdmaDbm >= -95)
				levelDbm = 2;
			else if (cdmaDbm >= -100)
				levelDbm = 1;
			else
				levelDbm = 0;

			// Ec/Io are in dB*10
			if (cdmaEcio >= -90)
				levelEcio = 4;
			else if (cdmaEcio >= -110)
				levelEcio = 3;
			else if (cdmaEcio >= -130)
				levelEcio = 2;
			else if (cdmaEcio >= -150)
				levelEcio = 1;
			else
				levelEcio = 0;

			return (levelDbm < levelEcio) ? levelDbm : levelEcio;
		} else {
			return -1;
		}
	}

	public int getEvdoLevel() {
		if (mSignalStrength != null) {
			int evdoDbm = mSignalStrength.getEvdoDbm();
			int evdoSnr = mSignalStrength.getEvdoSnr();
			int levelEvdoDbm = 0;
			int levelEvdoSnr = 0;

			if (evdoDbm >= -65)
				levelEvdoDbm = 4;
			else if (evdoDbm >= -75)
				levelEvdoDbm = 3;
			else if (evdoDbm >= -90)
				levelEvdoDbm = 2;
			else if (evdoDbm >= -105)
				levelEvdoDbm = 1;
			else
				levelEvdoDbm = 0;

			if (evdoSnr >= 7)
				levelEvdoSnr = 4;
			else if (evdoSnr >= 5)
				levelEvdoSnr = 3;
			else if (evdoSnr >= 3)
				levelEvdoSnr = 2;
			else if (evdoSnr >= 1)
				levelEvdoSnr = 1;
			else
				levelEvdoSnr = 0;

			return (levelEvdoDbm < levelEvdoSnr) ? levelEvdoDbm : levelEvdoSnr;
		} else {
			return -1;
		}
	}

	public boolean isGsmNetwork() {
		if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM)
			return true;
		else
			return false;
	}

}
