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

import java.util.ArrayList;
import java.util.List;

import erenik.evergreen.common.Player;
import erenik.evergreen.android.App;
import erenik.evergreen.common.player.*;
import erenik.evergreen.R;
import erenik.evergreen.android.ui.EvergreenButton;


public class SelectActivity extends EvergreenActivity
{
    static final int SELECT_ACTIVE_ACTION = 2;
    static final int SELECT_DAILY_ACTION = 0;
    static final int SELECT_SKILL = 1;

    List<String> selected = new ArrayList<String>();
    List<Skill> selectedSkills = new ArrayList<Skill>();
    List<DAction> selectedDActions = new ArrayList<DAction>();
    int buttonBgId = R.drawable.small_button;
    AAction activeAction = null;

    private final View.OnClickListener addItem = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button b = (Button)v;
            String text = b.getText().toString();
            if (type == SELECT_ACTIVE_ACTION){
                // o-o
                // Open display with further options
                activeAction = AAction.GetFromString(text);
                ViewGroup vg = (ViewGroup) findViewById(R.id.layoutQueue);
                vg.removeAllViews();
                switch (activeAction){
                    case GiveResources:
                        Spinner spinPlayer = new Spinner(getBaseContext());


                        ArrayAdapter<String> adapter = new ArrayAdapter(getBaseContext(), android.R.layout.simple_spinner_item);
                        //.createFromResource(this, arrayID, android.R.layout.simple_spinner_item);// Specify the layout to use when the list of choices appears
                        // Add choices to the thingy.
                        List<String> choices = App.GetPlayer().knownPlayerNames;
                        adapter.addAll(choices);
                        adapter.setDropDownViewResource(R.layout.evergreen_spinner_dropdown_item);
                        spinPlayer.setAdapter(adapter);// Apply the adapter to the spinner
                        spinPlayer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                activeAction.targetPlayer = (String) ((TextView)view).getText();
                                System.out.println("Resource to give");
                            }
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                        });
                        vg.addView(spinPlayer);

                        Spinner spin = new Spinner(getBaseContext());
                        SetSpinnerArray(spin, R.array.historySetSizes);
                        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                activeAction.resourceToGive = (String) ((TextView)view).getText();
                                System.out.println("Resource to give");
                            }
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                        });
                        vg.addView(spin);
                        EditText et = new EditText(getBaseContext());
                        et.setInputType(InputType.TYPE_CLASS_NUMBER);
                        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                activeAction.quantity = Float.parseFloat((String) v.getText());
                                return false;
                            }
                        });
                        vg.addView(et);
                        break;
                }
                Button okButton = new Button(getBaseContext());
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Verify arguments first?
                        App.GetPlayer().cd.queuedActiveActions.add(activeAction);                            // Save it?
                        App.DoQueuedActions();                            // Do it?
                    }
                });
                vg.addView(okButton);
                return;
            }
            if (type == SELECT_DAILY_ACTION) {
                // Get action
                DAction da = DAction.GetFromString(text);
                if (da.requiredArguments.size() >= 1)
                {
                    // Open window to confirm addition of it.
                    SelectDetailsDialogFragment sdf = new SelectDetailsDialogFragment();
                    sdf.da = da;
                    FragmentManager fragMan = getSupportFragmentManager();
                    sdf.show(fragMan, "selectDactionDetails");
                    return;
                }
            }
            selected.add(text);
            clicked(b.getText());
            updateQueue(); // Update queue gui.
        }
    };
    private final View.OnClickListener itemClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button b = (Button)v;
            clicked(b.getText());
        }
    };

    private void clicked(CharSequence text) {
        // Find it.
        System.out.println("Clicked: "+text);
        String s = text.toString().split(":")[0]; // First stuff before any eventual arguments.
        DAction action = DAction.GetFromString(s);
        if (action != null) {
            dActionClicked(action, text.toString());
        }
        Skill skill = Skill.GetFromString(s);
        if (skill != null){
            skillClicked(skill, text.toString());
        }
    }

    private void skillClicked(Skill skill, String header) {
        TextView tvName = (TextView) findViewById(R.id.textViewItemName);
        tvName.setText(header); // Show arguments here as well.
        TextView desc = (TextView) findViewById(R.id.textViewDescription);
        desc.setText(skill.briefDescription);
    }

    // For displaying default title?
    public void dActionClicked(DAction action)
    {
        dActionClicked(action, action.text);
    }
    public void dActionClicked(DAction da, String header)
    {
        TextView tvName = (TextView) findViewById(R.id.textViewItemName);
        tvName.setText(header); // Show arguments here as well.
        TextView desc = (TextView) findViewById(R.id.textViewDescription);
        desc.setText(da.description);
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
                    player.cd.dailyActions.add(selected.get(i));                    // Just save it as Strings?
            }
            else if (type == SELECT_SKILL) {
                player.cd.skillTrainingQueue.clear();
                for (int i = 0; i < selected.size(); ++i)
                    player.cd.skillTrainingQueue.add(selected.get(i));
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
    int type = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        type = getIntent().getIntExtra("Type", 0);

        String itemsHeaderName = "Possible items";
        String header = "heeeee";
        List<String> itemNames = new ArrayList<String>();
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
                selected.add(p.cd.dailyActions.get(i));
        }
        else if (type == SELECT_SKILL)
        {
            for (int i = 0; i < p.cd.skillTrainingQueue.size(); ++i)
                selected.add(p.cd.skillTrainingQueue.get(i));
        }
        // Update the queue
        updateQueue();

        findViewById(R.id.buttonConfirm).setOnClickListener(confirm);
        findViewById(R.id.buttonClear).setOnClickListener(clear);
        findViewById(R.id.buttonCancel).setOnClickListener(cancel);
    }
}
