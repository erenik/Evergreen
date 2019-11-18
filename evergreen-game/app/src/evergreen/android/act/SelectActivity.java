package evergreen.android.act;

import android.content.pm.ActivityInfo;
import android.database.DataSetObserver;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import evergreen.util.EList;
import evergreen.util.EList;

import evergreen.common.Player;
import evergreen.android.App;
import evergreen.common.player.*;
import evergreen.R;
import evergreen.android.ui.EvergreenButton;
import evergreen.util.Printer;


public class SelectActivity extends EvergreenActivity {
    static final int SELECT_ACTIVE_ACTION = 2;
    static final int SELECT_DAILY_ACTION = 0;
    static final int SELECT_SKILL = 1;

//    EList<String> selected = new EList<String>();
    EList<SkillType> selectedSkills = new EList<SkillType>();
    EList<Action> selectedActions = new EList<Action>();
    int buttonBgId = R.drawable.small_button;
    AAction activeAction = null;
    int type = -1; // Integer used when launching the activity - to know if we are choosing DActions, Skills or AActions.


    EList<View> queueButtons = new EList<>(), // To see the details or edit.
        removeButtons = new EList<>(); // To remove them from the queue.

    private final View.OnClickListener addItem = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button) v;
            itemClicked.onClick(b);
            String text = (String) b.getText();
            SkillType s = SkillType.GetFromString(text);
            if (s != null){ // If it's a skill, just add the text? No?
                selectedSkills.add(s);
                updateQueue();
                return;
            }
            Action action = Action.GetFromString(text);
            if (action == null) {
                Printer.out("Action null, couldn't set properly D:");
                return;
            }
            if (action.requiredArguments.size() >= 1) {
                // Open window to confirm addition of it.
                SelectDetailsDialogFragment sdf = new SelectDetailsDialogFragment();
                sdf.action = action;
                FragmentManager fragMan = getSupportFragmentManager();
                sdf.show(fragMan, "selectDactionDetails");
                return;
            }
            selectedActions.add(action);
            updateQueue(); // Update queue gui.
        }
    };

/*    Button okButton = new Button(getBaseContext());
    okButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Verify arguments first?
            App.GetPlayer().cd.queuedActiveActions.add(activeAction);                            // Save it?
            App.DoQueuedActions();                            // Do it?
        }
    });
*/

    private final View.OnClickListener itemClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button)v;
            String text = (String) b.getText();
            SkillType s = SkillType.GetFromString(text);
            if (s != null){
                skillClicked(s);
            }
            Action a = Action.GetFromString(text);
            if (a == null){
                Printer.out("Clicked null-content button.");
                return;
            }
            Printer.out("Clicked: "+a.text);
            String string = a.toString().split(":")[0]; // First stuff before any eventual arguments.
            if (a != null) {
                ActionClicked(a, false);
            }
        }
    };

    private final View.OnClickListener itemClickedInQueue = new View.OnClickListener() {
        @Override
        public void onClick(View v){
            Button b = (Button) v;
            String text = (String) b.getText();
            SkillType s = SkillType.GetFromString(text);
            if (s != null){
                skillClicked(s);
                return;
            }
            Action a = null;
            if (queueButtons.contains(b)){
                Printer.out("Is queue button1!!!1");
                int index = queueButtons.indexOf(b);
                a = selectedActions.get(index);
            }
            else {
                Printer.out("is not queue button...");
            }
            if (a == null){
                Printer.out("Clicked null-content button.");
                return;
            }
            Printer.out("Clicked: "+a.text);
            String string = a.toString().split(":")[0]; // First stuff before any eventual arguments.
            if (a != null) {
                ActionClicked(a, true);
            }
        }
    };

    private void skillClicked(SkillType skill) {
        TextView tvName = (TextView) findViewById(R.id.textViewItemName);
        tvName.setText(skill.text); // Show arguments here as well.
        TextView desc = (TextView) findViewById(R.id.textViewDescription);
        desc.setText(skill.briefDescription);
    }

    // For displaying default title?
    public void ActionClicked(Action action, boolean inQueue){
        TextView tvName = (TextView) findViewById(R.id.textViewItemName);
        tvName.setText(action.text); // Show arguments here as well.
        TextView desc = (TextView) findViewById(R.id.textViewDescription);

        String argsStr = "\n";
        if (inQueue && action.requiredArguments != null)
            for (int i = 0; i < action.requiredArguments.size(); ++i){
                ActionArgument actionArg = action.requiredArguments.get(i);
                if (actionArg.value.length() > 0)
                    argsStr += "\n" + actionArg.name()+": "+actionArg.value;
            }
        desc.setText(action.description + argsStr);
    }

    private final View.OnClickListener clear = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            selectedActions.clear();
            selectedSkills.clear();
            updateQueue();
        }
    };
    private final View.OnClickListener removeParentFromQueue = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v = (View) v.getParent(); // First get the main button.
            ViewGroup parent = (ViewGroup) v.getParent();
            int index = parent.indexOfChild(v);             // Get index. or check the lists of views?
            if (selectedSkills.size() > 0)
                selectedSkills.remove(index);
            else if (selectedActions.size() > 0)
                selectedActions.remove(index);
            parent.removeView(v);
            updateQueue(); // Update queue gui.
        }
    };
    // Updates queue gui.
    public void updateQueue() {
        queueButtons.clear();
        removeButtons.clear();
        // Yeah.
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutQueue);
        vg.removeAllViews();
        EList<Object> selected = (EList<Object>) (selectedActions.size()  > 0? selectedActions : selectedSkills);
        for (int i = 0; i < selected.size() && i < 8; ++i){ // Max 8?
            /// First add a LinearLayout (horizontal)
            LinearLayout ll = new LinearLayout(getBaseContext());
            // Give it an ID?
            int id  = 0;
            switch(i) {
                case 0: id = R.id.queueLayout0; break;
                case 1: id = R.id.queueLayout1; break;
                case 2: id = R.id.queueLayout2; break;
                case 3: id = R.id.queueLayout3; break;
                case 4: id = R.id.queueLayout4; break;
                case 5: id = R.id.queueLayout5; break;
                case 6: id = R.id.queueLayout6; break;
                case 7: id = R.id.queueLayout7; break;
            }
            ll.setId(id);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, App.GetScreenSize().y / 10);
            layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
            ll.setLayoutParams(layoutParams);
            ll.setBackgroundResource(buttonBgId);
            if (type == SELECT_SKILL){
                SkillType selectedSkillType = selectedSkills.get(i);
                int skillLevel = App.GetPlayer().Get(selectedSkillType).Level();
                if (skillLevel == SkillType.values()[i].MaxLevel()) // Capped level?
                    skillLevel = 9;
                ll.setBackgroundResource(GetDrawableForSkillLevel(skillLevel));
            }
            vg.addView(ll);

            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            // Now for the text..
            if (selected.get(i) instanceof Action){
                Action a = (Action) selected.get(i);
                b.setText(a.text);
            }
            else {
                SkillType s = (SkillType) selected.get(i);
                b.setText(s.text);
            }
            // Screen div 10 height per element?
            layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2.f);
            layoutParams.setMargins(5,0,5,0); // Margins left n right.
            layoutParams.gravity = Gravity.CENTER;
            b.setLayoutParams(layoutParams);
            b.setOnClickListener(itemClickedInQueue);
            b.setBackgroundColor(0x00);
            b.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.mainTextColor));
            queueButtons.add(b);
            ll.addView(b);

            // Add a button in the button to remove it.
            ImageButton removeButton = new ImageButton(getBaseContext());
            removeButton.setBackgroundColor(0x00); // SEe-through?
//            removeButton.setBackgroundColor(EvergreenButton.BackgroundColor(getBaseContext()));
            removeButton.setImageResource(R.drawable.remove);
            removeButton.setScaleType(ImageView.ScaleType.FIT_CENTER); // Scale to fit?
            removeButton.setOnClickListener(removeParentFromQueue);
            removeButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 4.0f)); // Weight at the end?
            removeButtons.add(removeButton);
            ll.addView(removeButton);
        }
    }

    private final View.OnClickListener confirm = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            Player player = App.GetPlayer();
            // Check stuff in list at present. Catenate. Return result? Save into player?
            if (type == SELECT_DAILY_ACTION) {
                player.cd.dailyActions = selectedActions;
            }
            else if (type == SELECT_SKILL) {
                player.cd.skillTrainingQueue.clear();
                for (int i = 0; i < selectedSkills.size(); ++i)
                    player.cd.skillTrainingQueue.add(selectedSkills.get(i).text);
            }
            else if (type == SELECT_ACTIVE_ACTION){
                player.cd.queuedActiveActions = selectedActions;
            }
            App.SaveLocally(); // Save the updates first.
            // It will probably auto-save when returning to the main-screen. Do not do this here....
//            SaveToServer(null); // Try save to server, ignore checking the updates for now.
            finish();
        }
    };
    private final View.OnClickListener cancel = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            // Return it?
            Printer.out("Canceling");
            setResult(-1);
            finish();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        type = getIntent().getIntExtra("Type", 0);

        String itemsHeaderName = "Possible items";
        String header = "heeeee";
        EList<String> itemNames = new EList<String>();
        switch(type)
        {
            case SELECT_DAILY_ACTION:
                header = "Select Daily Action";
                itemsHeaderName = "Daily actions";
                itemNames = DAction.Names();
                break;
            case SELECT_ACTIVE_ACTION:
                header = "Active actions";
                itemsHeaderName = "Details";
                itemNames = AAction.Names();
                break;
            case SELECT_SKILL:
                header = "Select skill to train";
                itemsHeaderName = "Trainable Skills";
                itemNames = SkillType.Names();
                break;
        }
        TextView tv = (TextView) findViewById(R.id.textViewPossibleItems);
        tv.setText(itemsHeaderName);
        tv = (TextView) findViewById(R.id.textViewSelectTitle);
        tv.setText(header);

        Player p = App.GetPlayer();

        /// Populate the available items.
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutItems);
        for (int i = 0; i < itemNames.size(); ++i)
        {
            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            b.setText(itemNames.get(i));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, App.GetScreenSize().y / 12);
            layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
            if (type == SELECT_SKILL){
                int skillLevel = p.Get(SkillType.values()[i]).Level();
                if (skillLevel == SkillType.values()[i].MaxLevel()) // Capped level?
                    skillLevel = 9;
                b.setBackgroundResource(GetDrawableForSkillLevel(skillLevel));
            }
            else
                b.setBackgroundResource(buttonBgId);
            b.setLayoutParams(layoutParams);
            vg.addView(b);
            b.setOnClickListener(addItem);
        }
        // Load from player.
        if (type == SELECT_DAILY_ACTION) {
            selectedActions = p.cd.dailyActions;
        }
        else if (type == SELECT_SKILL) {
            selectedSkills.clear();
            for (int i = 0; i < p.cd.skillTrainingQueue.size(); ++i)
                selectedSkills.add(SkillType.GetFromString(p.cd.skillTrainingQueue.get(i)));
        }
        if (type == SELECT_ACTIVE_ACTION) {
            selectedActions = p.cd.queuedActiveActions;
        }
        // Update the queue
        updateQueue();

        findViewById(R.id.buttonConfirm).setOnClickListener(confirm);
        findViewById(R.id.buttonClear).setOnClickListener(clear);
        findViewById(R.id.buttonCancel).setOnClickListener(cancel);
    }

    private int GetDrawableForSkillLevel(int skillLevel) {
        int id = R.drawable.skill_button_level_0;
        switch (skillLevel){
            case 1: id = R.drawable.skill_button_level_1; break;
            case 2: id = R.drawable.skill_button_level_2; break;
            case 3: id = R.drawable.skill_button_level_3; break;
            case 4: id = R.drawable.skill_button_level_4; break;
            case 5: id = R.drawable.skill_button_level_5; break;
            case 6: id = R.drawable.skill_button_level_6; break;
            case 7: id = R.drawable.skill_button_level_7; break;
            case 8: id = R.drawable.skill_button_level_8; break;
            case 9: id = R.drawable.skill_button_level_9; break;
        }
        return id;
    }
}
