package erenik.evergreen.android.act;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.android.ui.EvergreenTextView;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Invention.InventionStat;
import erenik.evergreen.common.Invention.Weapon;
import erenik.evergreen.common.Player;
import erenik.util.Printer;

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
        Printer.out("Index: "+itemIndex);
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
//            case RangedWeapon:
            case Armor:
            case Tool:
                ShowEquipButton();
                break;
            default:
                HideEquipButton();
                break;
        }
        View v = findViewById(R.id.buttonEquip);
        if (item.Get(InventionStat.Equipped) >= 0)
            ((Button)v).setText("Unequip");
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (item.Get(InventionStat.Equipped) >= 0){
                    Printer.out("Unequipped.");
                    item.Set(InventionStat.Equipped, -1);
                    Toast("Unequipped "+item.name);
                    UpdateEquipButtonText();
                    return;
                }
                player.Equip(item);
                Printer.out("Equipping item.");
                Toast("Equipped "+item.name);
                UpdateEquipButtonText();
//                HideEquipButton();
            }
        });

        UpdateRecyleButtonTest();
        findViewById(R.id.buttonRecycle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (item.Get(InventionStat.ToRecyle) == 0){
                    Printer.out("To be recycled.!");
                    item.Set(InventionStat.ToRecyle, 1);
                    Toast("Set for recycling: "+item.name);
                    return;
                } else {
                    item.Set(InventionStat.ToRecyle, 0);
                    Toast("Recycling undone");
                }
                UpdateRecyleButtonTest();
//                HideEquipButton();
            }
        });
    }

    private void UpdateRecyleButtonTest() {
        int toRecycle = item.Get(InventionStat.ToRecyle);
        Button b = (Button) findViewById(R.id.buttonRecycle);
        if (toRecycle == 1)
            b.setText("Undo recycle");
        else
            b.setText("Recycle");

    }

    void UpdateEquipButtonText(){
        Button button = (Button) findViewById(R.id.buttonEquip);
        if (item.Get(InventionStat.Equipped) >= 0)
            button.setText("Unequip");
        else
            button.setText("Equip");
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
                case Blueprint:
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
                    Printer.out("Defense: "+statValue);
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
            tv = new TextView(getBaseContext());
            if (customDisplay.length() > 0)
                tv.setText(customDisplay);
            else
                tv.setText(stat.name()+": "+statValue);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2.f);
            layoutParams.setMargins(15,0,15,0); // Margins left n right.
            layoutParams.gravity = Gravity.CENTER;
            tv.setLayoutParams(layoutParams);

            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
//            tv.setTextSize(100);

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
