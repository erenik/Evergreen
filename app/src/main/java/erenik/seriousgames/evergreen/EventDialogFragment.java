package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by Emil on 2016-10-30.
 */
public class EventDialogFragment extends DialogFragment
{
    Finding type = Finding.Nothing;
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(type.GetEventText()+"\nDo you want to play the event now?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // FIRE ZE MISSILES!
                        // Open new activity for this event.
                        // do it.
                        // Do the fight.
                        encounter.NewEncounter();
                        encounter.Random(new Dice(3, 2, 0));
                        encounter.Simulate();

                        // Update GUI of main activity.
                        Activity act = getActivity();
                        if (act instanceof MainScreen)
                        {
                            MainScreen ms = (MainScreen) act;
                            ms.UpdateGUI(); // Update GUI HP, log, etc.
                        }
                    }
                })
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // User cancelled the dialog

                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
