package erenik.seriousgames.evergreen.act;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import erenik.seriousgames.evergreen.App;
import erenik.seriousgames.evergreen.player.Constants;
import erenik.seriousgames.evergreen.player.Player;
import erenik.seriousgames.evergreen.R;

public class Startup extends EvergreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        findViewById(R.id.buttonLoadGame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App.LoadLocally(getBaseContext()))
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
        });

        findViewById(R.id.buttonNewGame).setOnClickListener(new View.OnClickListener() {
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
        });

        /// Check for local save file (default user-name, hashed pw, statistics, et al).
        SharedPreferences sp = App.GetPreferences();
    }
}
