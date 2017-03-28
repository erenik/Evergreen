package erenik.evergreen.android.act;

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

import erenik.evergreen.common.player.AAction;
import erenik.evergreen.common.player.Action;
import erenik.util.EList;

import erenik.evergreen.android.App;
import erenik.evergreen.R;
import erenik.evergreen.common.player.ActionArgument;
import erenik.evergreen.common.player.DAction;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Invention.InventionType;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.player.Skill;
import erenik.evergreen.common.player.Transport;
import erenik.weka.transport.TransportType;

import erenik.util.EList;

/**
 * Created by Emil on 2016-10-30.
 */
public class SelectDetailsDialogFragment extends DialogFragment {
    Action a = null;

    Player player = App.GetPlayer();
    EList<View> argumentViews = new EList<>();

    // When confirming everything. Add the action?
    DialogInterface.OnClickListener onClickOK = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int id)
        {
            Activity act = getActivity();
            System.out.println("Activity: "+act.toString());
            if (act instanceof SelectActivity) {
                // Update GUI of main activity.
                SelectActivity sa = (SelectActivity) act;
                // Hide system buttons?
                sa.setupFullscreenFlags(); // Remove stuff?
                String text = a.text;
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
                sa.ActionClicked(a);
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
        header = "Details: "+a.text;
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
        for (int ra = 0; ra < a.requiredArguments.size(); ++ra) {
            boolean numbersOnly = false;
            ActionArgument aarg = a.requiredArguments.get(ra);
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
                    System.out.println("InventionToCraft");
                    for (int j = 0; j < player.cd.inventions.size(); ++j) {
                        Invention inv = player.cd.inventions.get(j);
                        if (inv.IsCraftable()) {
                            System.out.println("inv: "+inv.name);
                            choices.add(inv.name);
                        }
                    }
                    if (choices.size() == 0) {
                        possible = false;
                        reason = "No invention blueprints available.";
                    }
                    break;
                case SkillToStudy:
                    choices = Skill.Names();
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
                default:
                    System.out.println("Bad thing to select from: "+aarg.name());
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
            if (a.daType != null)
                switch(a.daType) {
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
