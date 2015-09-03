package be.ugent.iii.sensors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.service.FrameworkService;
import be.ugent.iii.youtube.R;
import java.util.HashMap;

/**
 *
 * @author Thomas
 */
public class ProximitySensor implements SensorEventListener {

    private final SensorManager manager;
    private final Sensor prox;
    private final Activity context;
    private DevicePolicyManager deviceManager;
    private boolean countdown;
    private Thread t;
    private ComponentName adminComponent;
    private Handler handler;
    private HashMap<String, ?> preferences;

    public ProximitySensor(Activity c, SensorManager m) {
        context = c;
        manager = m;

        deviceManager = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);

        prox = manager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        manager.registerListener(this, prox, SensorManager.SENSOR_DELAY_NORMAL);

        handler = new Handler();
    }

    public void onSensorChanged(SensorEvent event) {
        FrameworkService service = FrameworkController.getInstance().getService();
        if (service != null) {
            preferences = service.getFrameworkPreferences();

            int proximity = (int) event.values[0];
            if (proximity == 0 && !countdown) {
                t = new Thread() {

                    @Override
                    public void run() {
                        try {
                            sleep(10000);
                            adminComponent = new ComponentName(context, Darclass.class);
                            if (!deviceManager.isAdminActive(adminComponent)) {
                                handler.post(new Runnable() {
                                    public void run() {
                                        addAdmin();
                                    }
                                });

                            } else {
                                deviceManager.lockNow();
                            }
                            countdown = false;
                        } catch (InterruptedException ex) {
                        }
                    }

                };
                t.start();
                countdown = true;
            }
            if (proximity > 0 && countdown) {
                t.interrupt();
                countdown = false;
            }
        }
    }

    private void addAdmin() {
        if (preferences.get("deviceAdmin") != null && preferences.get("deviceAdmin").toString().equals("false")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.deviceAdminWarning).setTitle("Info");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int i) {
                    di.dismiss();
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                    context.startActivityForResult(intent, 0);
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
    }

}
