package erenik.evergreen.android.act;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketError;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequest;
import erenik.evergreen.common.player.Stat;

public class GameOver extends EvergreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        Player player = App.GetPlayer();
        /*
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 5000); // 5000ms delay
        */
        // If player is non-null, update content properly?
        if (player != null) {
            TextView tv = (TextView) findViewById(R.id.textView_TurnSurvived);
            tv.setText(getString(R.string.survivedUntilTurn) + " " + (int) (player.Get(Stat.TurnSurvived)));
        }
        Button b = (Button) findViewById(R.id.buttonNewCharacter);
        b.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), CreateCharacter.class);
                startActivity(i);
            }
        });
        findViewById(R.id.buttonRestartSameCharacter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App.IsLocalGame()){
                    // Just restart it.
                    App.GetPlayer().PrepareForTotalRestart();
                    App.GetPlayer().ReviveRestart();
                    GoToMainScreen();
                    finish();
                    return;
                }
                // Request a restart of the same character.
                EGPacket pack = EGRequest.RestartSameCharacter(App.GetPlayer());
                ShowProgressBar();
                pack.addReceiverListener(new EGPacketReceiverListener() {
                    @Override
                    public void OnReceivedReply(EGPacket reply) {
                        HideProgressBar();
                        switch (reply.ResType()){
                            default:
                                Toast("Reply: "+reply.ResType().name());
                                break;
                            case Player:
                                Toast("Character restarted.");
                                try {
                                    App.UpdatePlayer(reply.GetPlayer());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast("Error: "+e.getMessage());
                                    return;
                                }
                                // Go to main screen then?
                                GoToMainScreen();
                                finish();
                                break;
                        }
                    }
                    @Override
                    public void OnError(EGPacketError error) {
                        HideProgressBar();
                        Toast("Error: "+error.name());
                    }
                });
                App.Send(pack);
            }
        });

        focusLastLogMessageUponUpdate = true;
    }
    // Delayed, make buttons visible?

    @Override
    protected void onResume() {
        super.onResume();
        UpdateLog();
    }
}
