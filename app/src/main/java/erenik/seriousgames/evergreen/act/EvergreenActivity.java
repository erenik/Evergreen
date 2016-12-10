package erenik.seriousgames.evergreen.act;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import erenik.seriousgames.evergreen.App;

/**
 * Created by Emil on 2016-12-09.
 */
public class EvergreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set default orientation - Portrait.
        // Store common shit here?
        App.OnActivityCreated(this);  // Setup system callbacks as/if needed.
    }
    /// Saves locally, using default preferences location.
    boolean SaveLocally() {
        return App.SaveLocally(getBaseContext());
    }
    boolean LoadLocally() {
        return App.LoadLocally(getBaseContext());
    }
}
