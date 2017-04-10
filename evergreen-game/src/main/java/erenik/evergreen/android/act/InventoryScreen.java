package erenik.evergreen.android.act;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import erenik.evergreen.R;
import erenik.evergreen.common.Invention.InventionType;
import erenik.util.Printer;

/**
 * Created by Emil on 2017-02-26.
 */

public class InventoryScreen extends EvergreenActivity {
    public static final int DisplayWeapons = 0,
        DisplayArmors = 1,
        DisplayTools = 2,
        DisplayAll = 3;
    public static final String DisplayFilter = "DisplayFilter"; // For the ordinals of the above.
    /// Populate the inventory.
    InventionType typeRelevant = InventionType.Weapon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        // Check reqID?
        int reqCode = getIntent().getIntExtra(DisplayFilter, 0);
        Printer.out("Req code: "+reqCode);
        switch (reqCode){
            case DisplayWeapons: typeRelevant = InventionType.Weapon; break;
            case DisplayArmors: typeRelevant = InventionType.Armor; break;
            case DisplayTools: typeRelevant = InventionType.Tool; break;
            case DisplayAll:
            default:
                typeRelevant = InventionType.Any; break;
        }

        UpdateList();
        findViewById(R.id.buttonWeapons).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetFilter(InventionType.Weapon);
                UpdateList();
            }
        });
        findViewById(R.id.buttonArmor).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SetFilter(InventionType.Armor);
//                typeRelevant = InventionType.Armor;
                UpdateList();
            }
        });
        findViewById(R.id.buttonTools).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetFilter(InventionType.Tool);
//                typeRelevant = InventionType.Tool;
                UpdateList();
            }
        });
        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void SetFilter(InventionType givenFilter) {
        if (typeRelevant == givenFilter)
            typeRelevant = InventionType.Any;
        else
            typeRelevant = givenFilter;
    }

    @Override
    protected void onResume() {
        super.onResume();
        UpdateList(); // Update it in-case some equipping actions were going on...

    }

    void UpdateList(){
        UpdateItemList((ViewGroup)(findViewById(R.id.layoutRelevantItems)), typeRelevant);
    }

}
