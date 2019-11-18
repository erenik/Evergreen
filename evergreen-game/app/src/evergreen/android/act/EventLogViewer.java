package evergreen.android.act;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import evergreen.android.App;
import evergreen.R;
import evergreen.common.logging.LogType;
import evergreen.common.Player;
import evergreen.util.Printer;

public class EventLogViewer extends EvergreenActivity {

    final String NUM_LOG_MESSAGES = "NumLogMessages";

    Player player = App.GetPlayer();
    private View.OnClickListener toggleFilter = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CheckBox cb = (CheckBox) v;
            boolean isChecked = cb.isChecked();
            switch(v.getId())
            {
                case R.id.checkboxMissedAttacks:
                    SetFilter(LogType.ATTACK_MISS, cb.isChecked());
                    SetFilter(LogType.ATTACKED_MISS, cb.isChecked());
                    SetFilter(LogType.PLAYER_ATTACK_MISS, cb.isChecked());
                    SetFilter(LogType.PLAYER_ATTACKED_ME_BUT_MISSED, cb.isChecked());
                    break;
                case R.id.checkboxDamage:
                    SetFilter(LogType.ATTACK, isChecked);
                    SetFilter(LogType.ATTACKED, isChecked);
                    SetFilter(LogType.PLAYER_ATTACK, isChecked);
                    SetFilter(LogType.PLAYER_ATTACKED_ME, isChecked);
                    break;
                case R.id.checboxVanquished:
                    SetFilter(LogType.DEFEATED_ENEMY, cb.isChecked());
                    Printer.out("Log typ set for DEFEATED_ENEMY: "+cb.isChecked());
                    break;
                case R.id.checkboxOtherCombat:
                    SetFilter(LogType.ENC_INFO, isChecked);
                    SetFilter(LogType.ENC_INFO_FAILED, isChecked);
                    break;
                case R.id.checboxActionFailed:
                    SetFilter(LogType.ACTION_FAILURE, cb.isChecked());
                    SetFilter(LogType.ACTION_NO_PROGRESS, cb.isChecked());
                    break;
                case R.id.checkboxInfo: // Group in success here as well for now.
                    SetFilter(LogType.INFO, cb.isChecked());
                    SetFilter(LogType.SUCCESS, isChecked);
                    SetFilter(LogType.EXP, isChecked);
                    SetFilter(LogType.OtherDamage, isChecked);
                    break;
            }
            UpdateLog();
        }
    };

    void SetFilter(LogType lt, boolean filterIt) {
        // Filter it -> Remove it from types to show.
        if (filterIt) {
            // Add it.
            if (IsFiltered(lt))
                return;
            player.logTypesToFilter.add(lt);
            return;
        }
        // Add it if not already there?
        for (int i = 0; i < player.logTypesToFilter.size(); ++i) {
            if (player.logTypesToFilter.get(i).ordinal() == lt.ordinal()) {
                player.logTypesToFilter.remove(i);
                --i;
            }
        }
    }
    boolean IsShown(LogType lt)
    {
        return !IsFiltered(lt);
    }
    boolean IsFiltered(LogType lt) {
        for (int i = 0; i < player.logTypesToFilter.size(); ++i) {
            if (player.logTypesToFilter.get(i).ordinal() == lt.ordinal())
                return true;
        }
        return false;
    }
    static int numRepliesReceived = 0;
    int logMessagesToLoad = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Printer.out("EventLogViewer onCreate");
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
        CheckBox checkboxVanquished = (CheckBox) findViewById(R.id.checboxVanquished);
        checkboxVanquished.setChecked(IsFiltered(LogType.DEFEATED_ENEMY));
        checkboxVanquished.setOnClickListener(toggleFilter);
        CheckBox checkboxOtherCombat = (CheckBox) findViewById(R.id.checkboxOtherCombat);
        checkboxOtherCombat.setChecked(IsFiltered(LogType.ENC_INFO));
        checkboxOtherCombat.setOnClickListener(toggleFilter);
        CheckBox checkboxInfo = (CheckBox) findViewById(R.id.checkboxInfo);
        checkboxInfo.setChecked(IsFiltered(LogType.INFO));
        checkboxInfo.setOnClickListener(toggleFilter);
        CheckBox checkboxActionFailed = (CheckBox) findViewById(R.id.checkboxInfo);
        checkboxActionFailed.setChecked(IsFiltered(LogType.ACTION_FAILURE));
        checkboxActionFailed.setOnClickListener(toggleFilter);

        Spinner spin = (Spinner) findViewById(R.id.spinnerLogMessagesToDisplay);
        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView) view;
                if (tv == null) return;
                String choice = (String) tv.getText();
                App.GetPreferences().edit().putString(NUM_LOG_MESSAGES, choice).commit();  // Save it into preferences.
                int num = Integer.parseInt(choice);
                // Update initial contents.
                maxLogLinesInEventLog = 500;
                focusLastLogMessageUponUpdate = true; // Scroll to the bottom.
                if (App.IsLocalGame()) {
                    UpdateLog();                    // Just update the log immediately?
                }
                else
                    RequestLogMessages(num, 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.num_log_messages_to_display, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(adapter);

        // Check the saved preferences.
        SharedPreferences sp = App.GetPreferences();
        String oldChoice = sp.getString(NUM_LOG_MESSAGES, "100");
        String[] choices = getResources().getStringArray(R.array.num_log_messages_to_display);
        int index = 0;
        for (int i = 0; i < choices.length; ++i){
            if (choices[i].equals(oldChoice))
                index = i;
        }
        spin.setSelection(index);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Scroll to the last message in the log?
        UpdateBackgroundView(findViewById(R.id.layout_log_viewer_bg));
    }
}
