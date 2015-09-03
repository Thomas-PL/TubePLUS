package be.ugent.iii.optimizer;

import be.ugent.iii.operators.NetworkGeneration;

/**
 *
 * @author Laurenz Ovaere
 */
public class AnalyseData {

    private boolean isWifi;
    private NetworkGeneration mobileNetworkGeneration;
    private boolean isRoaming;
    private boolean locationSpeedExceeded;

    public AnalyseData(boolean isWifi, NetworkGeneration mobileNetworkGeneration, boolean isRoaming, boolean locationSpeedExceeded) {
        super();
        this.isWifi = isWifi;
        this.mobileNetworkGeneration = mobileNetworkGeneration;
        this.isRoaming = isRoaming;
        this.locationSpeedExceeded = locationSpeedExceeded;
    }

    public boolean isWifi() {
        return isWifi;
    }

    public NetworkGeneration getMobileNetworkGeneration() {
        return mobileNetworkGeneration;
    }

    public boolean isRoaming() {
        return isRoaming;
    }

    public boolean isLocationSpeedExceeded() {
        return locationSpeedExceeded;
    }

    public void setLocationSpeedExceeded(boolean locationSpeedExceeded) {
        this.locationSpeedExceeded = locationSpeedExceeded;
    }

}
