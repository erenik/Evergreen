package erenik.evergreen.android.act;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import erenik.evergreen.android.App;
import erenik.evergreen.R;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.player.Stat;

/**
 * Created by Emil on 2016-12-09.
 */

public class StatViewActivity extends EvergreenActivity {
    Stat statType = null;
    List<Stat> relevantStats() {
        Stat[] statArr = new Stat[]{Stat.BASE_ATTACK, Stat.BASE_DEFENSE, Stat.EMISSIONS, Stat.FOOD, Stat.MATERIALS, Stat.HP};
        return new ArrayList<Stat>(Arrays.asList( statArr));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stat_view);

        int index = getIntent().getIntExtra("Stat", 0);
        statType = Stat.values()[index];
        System.out.println("Stat type: "+statType.name());

        // Set up listeners for main buttons (next, previous, go back?)
        findViewById(R.id.buttonPrevious).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = relevantStats().indexOf(statType);
                statType = relevantStats().get((index + 1) % relevantStats().size());
                UpdateUI();
            }
        });
        findViewById(R.id.buttonNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = relevantStats().indexOf(statType) - 1;
                if (index < 0)
                    index = relevantStats().size() - 1;
                statType = relevantStats().get(index % relevantStats().size());
                UpdateUI();
            }
        });
        findViewById(R.id.buttonHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop this activity, assume it goes back to the correct previous one?
                finish();
            }
        });
        findViewById(R.id.buttonToggleDetailedDescription).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = findViewById(R.id.textView_stat_detailedInfo);
                view.setVisibility(view.getVisibility() == View.INVISIBLE? View.VISIBLE : View.INVISIBLE);
            }
        });
        UpdateUI();        // Update UI.
    }
    /// MAIN UI Updater
    void UpdateUI() {
        int[] strIds;
        switch (statType) {
            default:
            case HP: strIds = new int[]{R.string.HP_name, R.string.HP_desc, R.drawable.icon_hp}; break;
            case FOOD: strIds = new int[]{R.string.Food_name, R.string.Food_desc, R.drawable.icon_food}; break;
            case MATERIALS: strIds = new int[]{R.string.Materials_name, R.string.Materials_desc, R.drawable.icon_materials}; break;
            case EMISSIONS: strIds = new int[]{R.string.Emissions_name, R.string.Emissions_desc, R.drawable.icon_emissions}; break;
            case ATTACK_BONUS: case BASE_ATTACK: strIds = new int[]{R.string.Attack_name, R.string.Attack_desc, R.drawable.icon_attack}; break;
            case DEFENSE_BONUS: case BASE_DEFENSE: strIds = new int[]{R.string.Defense_name, R.string.Defense_desc, R.drawable.icon_defense}; break;
        }
        // Update image, text, description.
        TextView tv = (TextView) findViewById(R.id.textView_stat_name);
        tv.setText(getString(strIds[0]));
        tv = (TextView) findViewById(R.id.textView_stat_detailedInfo);
        tv.setText(getString(strIds[1]));
        tv = (TextView) findViewById(R.id.textView_stat_quantity);
        Player player = App.GetPlayer();
        tv.setText(""+player.Get(statType));

        ImageView iv = (ImageView) findViewById(R.id.icon_image);
        iv.setImageResource(strIds[2]);

        // Update current bonuses, etc.

        // Update action buttons.

    }
        /// Populate the available items.
        /*
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutItems);
        for (int i = 0; i < itemNames.size(); ++i)
        {
            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            b.setText(itemNames.get(i));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, App.GetScreenSize().y / 12);
            layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
            b.setBackgroundResource(buttonBgId);
            b.setLayoutParams(layoutParams);
            vg.addView(b);
            b.setOnClickListener(addItem);
        }
        // Load from player.
        Player p = App.GetPlayer();
        if (type == SELECT_DAILY_ACTION) {
            for (int i = 0; i < p.dailyActions.size(); ++i)
                selected.add(p.dailyActions.get(i));
        }
        else if (type == SELECT_SKILL)
        {
            for (int i = 0; i < p.skillTrainingQueue.size(); ++i)
                selected.add(p.skillTrainingQueue.get(i));
        }
        // Update the queue
        updateQueue();
    };*/
}