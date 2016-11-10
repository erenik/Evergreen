package erenik.seriousgames.evergreen.act;

import android.graphics.Point;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import erenik.seriousgames.evergreen.App;
import erenik.seriousgames.evergreen.player.*;
import erenik.seriousgames.evergreen.EvergreenButton;
import erenik.seriousgames.evergreen.R;


public class SelectActivity extends FragmentActivity
{
    static final int SELECT_ACTIVE_ACTION = 2;
    static final int SELECT_DAILY_ACTION = 0;
    static final int SELECT_SKILL = 1;

    List<String> selected = new ArrayList<String>();
    List<Skill> selectedSkills = new ArrayList<Skill>();
    List<DAction> selectedDActions = new ArrayList<DAction>();
    int buttonBgId = R.drawable.small_button;

    private final View.OnClickListener addItem = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button b = (Button)v;
            String text = b.getText().toString();
            if (type == SELECT_DAILY_ACTION)
            {
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
        String s = text.toString();
        DAction action = DAction.GetFromString(s);
        if (action != null)
        {
            dActionClicked(action);
        }
    }

    public void dActionClicked(DAction da)
    {
        TextView tvName = (TextView) findViewById(R.id.textViewItemName);
        tvName.setText(da.text);
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
            Button b = new Button(getBaseContext());
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
            Player player = Player.getSingleton();
            // Check stuff in list at present. Catenate. Return result? Save into player?
            if (type == SELECT_DAILY_ACTION)
            {
                player.dailyActions.clear();
                for (int i = 0; i < selected.size(); ++i)
                {
                    // Just save it as Strings?
                    player.dailyActions.add(selected.get(i));
                }
            }
            else if (type == SELECT_SKILL)
            {
                player.skillTrainingQueue.clear();
                for (int i = 0; i < selected.size(); ++i)
                {
                    player.skillTrainingQueue.add(selected.get(i));
                }
            }
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        type = getIntent().getIntExtra("Type", 0);

        int arrayId = -1;
        String itemsHeaderName = "Possible items";
        List<String> itemNames = new ArrayList<String>();
        switch(type)
        {
            case SELECT_DAILY_ACTION:
                arrayId = R.array.dailyActions;
                itemsHeaderName = "Daily actions";
                itemNames = DAction.Names();
                break;
            case SELECT_ACTIVE_ACTION: arrayId = R.array.activeActions; break;
            case SELECT_SKILL:
                arrayId = R.array.skills;
                itemsHeaderName = "Trainable Skills";
                itemNames = Skill.Names();
                break;
        }
        TextView tv = (TextView) findViewById(R.id.textViewPossibleItems);
        tv.setText(itemsHeaderName);

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
        Player p = Player.getSingleton();
        if (type == SELECT_DAILY_ACTION) {
            for (int i = 0; i < p.dailyActions.size(); ++i)
                selected.add(p.dailyActions.get(i));
        }
        else if (type == SELECT_SKILL)
        {
            for (int i = 0; i < p.skillTrainingQueue.size(); ++i)
                selected.add(p.skillTrainingQueue.get(i));
        }
        // Update the queue
        updateQueue();

        findViewById(R.id.buttonConfirm).setOnClickListener(confirm);
        findViewById(R.id.buttonClear).setOnClickListener(clear);
        findViewById(R.id.buttonCancel).setOnClickListener(cancel);
    }
}
