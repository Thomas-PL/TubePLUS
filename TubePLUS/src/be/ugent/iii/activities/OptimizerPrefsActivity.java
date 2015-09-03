package be.ugent.iii.activities;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import be.ugent.iii.youtube.R;

/**
 * Activity voor de preferences van de optimizer te bekijken.
 * TODO: omzetten naar fragments
 * @author Thomas
 */
public class OptimizerPrefsActivity extends PreferenceActivity {

    public static final String OPTIMIZER_FILE = "optimizer_preferences";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		// Instellen welke file er precies moet worden gebruikt:
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(OPTIMIZER_FILE);
        addPreferencesFromResource(R.xml.optimizer_preferences);
    }

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
		if (preference != null) {
			if (preference instanceof PreferenceScreen) {
				if (((PreferenceScreen) preference).getDialog() != null) {
					((PreferenceScreen) preference).getDialog().getWindow().getDecorView()
							.setBackgroundDrawable(this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
				}
			}
		}
		return false;
	}
}
