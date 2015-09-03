package be.ugent.iii.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import be.ugent.iii.controllers.FrameworkController;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class CalculateThroughputTask extends AsyncTask<Void, Void, Long> {

    private FrameworkController parameterController;
    private boolean isFinished;

    @Override
    protected void onPreExecute() {
       isFinished = false;
    }

    @Override
    protected Long doInBackground(Void... params) {
        parameterController = FrameworkController.getInstance();
        long time = download_Image("http://nmap.org/movies/matrix/trinity-hacking-hd.png");
        return time;
    }

    @Override
    protected void onPostExecute(Long result) {
        isFinished = true;
        parameterController.setThroughput(result);
    }
    
    public boolean isFinished(){
        return isFinished;
    }

    private Long download_Image(String url) {        
        long bandwidth = 0;
        try {
            long startTime = System.currentTimeMillis();
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            //*8 voor bits ipv bytes
            long length = conn.getContentLength() * 8;
            Log.v("TP", "length: " + length);
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
            long endtime = System.currentTimeMillis();
            long timediff = (endtime - startTime);
            Log.v("TP", "timediff: " + timediff);
            //Afbeelding is in 10^6, timediff is 10^-3 dus is resultaat in kilobits
            bandwidth = (length) / timediff;

            Log.v("TP", "width: " + bandwidth);
        } catch (IOException e) {
            Log.e("ThroughPutTask", "Error getting the image from server : " + e.getMessage());
        } catch (OutOfMemoryError e){
            Log.e("ThroughPutTask", "Device out of memory...");
        }
        return bandwidth;
    }
}
