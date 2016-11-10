package erenik.seriousgames.evergreen.act;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import erenik.seriousgames.evergreen.EvergreenButton;
import erenik.seriousgames.evergreen.R;
import erenik.seriousgames.evergreen.player.ActionArgument;
import erenik.seriousgames.evergreen.player.DAction;
import erenik.seriousgames.evergreen.Invention.Invention;
import erenik.seriousgames.evergreen.Invention.InventionType;
import erenik.seriousgames.evergreen.player.Player;
import erenik.seriousgames.evergreen.player.Skill;
import erenik.seriousgames.evergreen.player.Transport;

import java.util.List;

/**
 * Created by Emil on 2016-10-30.
 */
public class SelectDetailsDialogFragment extends DialogFragment
{
    DAction da = null;
    Player player = Player.getSingleton();
    List<View> argumentViews = new ArrayList<>();

    // When confirming everything. Add the action?
    DialogInterface.OnClickListener onClickOK = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int id)
        {
            Activity act = getActivity();
            System.out.println("Activity: "+act.toString());
            if (act instanceof SelectActivity)
            {
                // Update GUI of main activity.
                SelectActivity sa = (SelectActivity) act;
                String text = da.text;
                // Fetch arguments as set in the ui?
                if (argumentViews.size() > 0)
                    text += ": ";
                for (int i = 0; i < argumentViews.size(); ++i)
                {
                    View v = argumentViews.get(i);
                    System.out.println("View: "+v.toString());
                    if (v instanceof Spinner) {
                        Spinner s = (Spinner) v;
                        String t = s.getSelectedItem().toString();
                        System.out.println("Selected text: "+t);
                        text += t;
                    }
                    else if (v instanceof EditText)
                    {
                        EditText et = (EditText) v;
                        String s = et.getText().toString();
                        System.out.println("text: "+s);
                        text += s;
                    }
                    if (i < argumentViews.size() - 1)
                        text += ", ";
                }

                // Add the text?
                sa.selected.add(text);
                sa.dActionClicked(da);
                sa.updateQueue(); // Update queue gui.
            }
        }
    };

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState)
    {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String header = "Action details:";
        switch(da)
        {
            case AugmentTransport: header = "Augment transport. Choose transport and upgrade type."; break;
            case Craft: header = "What to craft?"; break;
            case Invent: header = "What category of inventions do you want to try and invent?"; break;
            case Expedition: header = "Which stronghold do you want to attack?"; break;
            case AttackAPlayer: header = "Which player do you want to attack?"; break;
            case Steal: header = "Which player do you want to steal from?"; break;
            case LookForPlayer: header = "Enter the name of the player you are looking for"; break;
            case Study: header = "Choose a skill to study further"; break;
            default:
                header = "Set additional arguments";
                break;
        }
        builder.setMessage(header);
        builder
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
        builder.setCancelable(true); // Not cancelable?

        // Adda  scroll-layout?
        LinearLayout mainLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
        mainLayout.setBackgroundColor(EvergreenButton.BackgroundColor(getContext()));
        boolean textInput = false;
        boolean possible = false;
        for (int ra = 0; ra < da.requiredArguments.size(); ++ra)
        {
            ActionArgument aarg = da.requiredArguments.get(ra);
            List<String> choices = new ArrayList<String>();
            switch(aarg)
            {
                case Transport:
                    choices = Transport.GetStrings();
                    break;
                case TransportAugment:
                    for (int j = 0; j < player.inventions.size(); ++j)
                    {
                        if (player.inventions.get(j).type == InventionType.VehicleUpgrade)
                            choices.add(player.inventions.get(j).name);
                    }
                    break;
                case Player:
                    choices = player.KnownPlayerNames();
                    break;
                case TextSearchType:
                    choices.add("Exactly");
                    choices.add("Contains");
                    choices.add("Starts with");
                    break;
//                        ;; Contains, Exactly, StartsWith
                case PlayerName:
                    // Text input only.
                    textInput = true;
                    possible = true;
                    break;
                case Stronghold:
                    // Known ones only?
                    choices = player.knownStrongholds;
                    break;
                case InventionCategory:
                    choices = InventionType.GetStrings();
                    break;
                case InventionToCraft:
                    System.out.println("InventionToCraft");
                    for (int j = 0; j < player.inventions.size(); ++j)
                    {
                        Invention inv = player.inventions.get(j);
                        if (inv.IsCraftable()) {
                            System.out.println("inv: "+inv.name);
                            choices.add(inv.name);
                        }
                    }
                    System.out.println("InventionToCraft - 2");
                    break;
                case SkillToStudy:
                    choices = Skill.Names();
                    break;
                default:
                    System.out.println("Bad thing to select from");
                    System.exit(4);
            }
            if (choices.size() > 0)
                possible = true;
            LinearLayout.LayoutParams selectionLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.f);
            //selectionLayoutParams.setOrientation(LinearLayout.HORIZONTAL);
            selectionLayoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));

            // addView(ll);
            if (choices.size() > 0)
            {
                // Add choices to the thingy.
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item);
                arrayAdapter.addAll(choices);
                Spinner spinner = new Spinner(getContext());
                spinner.setAdapter(arrayAdapter);
                spinner.setLayoutParams(selectionLayoutParams);
                mainLayout.addView(spinner);
                argumentViews.add(spinner);
            }
            if (textInput)
            {
                // Name ?
                EditText et = new EditText(getContext());
                et.setId(R.id.nameInput);
                et.setLayoutParams(selectionLayoutParams);
                mainLayout.addView(et);
                argumentViews.add(et);
            }
        }
        if (possible)
            builder.setPositiveButton("OK", onClickOK);
        else
        {
            // Add error messages?
            TextView tv = new TextView(getContext());
            String text = "Unable to perform action.";
            switch(da)
            {
                case AugmentTransport:
                case Craft:
                    text = "No craftable inventions found.";
                    break;
            }
            tv.setText(text);
            mainLayout.addView(tv);
        }

        builder.setView(mainLayout);
        Dialog d = builder.show();
        // Create the AlertDialog object and return it
        return d;// builder.create();
    }

}
