package erenik.seriousgames.evergreen;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(getBaseContext(), MainScreen.class);
                startActivity(i);
            }
        }, 5000); // 5000ms delay

    }

    // Delayed, make buttons visible?
}
