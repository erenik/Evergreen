package erenik.evergreen.android.act;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;

/**
 * Created by Emil on 2017-02-20.
 */
public class MenuActivity extends EvergreenActivity {
    @Override
    protected void onResume() {
        super.onResume();
//        UpdateGUI(); // Always update GUI upon resuming.
  //      App.HandleNextEvent();
    }

    /// Main init function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        App.mainScreenActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        /// Update GUI.
//        UpdateGUI();

        findViewById(R.id.buttonChangeCharacter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.layoutCharacters).setVisibility(View.VISIBLE);
                findViewById(R.id.layoutMainMenu).setVisibility(View.INVISIBLE);
                // Clear and refresh list of characters.
                populateCharacterList();
            }
        });
        findViewById(R.id.buttonHelp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotImplemented();
            }
        });
        findViewById(R.id.buttonOptions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotImplemented();
            }
        });
        findViewById(R.id.buttonQuitGame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.currentActivity.finishAffinity();
            }
        });
        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Finish this activity.
            }
        });
        findViewById(R.id.buttonBackFromCharacters).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.layoutCharacters).setVisibility(View.INVISIBLE);
                findViewById(R.id.layoutMainMenu).setVisibility(View.VISIBLE);
            }
        });

    }

    private void populateCharacterList() {
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutCharacterList);
        vg.removeAllViews();
        // Re-populate.
        List<Player> players = App.GetPlayers();
        for (int i = 0; i < players.size(); ++i) {
            Player p = players.get(i);
            // do stuff.
            NotImplemented();
        }
        if (players.size() == 0) {
            Toast("No players found");
        }
    }

}
