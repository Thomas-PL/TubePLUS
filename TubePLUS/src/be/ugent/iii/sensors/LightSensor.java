/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.sensors;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager.LayoutParams;

/**
 *
 * @author Thomas
 */
public class LightSensor implements SensorEventListener {

    private final SensorManager manager;
    private final Sensor light;
    private int currentLux;
    private final Activity context;
    private int brightness;

    public LightSensor(Activity c, SensorManager m) {
        context = c;
        manager = m;
        light = manager.getDefaultSensor(Sensor.TYPE_LIGHT);
        manager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onSensorChanged(SensorEvent event) {
        currentLux = (int) event.values[0];
        
        LayoutParams layoutpars = context.getWindow().getAttributes();
        if (currentLux < 20) {
            //device ligt ergens onder of zit in broekzak... in Proximitysensor zal nu timer beginnen lopen om device te locken
            brightness = 0;
        } else {
            if (currentLux < 100) {
                //low brightness, about 1/4
                brightness = 64;
            } else {
                if (currentLux < 1000) {
                    //semi bright
                    brightness = 128;
                } else {
                    if (currentLux < 5000) {
                        //almost max, about 3/4
                        brightness = 191;
                    } else {
                        //max brightness
                        brightness = 255;
                    }
                }
            }
        }
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        

        layoutpars.screenBrightness = brightness / (float) 255;

        context.getWindow().setAttributes(layoutpars);
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
