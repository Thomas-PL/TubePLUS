package be.ugent.iii.operators;

import java.util.HashMap;
import java.util.Map;

import android.telephony.TelephonyManager;

/**
 * 
 * @author Laurenz Ovaere
 */
public enum NetworkGeneration {

	UNKNOWN(0), G2(2), G2_5(2.5), G2_75(2.75), G3(3), G3_5(3.5), G_4(4);

	public double number;

	private NetworkGeneration(double number) {
		this.number = number;
	}

	public static NetworkGeneration ConvertToNetworkGeneration(int networkType) {
		if (networkType == TelephonyManager.NETWORK_TYPE_IDEN) {
			return NetworkGeneration.G2;
		} else if (networkType == TelephonyManager.NETWORK_TYPE_GPRS) {
			return NetworkGeneration.G2_5;
		} else if (networkType == TelephonyManager.NETWORK_TYPE_EDGE) {
			return NetworkGeneration.G2_75;
		} else if (networkType == TelephonyManager.NETWORK_TYPE_UMTS) {
			return NetworkGeneration.G3;
		} else if (networkType == TelephonyManager.NETWORK_TYPE_EHRPD || networkType == TelephonyManager.NETWORK_TYPE_HSDPA
				|| networkType == TelephonyManager.NETWORK_TYPE_HSPA || networkType == TelephonyManager.NETWORK_TYPE_HSPAP
				|| networkType == TelephonyManager.NETWORK_TYPE_HSUPA) {
			return NetworkGeneration.G3_5;
		} else if (networkType == TelephonyManager.NETWORK_TYPE_LTE) {
			return NetworkGeneration.G_4;
		} else
			return NetworkGeneration.UNKNOWN;
	}

	private static final Map<Double, NetworkGeneration> doubleToTypeMap = new HashMap<Double, NetworkGeneration>();
	static {
		for (NetworkGeneration generation : NetworkGeneration.values()) {
			doubleToTypeMap.put(generation.number, generation);
		}
	}

	public static NetworkGeneration fromDouble(double value) {
		NetworkGeneration generation = doubleToTypeMap.get(Double.valueOf(value));
		if (generation == null)
			return NetworkGeneration.UNKNOWN;
		return generation;
	}
}
