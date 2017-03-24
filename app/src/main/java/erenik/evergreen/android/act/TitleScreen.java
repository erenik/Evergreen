package erenik.evergreen.android.act;

import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.Visibility;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;
import erenik.evergreen.common.auth.Auth;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketError;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequest;

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
        App.SetActivePlayer(null);

        EditText et = (EditText)findViewById(R.id.editText_password);
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    // Press?
                    findViewById(R.id.button_tryLoad).callOnClick(); // Call ittt!!
                    System.out.println("DONEOEENENNEe");
                    return true;
                }
                return false;
            }
        });


        View v = findViewById(R.id.layoutMainButtons);
        v.setVisibility(View.VISIBLE);
        findViewById(R.id.layout_loadGameButtons).setVisibility(View.INVISIBLE);
        findViewById(R.id.button_singleplayer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.SetLocalGame();
                TryLoadOrNewGame();
            }
        });
        findViewById(R.id.button_multiplayer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.SetMultiplayer();
                NewGame();
            }
        });
        findViewById(R.id.button_loadGame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.layoutMainButtons).setVisibility(View.INVISIBLE);
                findViewById(R.id.layout_loadGameButtons).setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.layoutMainButtons).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_loadGameButtons).setVisibility(View.INVISIBLE);
            }
        });
        findViewById(R.id.button_tryLoad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = ((EditText)findViewById(R.id.autoCompleteTextView_email)).getText().toString();
                String pw = ((EditText)findViewById(R.id.editText_password)).getText().toString();
                String encPw = Auth.Encrypt(pw, Auth.DefaultKey);
                System.out.println("Sending to server: "+email+" pw: "+encPw);
//                Toast("Trying to send to server...");
                ShowProgressBar();
                EGPacket pack = EGRequest.LoadCharacters(email, encPw);
                App.Send(pack);
                pack.addReceiverListener(new EGPacketReceiverListener() {
                    @Override
                    public void OnReceivedReply(EGPacket reply) {
                        HideProgressBar();
                        switch (reply.ResType()){
                            default:
                                Toast("Unclear res type: "+reply.ResType().name());
                                break;
                            case BadPassword:
                                ToastUp("Bad password, try again");
                                break;
                            case NoSuchPlayer:
                                ToastUp("Found no players under that e-mail and password. Try again.");
                                break;
                            case Players:
                                ToastUp("Received reply");
                                System.out.println("Received reply: "+reply.toString());
                                ArrayList<Player> players = reply.parsePlayers();
                                if (players.size() == 0){
                                    ToastUp("Error parsing Players data");
                                    return;
                                }
                                App.SetPlayers(reply.parsePlayers());
                                // Open screen to select them, or just auto-load the first one?
                                App.MakeActivePlayer(players.get(0));
                                GoToMainScreen();
                                break;
                        }
                    }
                    @Override
                    public void OnError(EGPacketError error) {
                        HideProgressBar();
                        Toast("Error: "+error.name());
                    }
                });
            }
        });
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
            // Don't kill this activity, leave it as the base if the user backs from the main screen.
//            finish();
            return;
        }
        else
            ToastLong("New player, eh? Welcome aboard! Choose singleplayer if you want want a brief tutorial.");

    }

    private void NewGame() {
        System.out.println("New game, opening intro-screen..");
        Intent i = new Intent(getBaseContext(), IntroScreen.class);
        startActivity(i);
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
        // Don't kill this activity, leave it as the base if the user backs from the main screen.
//        finish(); // Kill this activity so that we cannot go back here. Only used on init.
    }

}
