/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.service.FrameworkService;
import be.ugent.iii.youtube.R;
import java.util.HashMap;

/**
 *
 * @author Thomas
 */
public class TogglePreference extends EditTextPreference {

    // Map voor kwaliteitsvoorkeuren:
    private HashMap<String, ?> optimizer_preferences;
    private final String TAG = "TogglePreference";
    private static final String APP_FILE = "app_preferences";
    private Context ctx;

    public TogglePreference(Context context) {
        super(context);
        ctx=context;
    }

    public TogglePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        ctx=context;
    }

    public TogglePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        ctx=context;
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new LinearLayout(getContext());
            ((LinearLayout) convertView)
                    .setOrientation(LinearLayout.HORIZONTAL);

            TextView txtInfo = new TextView(getContext());

            txtInfo.setText(R.string.quality_performance);
            ((LinearLayout) convertView).addView(txtInfo,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            final FrameworkService frameworkService = FrameworkController.getInstance().getService();
            //in commentaar omdat deze optie is verzet naar private preferences voor tijdens de testfase
//SharedPreferences optimizerPrefs = frameworkService.getSharedPreferences(OptimizerPrefsActivity.OPTIMIZER_FILE, Context.MODE_PRIVATE);
            SharedPreferences appPrefs = frameworkService.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
//            optimizer_preferences = (HashMap<String, ?>) optimizerPrefs.getAll();   
            optimizer_preferences = (HashMap<String, ?>) appPrefs.getAll();

            int ischecked = Integer.parseInt(optimizer_preferences.get("quality_performance").toString());
            
            final ToggleButton btn = new ToggleButton(getContext());
            btn.setTextOff(ctx.getString(R.string.performance));
            btn.setTextOn(ctx.getString(R.string.quality));

            switch (ischecked) {
                case 0:
                    btn.setChecked(false);

                    break;
                case 1:
                    btn.setChecked(true);
                    break;
            }

            btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                public void onCheckedChanged(CompoundButton cb, boolean bln) {
                    SharedPreferences optimizerPreferences = frameworkService.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = optimizerPreferences.edit();
                    if (btn.isChecked()) {
                        //Quality
                        editor.putString("quality_performance", Integer.toString(1));
                    } else {
                        //Performance
                        editor.putString("quality_performance", Integer.toString(0));
                    }
                    editor.commit();
                }
            });
            ((LinearLayout) convertView).addView(btn);
        }

        return convertView;
    }
}
