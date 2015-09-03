package be.ugent.iii.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.operators.LocationOperator;

/**
 * Activity die gelaunched wordt als de gps niet enabled staat.
 * @author Thomas
 */
public class EnableGPSActivity extends Activity {

    /**
     * Called when the activity is first created.
     *
     * @param icicle
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(myIntent, 10);
        
    }

    /**
     * Als de gebruiker terugkeert van de settings waarin de gps misschien is aangezet,
     * wordt er nog een controle gevoerd of deze effectief werd aangezet.
     * Indien dit het geval was, wordt deze enabled in het framework. Anders niet.
     * @param requestCode
     * @param resultCode
     * @param data 
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LocationOperator lo = FrameworkController.getInstance().getLocationOperator();
        if (lo.checkIfIsAvailable()) {
            lo.enableOperator();
        }
        lo.disableDialog();
        finish();
    }
}
