package erenik.seriousgames.evergreen.act;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import erenik.seriousgames.evergreen.player.Player;
import erenik.seriousgames.evergreen.R;

public class GameOver extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        // Overwrite/reset save.
        Player player = Player.getSingleton();
        player.SetDefaultStats();
        player.SaveLocally();
        // Delayed move to main menu again?

        /*
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 5000); // 5000ms delay
        */

        Button b = (Button) findViewById(R.id.buttonNewGame);
        View.OnClickListener click = new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), MainScreen.class);
                startActivity(i);
            }
        };
        b.setOnClickListener(click);
    }
    // Delayed, make buttons visible?
}
