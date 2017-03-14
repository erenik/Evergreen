package erenik.evergreen.android.act;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.android.ui.EvergreenButton;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        /// Update GUI.
//        UpdateGUI();

        findViewById(R.id.buttonNewCharacter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), CreateCharacter.class);
                startActivity(i);
                finish();
            }
        });
        findViewById(R.id.buttonDeleteAllCharacters).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.DeletePlayers();
                App.SaveLocally();
                Intent i = new Intent(getBaseContext(), CreateCharacter.class);
                startActivity(i);
                finish();
            }
        });
        findViewById(R.id.buttonTransportUsage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), TransportUsage.class);
                startActivity(i);
//                finish();
            }
        });
        findViewById(R.id.buttonNewCharacter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), CreateCharacter.class);
                startActivity(i);
                finish();
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
            // Make a bouton.
            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            b.setText(p.name);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, App.GetScreenSize().y / 12);
            layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
            int buttonBgId = R.drawable.small_button;
            b.setBackgroundResource(buttonBgId);
            b.setLayoutParams(layoutParams);
            vg.addView(b);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button b = (Button) v;
                    List<Player> players = App.GetPlayers();
                    for (int i = 0; i < players.size(); ++i){
                        Player p = players.get(i);
                        if (p.name.equals(b.getText()))
                        App.MakeActivePlayer(p);
                        GoToMainScreen();
                        finish();
                    }
                }
            });
            // do stuff.
        }
        if (players.size() == 0) {
            Toast("No players found");
        }
    }

}
