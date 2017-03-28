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
        final Player player = App.GetPlayer();
        item = player.cd.inventory.get(itemIndex);
        if (item == null) {
            Toast("Failed to grab item details.");
            finish();
            return;
        }
        UpdateUI();
        switch (item.type){
            case Weapon:
            case RangedWeapon:
            case Armor:
            case Tool:
                ShowEquipButton();
                break;
            default:
                HideEquipButton();
                break;
        }
        View v = findViewById(R.id.buttonEquip);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.Equip(item);
                System.out.println("Equipping item.");
                Toast("Equipped "+item.name);
                HideEquipButton();
            }
        });
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

    void ShowEquipButton(){
        View v = findViewById(R.id.buttonEquip);
        ViewGroup.LayoutParams lp = v.getLayoutParams();

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, lp.height, 0.8f);
        int margin = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin_smaller);
        lp2.setMargins(margin,margin, margin,margin);
        v.setLayoutParams(lp2);
//
//        <item name="android:layout_weight">0.8</item>
//        <item name="android:layout_margin">@dimen/activity_vertical_margin_smaller</item>
//        <item name="android:textSize">16dp</item>
//
//
//        <item name="android:layout_height">45sp</item>
//        <item name="android:textColor">@color/mainButtonTextColor</item>
//        <item name="android:layout_margin">@dimen/activity_vertical_margin_smaller</item>
//        <item name="android:background">@drawable/small_button</item>
//        <item name="android:textSize">@dimen/textSize.Larger</item>

    }
    void HideEquipButton(){
        View v = findViewById(R.id.buttonEquip);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
    }

}
