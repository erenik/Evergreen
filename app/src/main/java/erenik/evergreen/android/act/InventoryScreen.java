package erenik.evergreen.android.act;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import erenik.evergreen.R;
import erenik.evergreen.common.Invention.InventionType;

/**
 * Created by Emil on 2017-02-26.
 */

public class InventoryScreen extends EvergreenActivity {
    public enum ReqCode {
        DisplayWeapons,
        DisplayArmors,
        DisplayTools,
        DisplayAll,
    };
    /// Populate the inventory.
    InventionType typeRelevant = InventionType.Weapon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        // Check reqID?
        int reqCode = getIntent().getIntExtra("ReqCode", 0);
        ReqCode rc = ReqCode.values()[reqCode];
        System.out.println("Req code: "+reqCode+" rc: "+rc);
        switch (rc){
            case DisplayWeapons: typeRelevant = InventionType.Weapon; break;
            case DisplayArmors: typeRelevant = InventionType.Armor; break;
            case DisplayTools: typeRelevant = InventionType.Tool; break;
        }

        UpdateList();
        findViewById(R.id.buttonWeapons).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                typeRelevant = InventionType.Weapon;
                UpdateList();
            }
        });
        findViewById(R.id.buttonArmor).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                typeRelevant = InventionType.Armor;
                UpdateList();
            }
        });
        findViewById(R.id.buttonTools).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                typeRelevant = InventionType.Tool;
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

    void UpdateList(){
        UpdateItemList((ViewGroup)(findViewById(R.id.layoutRelevantItems)), typeRelevant);
    }

}
