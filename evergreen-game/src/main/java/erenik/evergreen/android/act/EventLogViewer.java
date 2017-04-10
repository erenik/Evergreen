package erenik.evergreen.android.act;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import erenik.evergreen.android.App;
import erenik.evergreen.R;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.Player;
import erenik.util.Printer;

public class EventLogViewer extends EvergreenActivity {

    final String NUM_LOG_MESSAGES = "NumLogMessages";

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
                case R.id.checboxVanquished:
                    SetFilter(LogType.DEFEATED_ENEMY, cb.isChecked());
                    Printer.out("Log typ set for DEFEATED_ENEMY: "+cb.isChecked());
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
