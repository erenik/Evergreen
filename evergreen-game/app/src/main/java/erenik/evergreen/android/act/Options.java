package erenik.evergreen.android.act;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.player.Config;

/**
 * Created by Emil on 2017-04-11.
 */

public class Options extends EvergreenActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);

        findViewById(R.id.buttonRetainDailyActions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Player player = App.GetPlayer();
                if (player == null){
                    Toast("No active player");
                    return;
                }
                int value = (int) player.Get(Config.RetainDailyActionsOnNewTurn);
                value = value == 1? 0 : 1;
                player.Set(Config.RetainDailyActionsOnNewTurn, value);
                SaveLocally();
                UpdateUI();
            }
        });
        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        UpdateUI();
    }

    public void UpdateUI(){
        Player player = App.GetPlayer();
        if (player == null)
            return;
        ((Button)findViewById(R.id.buttonRetainDailyActions)).setText(player.Get(Config.RetainDailyActionsOnNewTurn) == 1? "Retain same daily actions after new turn" : "Reset daily actions on new turn");
    }

}
