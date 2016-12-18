package erenik.evergreen.android.act;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;

public class GameOver extends EvergreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        // Overwrite/reset save.
        Player player = App.GetPlayer();
        player.SetDefaultStats();
        Save();
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
