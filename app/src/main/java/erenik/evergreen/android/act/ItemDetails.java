package erenik.evergreen.android.act;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.android.ui.EvergreenTextView;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Invention.InventionStat;
import erenik.evergreen.common.Invention.Weapon;
import erenik.evergreen.common.Player;

public class ItemDetails extends EvergreenActivity {

    int itemIndex = 0;
    Invention item = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);
        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        int itemIndex = getIntent().getIntExtra("ItemIndex", 0);
        System.out.println("Index: "+itemIndex);
        Player p = App.GetPlayer();
        item = p.inventory.get(itemIndex);
        if (item == null) {
            Toast("Failed to grab item details.");
            finish();
            return;
        }
        UpdateUI();
    }

    public void UpdateUI(){
        TextView tv = (TextView) findViewById(R.id.textViewItemName);
        tv.setText(item.name);
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutItemStats);
        vg.removeAllViews();
        for (int i = 0; i < InventionStat.values().length; ++i){
            InventionStat stat = InventionStat.values()[i];
            int statValue = item.Get(InventionStat.values()[i]);
            int drawableID;
            String customDisplay = "";
            switch (stat){
                // Skip display of some stats?
                case QualityLevel:
                case MaterialCost:
                case ProgressRequiredToCraft:
                case TimesInvented:
                case Type:
                case SubType:
                case Equipped:
                case AdditionalEffect:
                case IsRanged:
                case AttackDamageDiceType: // Skip this always, just show attack on one row?
                case AttackDamageBonus:
                    continue;
                case DefenseBonus:
                    System.out.println("Defense: "+statValue);
                // For others, skip their display if their value is default (0)?
                default:
                    if (statValue <= 0)
                        continue;
            }

            switch (stat){
                case AttackDamageDice:
                    if (statValue > 0) {
                        Weapon weapon = (Weapon) item;
                        drawableID = R.drawable.icon_attack;
                        customDisplay = "Base damage: " + weapon.MinimumDamage() + " to "+weapon.MaximumDamage();
                    }
                    break;
            }

            LinearLayout ll = new LinearLayout(getBaseContext());
            vg.addView(ll);

            // Add text
            tv = new EvergreenTextView(getBaseContext());
            if (customDisplay.length() > 0)
                tv.setText(customDisplay);
            else
                tv.setText(stat.name()+": "+statValue);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            ll.addView(tv);
        }
    }

}
