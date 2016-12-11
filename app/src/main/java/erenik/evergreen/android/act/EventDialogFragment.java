package erenik.evergreen.android.act;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import erenik.evergreen.Player;
import erenik.evergreen.android.App;
import erenik.evergreen.logging.LogType;
import erenik.evergreen.player.Finding;
import erenik.evergreen.player.Stat;
import erenik.evergreen.util.Dice;

/**
 * Created by Emil on 2016-10-30.
 */
public class EventDialogFragment extends DialogFragment
{
    public Finding type = Finding.Nothing;
    Encounter enc = new Encounter();
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String moreText = "\n\nDo you want to play the event now?";
        Player player = App.GetPlayer();
        boolean skippable = true;
        float units = 0.f;
        skippable = type.Skippable();
        moreText = type.Question();
        switch(type)
        {
            case AbandonedShelter:
            case RandomPlayerShelter:
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
                enc.Log("You find " + units + " of materials.", LogType.INFO);
                skippable = true;
                break;
            case FoodHotSpot:
                units = Dice.RollD3(2)+1;
                player.Adjust(Stat.FOOD, units);
                enc.Log("You find " + units + " of food.", LogType.INFO);
                skippable = true;
                break;
        }
        builder.setMessage(type.GetEventText()+moreText)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        /// Set that the user clicked yes -> Offer next event after this one finishes.
                        Player player = App.GetPlayer();
                        System.out.println("event type: "+type.GetEventText());
                        switch (type) {
                            case AttacksOfTheEvergreen:
                                AttacksOfTheEvergreen();
                                break;
                            case Encounter:
                                RandomEncounter();
                                break;
                            case AbandonedShelter:
                                enc.AbandonedShelter();
                                break;
                            case RandomPlayerShelter:
                                enc.RandomPlayerShelter();
                                break;
                            default:
                                throw new NullPointerException();
                        }
                        // Update GUI of main activity.
                        Activity act = getActivity();
                        if (act instanceof MainScreen) {
                            MainScreen ms = (MainScreen) act;
                            ms.UpdateGUI(); // Update GUI HP, log, etc.
                            App.HandleNextEvent();
                        }
                    }
                });
        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                Player player = App.GetPlayer();
                player.playEvents = false; // Flag that the player may be interested in seeing the next event being played when this one finishes.
            }
        });
        builder.setCancelable(!skippable); // Not cancelable?
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private void AttacksOfTheEvergreen()
    {
        enc.NewEncounter(true);
        enc.AssaultsOfTheEvergreen();
        enc.Simulate();
    }

    void RandomEncounter()
    {
        // Open new activity for this event?
        enc.NewEncounter(false);
        enc.Random(new Dice(3, 2, 0));
        enc.Simulate();
    }
}
