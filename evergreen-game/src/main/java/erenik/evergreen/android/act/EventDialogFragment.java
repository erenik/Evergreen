package erenik.evergreen.android.act;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import erenik.evergreen.common.Player;
import erenik.evergreen.android.App;
import erenik.evergreen.common.encounter.Encounter;
import erenik.evergreen.common.player.Finding;
import erenik.util.Printer;

/**
 * Created by Emil on 2016-10-30.
 */
public class EventDialogFragment extends DialogFragment
{
    public Finding type = Finding.Nothing;
    EncounterActivity encounterActivity = new EncounterActivity();

    Encounter enc = new Encounter(0);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Printer.out("onCreate");
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        /*
        enc.listeners.add(new EncounterListener() {
            @Override
            public void OnEncounterEnded(Encounter enc) {
                // TODO: Save or stuff?
                App.Save();
            }
        });
*/
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
            /*
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
                */
        }
        builder.setMessage(type.GetEventText()+moreText)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        /// Set that the user clicked yes -> Offer next event after this one finishes.
                        Player player = App.GetPlayer();
                        Printer.out("event type: "+type.GetEventText());
                        switch (type) {
                            case AttacksOfTheEvergreen:
                                AttacksOfTheEvergreen();
                                break;
                            case RandomEncounter:
                                RandomEncounter(player);
                                break;
//                            case AbandonedShelter:
  //                              enc.AbandonedShelter();
    //                            break;
                           // case RandomPlayerShelter:
                             //   enc.RandomPlayerShelter();
                               // break;
                            default:
                                throw new NullPointerException();
                        }
                        // Update GUI of main activity.
                        Activity act = getActivity();
                        if (act instanceof EvergreenActivity) {
                            EvergreenActivity ms = (EvergreenActivity) act;
                            ms.UpdateUI(); // Update GUI HP, log, etc.
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
      //  enc.NewEncounter(true);
        //enc.AssaultsOfTheEvergreen();
        //enc.Simulate();
    }

    void RandomEncounter(Player attackedPlayer)
    {
        // Open new activity for this event?
//        enc.NewEncounter(false);
  //      enc.RandomMonsterEncounter(new Dice(3, 2, 0), attackedPlayer);
    //    enc.Simulate();
    }
}
