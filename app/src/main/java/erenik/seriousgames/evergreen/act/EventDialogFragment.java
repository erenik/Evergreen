package erenik.seriousgames.evergreen.act;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import erenik.seriousgames.evergreen.player.Finding;
import erenik.seriousgames.evergreen.logging.*;
import erenik.seriousgames.evergreen.player.*;
import erenik.seriousgames.evergreen.util.Dice;

/**
 * Created by Emil on 2016-10-30.
 */
public class EventDialogFragment extends DialogFragment
{
    public Finding type = Finding.Nothing;
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String moreText = "\n\nDo you want to play the event now?";
        Player player = Player.getSingleton();
        boolean skippable = true;
        float units = 0.f;
        skippable = type.Skippable();
        moreText = type.Question();
        switch(type)
        {
            case AbandonedShelter:
            {
                skippable = true;
                break;
            }
            case AttacksOfTheEvergreen:
            case Encounter:
                skippable = false;
                break;
            case MaterialsDepot:
                units = Dice.RollD3(2)+1;
                player.Adjust(Stat.MATERIALS, units);
                encounter.Log("You find " + units + " of materials.", LogType.INFO);
                skippable = true;
                break;
            case FoodHotSpot:
                units = Dice.RollD3(2)+1;
                player.Adjust(Stat.FOOD, units);
                encounter.Log("You find "+units+" of food.", LogType.INFO);
                skippable = true;
                break;
        }
        builder.setMessage(type.GetEventText()+moreText)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        switch (type) {
                            case AttacksOfTheEvergreen:
                                AttacksOfTheEvergreen();
                                break;
                            case Encounter:
                                RandomEncounter();
                        }
                        // Update GUI of main activity.
                        Activity act = getActivity();
                        if (act instanceof MainScreen) {
                            MainScreen ms = (MainScreen) act;
                            ms.UpdateGUI(); // Update GUI HP, log, etc.
                            ms.HandleNextEvent(); // If there is any.
                        }
                    }
                });
        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        builder.setCancelable(!skippable); // Not cancelable?
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void AttacksOfTheEvergreen()
    {
        encounter.NewEncounter(true);
        encounter.AssaultsOfTheEvergreen();
        encounter.Simulate();
    }

    void RandomEncounter()
    {
        // Open new activity for this event?
        encounter.NewEncounter(false);
        encounter.Random(new Dice(3, 2, 0));
        encounter.Simulate();
    }
}
