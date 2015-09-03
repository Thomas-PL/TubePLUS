package be.ugent.iii.sensors;

/**
 *
 * @author Thomas
 */
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;

public class SoundMeter {

    private static final double EMA_FILTER = 0.6;
    private static final double REFERENCE = 0.00002;

    private MediaRecorder mRecorder = null;
    private double mEMA = 0.0;

    public void start() {
        if (mRecorder == null) {
            try {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null");
                mRecorder.prepare();
                mRecorder.start();
                Log.v("Soundmeter", "started");
                mEMA = 0.0;
            } catch (IllegalStateException ex) {
                Log.e("SoundMeter", "illegalState");
            } catch (IOException ex) {
                Log.e("SoundMeter", "ioException");
            }
        }
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null) {
            double ampl = mRecorder.getMaxAmplitude();
            return (ampl / 2700.0);
        } else {
            return 0;
        }

    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }
    
    public double getDB(){
        double amp =getAmplitude();
        double db = (20 * Math.log10(amp/REFERENCE));
        return db;
    }
}
