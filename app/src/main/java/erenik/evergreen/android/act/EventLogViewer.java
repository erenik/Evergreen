package erenik.evergreen.android.act;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import erenik.evergreen.android.App;
import erenik.evergreen.R;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.Player;

public class EventLogViewer extends EvergreenActivity {

    Player player = App.GetPlayer();
    private View.OnClickListener toggleFilter = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CheckBox cb = (CheckBox) v;
            switch(v.getId())
            {
                case R.id.checkboxMissedAttacks:
                    SetFilter(LogType.ATTACK_MISS, cb.isChecked());
                    SetFilter(LogType.ATTACKED_MISS, cb.isChecked());
                    break;
                case R.id.checkboxDamage:
                    SetFilter(LogType.ATTACK, cb.isChecked());
                    SetFilter(LogType.ATTACKED, cb.isChecked());
                    break;
            }
            UpdateLog();
        }
    };

    void SetFilter(LogType lt, boolean filterIt) {
        // Filter it -> Remove it from types to show.
        if (filterIt)
        {
            for (int i = 0; i < player.logTypesToShow.size(); ++i)
            {
                if (player.logTypesToShow.get(i).ordinal() == lt.ordinal())
                {
                    player.logTypesToShow.remove(i);
                    --i;
                }
            }
            return;
        }
        // Add it if not already there?
        if (IsShown(lt))
            return;
        // Add it.
        player.logTypesToShow.add(lt);
    }
    boolean IsShown(LogType lt)
    {
        return !IsFiltered(lt);
    }
    boolean IsFiltered(LogType lt)
    {
        for (int i = 0; i < player.logTypesToShow.size(); ++i)
        {
            if (player.logTypesToShow.get(i).ordinal() == lt.ordinal())
                return false;
        }
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);
        ///  Update checkboxes.
        CheckBox missedAttacks = (CheckBox) findViewById(R.id.checkboxMissedAttacks);
        missedAttacks.setChecked(IsFiltered(LogType.ATTACK_MISS));
        missedAttacks.setOnClickListener(toggleFilter);
        CheckBox checkboxDamage = (CheckBox) findViewById(R.id.checkboxDamage);
        checkboxDamage.setChecked(IsFiltered(LogType.ATTACK));
        checkboxDamage.setOnClickListener(toggleFilter);

        // Update initial contents.
        maxLogLinesInEventLog = 500;
        focusLastLogMessageUponUpdate = true; // Scroll to the bottom.
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Scroll to the last message in the log?
    }
}
