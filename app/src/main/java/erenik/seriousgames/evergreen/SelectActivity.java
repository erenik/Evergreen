package erenik.seriousgames.evergreen;

import android.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;


public class SelectActivity extends AppCompatActivity
{
    static final int SELECT_ACTIVE_ACTION = 2;
    static final int SELECT_DAILY_ACTION = 0;
    static final int SELECT_SKILL = 1;

    List<String> selected = new ArrayList<String>();
    private final View.OnClickListener itemClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button b = (Button)v;
            selected.add(b.getText().toString());
            updateQueue(); // Update queue gui.
        }
    };
    private final View.OnClickListener clear = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            selected.clear();
            updateQueue();
        }
    };
    private final View.OnClickListener removeItemFromQueue = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            ViewGroup parent = (ViewGroup) v.getParent();
            // Get index.
            int index = parent.indexOfChild(v);
            selected.remove(index);
            parent.removeView(v);
            updateQueue(); // Update queue gui.
        }
    };
    int queueColor = 0xAAAAAAAA;
    // Updates queue gui.
    private void updateQueue()
    {
        // Yeah.
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutQueue);
        vg.removeAllViews();
        for (int i = 0; i < selected.size(); ++i)
        {
            // Make a button out of it.
            Button b = new Button(getBaseContext());
            b.setText(selected.get(i));
            b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); // Yeah, yeah.
            b.setBackgroundColor(queueColor);
            vg.addView(b);
            b.setOnClickListener(removeItemFromQueue);
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
                    DAction da = DAction.GetFromString(selected.get(i));
                    if (da != null)
                        player.dailyActions.add(da);
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
                itemNames = Skills.Names();
                break;
        }

        /// Populate the available items.
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutItems);
        for (int i = 0; i < names.size(); ++i)
        {
            // Make a button out of it.
            Button b = new Button(getBaseContext());
            b.setText(names.get(i));
            b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); // Yeah, yeah.
            vg.addView(b);
            b.setOnClickListener(itemClicked);
        }
        // Load from player.
        Player p = Player.getSingleton();
        List<DAction> lda = p.dailyActions;
        for (int i = 0; i < lda.size(); ++i)
        {
            DAction da = lda.get(i);
            if (da == null)
                continue;
            System.out.println("da: "+da);
            selected.add(da.text);
        }
        // Update the queue
        updateQueue();

        findViewById(R.id.buttonConfirm).setOnClickListener(confirm);
        findViewById(R.id.buttonClear).setOnClickListener(clear);
        findViewById(R.id.buttonCancel).setOnClickListener(cancel);

    }
}
