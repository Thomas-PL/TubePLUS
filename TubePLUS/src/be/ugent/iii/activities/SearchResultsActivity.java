package be.ugent.iii.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import be.ugent.iii.controllers.FrameworkController;
import be.ugent.iii.youtube.R;

/**
 * Activity voor de search-widget.
 * @author Thomas
 */
public class SearchResultsActivity extends Activity {
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
 
        // get the action bar
        ActionBar actionBar = getActionBar();
        actionBar.hide(); 
        handleIntent(getIntent());
    }
 
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }
 
    /**
     * Handling intent data
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);           
            WebView view = FrameworkController.getInstance().getHomeView();    
            if(view != null)
                view.loadUrl("javascript:zoek('"+query+"')");            
            finish();
        } 
    }    
}
