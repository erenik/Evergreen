package evergreen.android.act;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import evergreen.common.player.AAction;
import evergreen.common.player.Action;
import evergreen.common.player.SkillType;
import evergreen.util.EList;

import evergreen.android.App;
import evergreen.R;
import evergreen.common.player.ActionArgument;
import evergreen.common.player.DAction;
import evergreen.common.Invention.Invention;
import evergreen.common.Invention.InventionType;
import evergreen.common.Player;
import evergreen.common.player.Skill;
import evergreen.common.player.Transport;
import evergreen.util.Printer;
import transport.TransportType;

import evergreen.util.EList;

/**
 * Used for...
 * Created by Emil on 2016-10-30.
 */
public class SelectDetailsDialogFragment extends DialogFragment {
    Action action = null;

    Player player = App.GetPlayer();
    EList<View> argumentViews = new EList<>();

    // When confirming everything. Add the action?
    DialogInterface.OnClickListener onClickOK = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int id)
        {
            Activity act = getActivity();
            Printer.out("Activity: "+act.toString());
            if (act instanceof SelectActivity) {
                SelectActivity sa = (SelectActivity) act;   // Update GUI of main activity.
                sa.setupFullscreenFlags(); // Remove stuff?                 // Hide system buttons?
                for (int i = 0; i < argumentViews.size(); ++i) {
                    View v = argumentViews.get(i);
                    Printer.out("View: "+v.toString());
                    String valueSet = "";
                    if (v instanceof Spinner) {
                        Spinner s = (Spinner) v;
                        valueSet = s.getSelectedItem().toString();
                    }
                    else if (v instanceof EditText) {
                        EditText et = (EditText) v;
                        valueSet = et.getText().toString();
                        if (valueSet.length() > 128)
                            valueSet = valueSet.substring(0, 128); // Substring if too long :) max 100 chars per message?
                    }
                    if (action.requiredArguments.size() < i)
                        continue;
                    Printer.out("Argument: "+action.requiredArguments.get(i).name()+" value: "+valueSet);
                    action.requiredArguments.get(i).value = valueSet;
                }
                // Add the text?\
                sa.selectedActions.add(action);
                sa.ActionClicked(action, false);
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
        header = "Details: "+action.text;
        /*
        switch(da)
        {
            //case AugmentTransport: header = "Augment transport. Choose transport and upgrade type."; break;
            case Craft: header = "What to craft?"; break;
            case Invent: header = "What category of inventions do you want to try and invent?"; break;
      //      case Expedition: header = "Which stronghold do you want to attack?"; break;
            case AttackAPlayer: header = "Which player do you want to attack?"; break;
            case Steal: header = "Which player do you want to steal from?"; break;
            case LookForPlayer: header = "Enter the name of the player you are looking for"; break;
            case Study: header = "Choose a skill to study further"; break;
            default:
                header = "Set additional arguments";
                break;
        }*/
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
        mainLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN); // Hide the back/home/change app buttons still...!

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
        mainLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.mainButtonBackground));
        boolean textInput = false;
        boolean possible = false;
        possible = true;
        String reason = "";
        EList<View> tempList = new EList<>();
        for (int ra = 0; ra < action.requiredArguments.size(); ++ra) {
            boolean numbersOnly = false;
            ActionArgument aarg = action.requiredArguments.get(ra);
            EList<String> choices = new EList<String>();
            switch(aarg)
            {
                case Transport:
                    choices = TransportType.GetStrings();
                    break;
                /*
                case TransportAugment:
                    for (int j = 0; j < player.inventions.size(); ++j) {
                        if (player.inventions.get(j).type == InventionType.VehicleUpgrade)
                            choices.add(player.inventions.get(j).name);
                    }
                    break;
                    */
                case Player:
                    choices = player.KnownPlayerNames();
                    Printer.out("known players: "+choices.size());
                    if (choices.size() == 0) {
                        possible = false;
                        reason = "No known players.";
                    }
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
                    break;
                case Stronghold:
                    // Known ones only?
                    choices = player.knownStrongholds;
                    break;
                case InventionCategory:
                    choices = InventionType.GetStrings();
                    break;
                case InventionToCraft:
                    Printer.out("InventionToCraft");
                    for (int j = 0; j < player.cd.inventionBlueprints.size(); ++j) {
                        Invention inv = player.cd.inventionBlueprints.get(j);
                        if (inv.IsCraftable()) {
                            Printer.out("inv: "+inv.name);
                            choices.add(inv.name);
                        }
                    }
                    if (choices.size() == 0) {
                        possible = false;
                        reason = "No invention blueprints available.";
                    }
                    break;
                case SkillToStudy:
                    choices = SkillType.Names();
                    break;
                case ResourceType:
                    choices = new EList<>();
                    choices.add("Food");
                    choices.add("Materials");
                    break;
                case ResourceQuantity:
                    textInput = true;
                    numbersOnly = true;
                    break;
                case Text:
                    textInput = true;
                    break;
                case Item:
                    Printer.out("Select Item");
                    for (int ii = 0; ii < player.cd.inventory.size(); ++ii){
                        Invention item = player.cd.inventory.get(ii);
                        choices.add(item.name);
                    }
                    if (choices.size() == 0){
                        possible = false;
                        reason = "No items available.";
                    }
                    break;
                case Blueprint:
                    Printer.out("Select blueprint");
                    for (int bi = 0; bi < player.cd.inventionBlueprints.size(); ++bi){
                        Invention blueprint = player.cd.inventionBlueprints.get(bi);
                        choices.add(blueprint.name);
                    }
                    if (choices.size() == 0){
                        possible = false;
                        reason = "No blueprints available.";
                    }
                    break;
                default:
                    Printer.out("Bad thing to select from: "+aarg.name());
                    new Exception().printStackTrace();
                    System.exit(4);
            }
            LinearLayout.LayoutParams selectionLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.f);
            //selectionLayoutParams.setOrientation(LinearLayout.HORIZONTAL);
            selectionLayoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));

            // addView(ll);
            if (choices.size() > 0) {
                // Add choices to the thingy.
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item);
                arrayAdapter.addAll(choices.asArrayList());
                Spinner spinner = new Spinner(getContext());
                spinner.setAdapter(arrayAdapter);
                spinner.setLayoutParams(selectionLayoutParams);
                tempList.add(spinner);
            }
            if (textInput) {
                // Name ?
                EditText et = new EditText(getContext());
                et.setId(R.id.nameInput);
                et.setLayoutParams(selectionLayoutParams);
                if (numbersOnly) {
                    et.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    et.setInputType(InputType.TYPE_CLASS_PHONE);
                }
                tempList.add(et);
            }
        }
        if (possible) {
            builder.setPositiveButton("OK", onClickOK);
            for (int i = 0; i < tempList.size(); ++i){
                mainLayout.addView(tempList.get(i));
            }
            argumentViews = tempList;
        }
        else {
            // Add error messages?
            TextView tv = new TextView(getContext());
            String text = "Unable to perform action. "+reason;
            if (action.DailyAction() != null)
                switch(action.DailyAction()) {
    //                case AugmentTransport:
                    case Craft:
                        text = "No craftable inventions found.";
                        break;
                }
            tv.setText(text);
            mainLayout.addView(tv);
        }

        builder.setView(mainLayout);
        AlertDialog d = builder.show();
        // Create the AlertDialog object and return it
        return d;// builder.create();
    }

}
