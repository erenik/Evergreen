package erenik.evergreen.android.act;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;
import erenik.evergreen.common.player.Stat;

public class GameOver extends EvergreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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

        TextView tv = (TextView) findViewById(R.id.textView_TurnSurvived);
        tv.setText(getString(R.string.survivedUntilTurn)+" "+(int)(player.Get(Stat.TurnSurvived)));

        Button b = (Button) findViewById(R.id.buttonNewGame);
        View.OnClickListener click = new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), MainScreen.class);
                startActivity(i);

                // Overwrite/reset save.
                Player player = App.GetPlayer();
                player.SetDefaultStats();
                Save();
            }
        };
        b.setOnClickListener(click);

        focusLastLogMessageUponUpdate = true;
    }
    // Delayed, make buttons visible?
}
