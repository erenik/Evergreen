package evergreen.android.act;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import evergreen.common.Invention.InventionStat;
import evergreen.common.player.SkillType;
import evergreen.util.EList;
import evergreen.util.Arrays;
import evergreen.util.EList;

import evergreen.android.App;
import evergreen.R;
import evergreen.common.Invention.Invention;
import evergreen.common.Invention.InventionType;
import evergreen.common.Player;
import evergreen.common.player.Stat;
import evergreen.util.Printer;

/**
 * Created by Emil on 2016-12-09.
 */

public class StatViewActivity extends EvergreenActivity {
    Stat statType = null;
    EList<Stat> relevantStats() {
        Stat[] statArr = new Stat[]{Stat.BASE_ATTACK, Stat.BASE_DEFENSE, Stat.AccumulatedEmissions, Stat.FOOD, Stat.MATERIALS, Stat.HP};
        return new EList<Stat>(statArr);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stat_view);

        int index = getIntent().getIntExtra("Stat", 0);
        statType = Stat.values()[index];
        Printer.out("Stat type: "+statType.name());

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

        findViewById(R.id.buttonRelevantItems).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), InventoryScreen.class);
                int reqCode = InventoryScreen.DisplayAll;
                switch (statType){
                    case BASE_ATTACK: reqCode = InventoryScreen.DisplayWeapons; break;
                    case BASE_DEFENSE: reqCode = InventoryScreen.DisplayArmors; break;
                }
                i.putExtra(InventoryScreen.DisplayFilter, reqCode);
                startActivity(i);
            }
        });
    }
    /// MAIN UI Updater
    public void UpdateUI() {
        int[] strIds;
        switch (statType) {
            default:
            case HP: strIds = new int[]{R.string.HP_name, R.string.HP_desc, R.drawable.icon_hp}; break;
            case FOOD: strIds = new int[]{R.string.Food_name, R.string.Food_desc, R.drawable.icon_food}; break;
            case MATERIALS: strIds = new int[]{R.string.Materials_name, R.string.Materials_desc, R.drawable.icon_materials}; break;
            case AccumulatedEmissions: strIds = new int[]{R.string.Emissions_name, R.string.Emissions_desc, R.drawable.icon_emissions}; break;
            case BASE_ATTACK: strIds = new int[]{R.string.Attack_name, R.string.Attack_desc, R.drawable.icon_attack}; break;
            case BASE_DEFENSE: strIds = new int[]{R.string.Defense_name, R.string.Defense_desc, R.drawable.icon_defense}; break;
        }
        // Update image, text, description.
        TextView tv = (TextView) findViewById(R.id.textView_stat_name);
        tv.setText(getString(strIds[0]));
        tv = (TextView) findViewById(R.id.textView_stat_detailedInfo);
        tv.setText(getString(strIds[1]));
        tv = (TextView) findViewById(R.id.textView_stat_quantity);
        Player player = App.GetPlayer();
        switch(statType) {
            case BASE_ATTACK:
                tv.setText(""+(int) player.AggregateAttackBonus());
                break;
            default:
                tv.setText("" + (int) player.Get(statType));
        }
        ImageView iv = (ImageView) findViewById(R.id.icon_image);
        iv.setImageResource(strIds[2]);

        // Make visible relevant details parts.
        int idmv = 0; // id to make visible.
        int[] ids = new int[]{R.id.layoutAttackDetails, R.id.layoutDefenseDetails, R.id.layoutFoodDetails};
        for (int i = 0; i < ids.length; ++i)
            findViewById(ids[i]).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)); // 0 height to make invisible.
        InventionType typeRelevant = null;
        switch(statType)
        {
            case BASE_ATTACK:
                Invention weapon = player.GetEquippedWeapon();
                idmv = R.id.layoutAttackDetails;
                ((TextView)findViewById(R.id.tvEquippedMainWeapon)).setText(weapon != null? weapon.name : getString(R.string.unarmed));
                ((TextView)findViewById(R.id.tvAttackDamage)).setText(player.Damage().Min()+"-"+player.Damage().Max());
                ((TextView)findViewById(R.id.tvAttackWhenOut)).setText(""+player.BaseAttack());
                ((TextView)findViewById(R.id.tvAttackWhenInShelter)).setText(""+player.ShelterAttack());
                ((TextView)findViewById(R.id.tvAttacksPerRound)).setText(""+player.attacksPerTurn);
                typeRelevant = InventionType.Weapon;
                break;
            case BASE_DEFENSE:
                idmv = R.id.layoutDefenseDetails;
                Invention armor = player.GetEquippedArmor();
                ((TextView)findViewById(R.id.tvEquippedArmor)).setText(armor != null? armor.name : getString(R.string.ordinaryClothes));
                ((TextView)findViewById(R.id.tvDefenseWhenOut)).setText(""+player.BaseDefense());
                ((TextView)findViewById(R.id.tvDefenseWhenInShelter)).setText(""+player.ShelterDefense());
                ((TextView)findViewById(R.id.tvParryingBonus)).setText(""+(player.GetEquipped(InventionStat.ParryBonus) + player.Get(SkillType.Parrying).Level()));
//                ((TextView)findViewById(R.id.tvAttackDamage)).setText
                typeRelevant = InventionType.Armor;
                break;
            case FOOD:
                idmv = R.id.layoutFoodDetails;
                typeRelevant = InventionType.Tool;
                break;
            default:
                typeRelevant = InventionType.Tool;
                break;
        }
        View detailView = findViewById(idmv);
        if (detailView != null)
            detailView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); // 0 height to make invisible.

        // Update current bonuses, etc.


    }

    @Override
    protected void onResume() {
        super.onResume();
        UpdateUI(); // If something was equipped for example..
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