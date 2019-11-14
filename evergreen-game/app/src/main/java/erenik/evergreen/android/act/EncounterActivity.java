package erenik.evergreen.android.act;

import android.os.Bundle;

import erenik.evergreen.android.App;
import erenik.evergreen.*;
import erenik.evergreen.common.Player;

public class EncounterActivity extends EvergreenActivity
{
    Player player = App.GetPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter);
        // Hook up some GUI stuffs.

        // Update gui.
        // ?

    }
    void UpdateGui()
    {
        // HP, attack, defense, debuffs, enemies.
    }

}
