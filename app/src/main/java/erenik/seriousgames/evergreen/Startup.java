package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Startup extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        // Setup system callbacks.
        App.NewActivityLifeCycleCallback();
        getApplication().registerActivityLifecycleCallbacks(App.actLCCallback);
        App.currentActivity = this; // Point it right.

        /// Check for local save file (default user-name, hashed pw, statistics, et al).
        SharedPreferences sp = App.GetPreferences();
        if (sp.getBoolean(Constants.SAVE_EXISTS, false)) // "saveExists"
        {
            System.out.println("Save exists, opening main-screen.");
            // Actually load into the singleton.
            Player player = Player.getSingleton();
            player.LoadLocally();
            // Load main-screen.
            Intent i = new Intent(getBaseContext(), MainScreen.class);
            startActivity(i);
            // Kill this activity so that we cannot go back here. Only used on init.
            finish();
        }
        else
        {
            System.out.println("New game, opening into-screen..");
            // New game.
            Intent i = new Intent(getBaseContext(), IntroScreen.class);
            startActivity(i);
            // Kill this activity so that we cannot go back here. Only used on init.
            finish();
        }
    }
}
