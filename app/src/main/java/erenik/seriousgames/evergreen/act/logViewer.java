package erenik.seriousgames.evergreen.act;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import erenik.seriousgames.evergreen.R;
import erenik.seriousgames.evergreen.logging.Log;
import erenik.seriousgames.evergreen.logging.LogType;
import erenik.seriousgames.evergreen.player.Player;

public class logViewer extends AppCompatActivity
{

    Player player = Player.getSingleton();
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
            UpdateShownLog();
        }
    };

    void SetFilter(LogType lt, boolean filterIt)
    {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


/*        // Fabulous!
FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        ///  Update checkboxes.
        CheckBox missedAttacks = (CheckBox) findViewById(R.id.checkboxMissedAttacks);
        missedAttacks.setChecked(IsFiltered(LogType.ATTACK_MISS));
        missedAttacks.setOnClickListener(toggleFilter);
        CheckBox checkboxDamage = (CheckBox) findViewById(R.id.checkboxDamage);
        checkboxDamage.setChecked(IsFiltered(LogType.ATTACK));
        checkboxDamage.setOnClickListener(toggleFilter);
//        CheckBox
        // Update initial contents.
        UpdateShownLog();
    }
    void UpdateShownLog()
    {
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutLog);
        vg.removeAllViews();
        // Add children? Filter?
        Log.UpdateLog(vg, getBaseContext(), 500, player.logTypesToShow);
    }
}