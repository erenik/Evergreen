package erenik.evergreen.android.act;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;

/**
 * Activity that starts the thingy.
 * Query new or old player:
 * -    If new player: go to character and game mode creation straight away.
 * -    If old player: enter login details to fetch game data from server. Bind game data to google account, or...?
 */
public class Startup extends EvergreenActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        findViewById(R.id.buttonLoadGame).setOnClickListener(loadGameListener);
        findViewById(R.id.buttonNewGame).setOnClickListener(newGameListener);
        /// Check for local save file (default user-name, hashed pw, statistics, et al).
        SharedPreferences sp = App.GetPreferences();
    }

    View.OnClickListener newGameListener = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            System.out.println("New game, opening into-screen..");
            // New game.
            Intent i = new Intent(getBaseContext(), IntroScreen.class);
            startActivity(i);
            // Kill this activity so that we cannot go back here. Only used on init.
            finish();
        }
    };

    View.OnClickListener loadGameListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Load())
            {
                System.out.println("Save exists, opening main-screen.");
                // Actually load into the singleton.
                Player player = App.GetPlayer();
                // Load main-screen.
                Intent i = new Intent(getBaseContext(), MainScreen.class);
                startActivity(i);
                // Kill this activity so that we cannot go back here. Only used on init.
                finish();
            }
        }
    };
}
