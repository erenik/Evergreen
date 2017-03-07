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
public class TitleScreen extends EvergreenActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title_screen);
        findViewById(R.id.button_singleplayer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.isLocalGame = true;
                App.isMultiplayerGame = false;
                TryLoadOrNewGame();
            }
        });
        findViewById(R.id.button_multiplayer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.isMultiplayerGame = true;
                App.isLocalGame = false;
                TryLoadOrNewGame();
            }
        });
        /// Check for local save file (default user-name, hashed pw, statistics, et al).
        SharedPreferences sp = App.GetPreferences();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // After creating successfully, try and load the most recently played character automatically?
        boolean ok = App.LoadLocally();
        if (App.GetPlayers().size() > 0) {
            // Auto-load the first-index one for now?
            Player p = App.GetMostRecentlyEditedPlayer();
            Toast("Auto-loading: "+p.name);
            System.out.println("Auto-loading player: "+p.name);
            App.MakeActivePlayer(p);
            GoToMainScreen();
            finish();
            return;
        }
        else
            ToastLong("New player, eh? Welcome aboard! Choose singleplayer if you want want a brief tutorial.");

    }

    private void TryLoadOrNewGame() {
        if (App.GetPlayers().size() > 0) {
            System.out.println("Load succeeded. Opening main-screen.");
            // Actually load into the singleton.
            Player player = App.GetPlayer();
            // Load main-screen.
            Intent i = new Intent(getBaseContext(), MainScreen.class);
            startActivity(i);
        }
        else {
            System.out.println("New game, opening intro-screen..");
            Intent i = new Intent(getBaseContext(), IntroScreen.class);
            startActivity(i);
        }
        finish(); // Kill this activity so that we cannot go back here. Only used on init.
    }

}
