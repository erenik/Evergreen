package erenik.evergreen.android.act;

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

import erenik.util.EList;
import erenik.util.EList;

import erenik.evergreen.common.Player;
import erenik.evergreen.android.App;
import erenik.evergreen.common.player.*;
import erenik.evergreen.R;
import erenik.evergreen.android.ui.EvergreenButton;


public class SelectActivity extends EvergreenActivity {
    static final int SELECT_ACTIVE_ACTION = 2;
    static final int SELECT_DAILY_ACTION = 0;
    static final int SELECT_SKILL = 1;

    EList<String> selected = new EList<String>();
    EList<Skill> selectedSkills = new EList<Skill>();
    EList<Action> selectedActions = new EList<Action>();
    int buttonBgId = R.drawable.small_button;
    AAction activeAction = null;
    int type = -1; // Integer used when launching the activity - to know if we are choosing DActions, Skills or AActions.

    private final View.OnClickListener addItem = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button b = (Button) v;
            clicked(b);
            String text = (String) b.getText();
            Skill s = Skill.GetFromString(text);
            if (s != null){
                selected.add(text);
                updateQueue();
                return;
            }
            Action a = Action.GetFromString(text);
            if (a == null) {
                System.out.println("Action null, couldn't set properly D:");
                return;
            }
            if (a.requiredArguments.size() >= 1) {
                // Open window to confirm addition of it.
                SelectDetailsDialogFragment sdf = new SelectDetailsDialogFragment();
                sdf.a = a;
                FragmentManager fragMan = getSupportFragmentManager();
                sdf.show(fragMan, "selectDactionDetails");
                return;
            }
            selected.add(text);
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

    private final View.OnClickListener itemClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button b = (Button)v;
            clicked(b);
        }
    };

    private void clicked(Button b) {
        String text = (String) b.getText();
        Skill s = Skill.GetFromString(text);
        if (s != null){
            skillClicked(s);
        }
        Action a = Action.GetFromString(text);
        if (a == null){
            System.out.println("Clicked null-content button.");
            return;
        }
        System.out.println("Clicked: "+a.text);
        String string = a.toString().split(":")[0]; // First stuff before any eventual arguments.
        if (a != null) {
            ActionClicked(a);
        }
    }

    private void skillClicked(Skill skill) {
        TextView tvName = (TextView) findViewById(R.id.textViewItemName);
        tvName.setText(skill.text); // Show arguments here as well.
        TextView desc = (TextView) findViewById(R.id.textViewDescription);
        desc.setText(skill.briefDescription);
    }

    // For displaying default title?
    public void ActionClicked(Action action){
        ActionClicked(action, action.text);
    }
    public void ActionClicked(Action a, String header){
        TextView tvName = (TextView) findViewById(R.id.textViewItemName);
        tvName.setText(header); // Show arguments here as well.
        TextView desc = (TextView) findViewById(R.id.textViewDescription);
        desc.setText(a.description);
    }

    private final View.OnClickListener clear = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            selected.clear();
            updateQueue();
        }
    };
    private final View.OnClickListener removeParentFromQueue = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            v = (View) v.getParent(); // First get the main button.
            ViewGroup parent = (ViewGroup) v.getParent();
            // Get index.
            int index = parent.indexOfChild(v);
            selected.remove(index);
            parent.removeView(v);
            updateQueue(); // Update queue gui.
        }
    };
    // Updates queue gui.
    public void updateQueue()
    {
        // Yeah.
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutQueue);
        vg.removeAllViews();
        for (int i = 0; i < selected.size() && i < 8; ++i) // Max 8?
        {
            /// First add a LinearLayout (horizontal)
            LinearLayout ll = new LinearLayout(getBaseContext());
            // Give it an ID?
            int id  = 0;
            switch(i)
            {
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
            vg.addView(ll);

            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            b.setText(selected.get(i));
            // Screen div 10 height per element?
            layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2.f);
            layoutParams.setMargins(5,0,5,0); // Margins left n right.
            layoutParams.gravity = Gravity.CENTER;
            b.setLayoutParams(layoutParams);
            b.setOnClickListener(itemClicked);
            b.setBackgroundColor(0x00);
            b.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.mainTextColor));
            ll.addView(b);

            // Add a button in the button to remove it.
            ImageButton removeButton = new ImageButton(getBaseContext());
            removeButton.setBackgroundColor(0x00); // SEe-through?
//            removeButton.setBackgroundColor(EvergreenButton.BackgroundColor(getBaseContext()));
            removeButton.setImageResource(R.drawable.remove);
            removeButton.setScaleType(ImageView.ScaleType.FIT_CENTER); // Scale to fit?
            removeButton.setOnClickListener(removeParentFromQueue);
            removeButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 4.0f)); // Weight at the end?
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
                player.cd.dailyActions.clear();
                for (int i = 0; i < selected.size(); ++i)
                    player.cd.dailyActions.add(Action.ParseFrom(selected.get(i)));                    // Just save it as Strings?
            }
            else if (type == SELECT_SKILL) {
                player.cd.skillTrainingQueue.clear();
                for (int i = 0; i < selected.size(); ++i)
                    player.cd.skillTrainingQueue.add(selected.get(i));
            }
            else if (type == SELECT_ACTIVE_ACTION){
                player.cd.queuedActiveActions.clear();
                for (int i = 0; i < selected.size(); ++i) {
                    player.cd.queuedActiveActions.add(Action.ParseFrom(selected.get(i)));                    // Just save it as Strings?
                    System.out.println("Queued action: "+player.cd.queuedActiveActions.get(i));
                }
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
            System.out.println("Canceling");
            setResult(-1);
            finish();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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
                itemNames = Skill.Names();
                break;
        }
        TextView tv = (TextView) findViewById(R.id.textViewPossibleItems);
        tv.setText(itemsHeaderName);
        tv = (TextView) findViewById(R.id.textViewSelectTitle);
        tv.setText(header);

        /// Populate the available items.
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutItems);
        for (int i = 0; i < itemNames.size(); ++i)
        {
            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            b.setText(itemNames.get(i));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, App.GetScreenSize().y / 12);
            layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
            b.setBackgroundResource(buttonBgId);
            b.setLayoutParams(layoutParams);
            vg.addView(b);
            b.setOnClickListener(addItem);
        }
        // Load from player.
        Player p = App.GetPlayer();
        if (type == SELECT_DAILY_ACTION) {
            for (int i = 0; i < p.cd.dailyActions.size(); ++i)
                selected.add(p.cd.dailyActions.get(i).toString());
        }
        else if (type == SELECT_SKILL) {
            for (int i = 0; i < p.cd.skillTrainingQueue.size(); ++i)
                selected.add(p.cd.skillTrainingQueue.get(i));
        }
        if (type == SELECT_ACTIVE_ACTION) {
            for (int i = 0; i < p.cd.queuedActiveActions.size(); ++i)
                selected.add(p.cd.queuedActiveActions.get(i).toString());
        }
        // Update the queue
        updateQueue();

        findViewById(R.id.buttonConfirm).setOnClickListener(confirm);
        findViewById(R.id.buttonClear).setOnClickListener(clear);
        findViewById(R.id.buttonCancel).setOnClickListener(cancel);
    }
}
