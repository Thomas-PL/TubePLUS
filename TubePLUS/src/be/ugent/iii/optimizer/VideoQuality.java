package be.ugent.iii.optimizer;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * Klasse die videokwaliteiten voorstelt
 * @author Thomas
 */
public enum VideoQuality {

    //AUDIO(1), 
    VIDEO_240p(0), VIDEO_360p(1), VIDEO_480p(2), VIDEO_720p(3), VIDEO_1080p(4), ERROR(5);

    public int number;

    /**
     * Constructor
     * @param number 
     */
    private VideoQuality(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        //Andere beschrijving dan bovenaan, want youtube gebruikt andere markeringen. 
        //Bovenaan toch gewoon gelaten omdat het makkelijker te zien is welke kwaliteit er precies bedoeld wordt
        switch (number) {
//            case 0:
//                return "ERROR";
//            case 1:
//                return "AUDIO";
            case 0:
                return "VIDEO_small";
            case 1:
                return "VIDEO_medium";
            case 2:
                return "VIDEO_large";
            case 3:
                return "VIDEO_hd720";
            case 4:
                return "VIDEO_hd1080";
            case 5:
                return "ERROR";
            default:
                return super.toString();
        }
    }

    private static final Map<Integer, VideoQuality> intToTypeMap = new HashMap<Integer, VideoQuality>();

    static {
        for (VideoQuality quality : VideoQuality.values()) {
            intToTypeMap.put(quality.number, quality);
        }
    }

    /**
     * Geef een videokwaliteit terug die overeenkomt met het getal dat werd meegegeven als parameter
     * @param value
     * @return 
     */
    public static VideoQuality fromInt(int value) {
        VideoQuality quality = intToTypeMap.get(Integer.valueOf(value));
        if (quality == null) {
            Log.e("VideoQuality", "Quality with value " + value + " not found");
            return VideoQuality.ERROR;
        }
        return quality;
    }

    /**
     * Youtube-filmpjes hebben een andere textuele aanduiding van kwaliteit,
     * dus wordt deze in de methode omgezet naar de gebruikte waarden in ons framework.
     * @param quality
     * @return 
     */
    public static VideoQuality fromQualityString(String quality) {
        VideoQuality output;
        if (quality.equals("small")) {
            output = VideoQuality.VIDEO_240p;
        } else {
            if (quality.equals("medium")) {
                output = VideoQuality.VIDEO_360p;
            } else {
                if (quality.equals("large")) {
                    output = VideoQuality.VIDEO_480p;
                } else {
                    if (quality.equals("hd720")) {
                        output = VideoQuality.VIDEO_720p;
                    } else {
                        output = VideoQuality.VIDEO_1080p;
                    }
                }
            }
        }

        return output;
    }
    
    public static VideoQuality getMaxQuality()
    {
        return VIDEO_1080p;
    }
}
