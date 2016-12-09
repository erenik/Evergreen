package erenik.seriousgames.evergreen.act;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import erenik.seriousgames.evergreen.App;
import erenik.seriousgames.evergreen.Simulator;
import erenik.seriousgames.evergreen.logging.*;
import erenik.seriousgames.evergreen.player.*;
import erenik.seriousgames.evergreen.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainScreen extends EvergreenActivity //AppCompatActivity
{
    Player player = App.GetPlayer();
    Simulator simulator = Simulator.getSingleton();

    @Override
    protected void onResume() {
        super.onResume();
        UpdateGUI(); // Always update GUI upon resuming.
        App.HandleNextEvent();
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnClickListener selectActionSkill = new View.OnClickListener()
    {   @Override
        public void onClick(View view)
        {
            if (App.HandleNextEvent())
                return;
            int selection = -1;
            switch(view.getId())
            {
                case R.id.buttonChooseSkill: selection = SelectActivity.SELECT_SKILL; break;
                case R.id.buttonChooseAction: selection = SelectActivity.SELECT_DAILY_ACTION; break;
                case R.id.buttonChooseActiveAction: selection = SelectActivity.SELECT_ACTIVE_ACTION; break;
            }
            Intent i = new Intent(getBaseContext(), SelectActivity.class);
            i.putExtra("Type", selection);
            startActivityForResult(i, selection);
        }
    };
    View.OnClickListener nextDay = new View.OnClickListener()
    {   @Override
        public void onClick(View v) {
            if (App.HandleNextEvent())
                return;
            simulator.NextDay();
            // Update UI? lol
            UpdateGUI();
             /// Queues next event to be handled, if there are any.
            App.HandleNextEvent();
        }
    };
    /// For the log...? Or open in a new activity? Since it's just readng?
    boolean fullScreen;
    View.OnClickListener toggleLogFullScreen = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            Intent i = new Intent(getBaseContext(), logViewer.class);
            startActivity(i);
            System.out.println("Toggle fullscreen");
        }
    };

    /// Main init function
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        App.mainScreenActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        // Load stuff as needed.
        App.LoadLocally(getBaseContext());
//        player.SaveLocally(); // Save copy? - Why?
        /// Update GUI.
        UpdateGUI();

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.buttonChooseActiveAction).setOnClickListener(selectActionSkill);
        findViewById(R.id.buttonChooseAction).setOnClickListener(selectActionSkill);
        findViewById(R.id.buttonChooseSkill).setOnClickListener(selectActionSkill);
        findViewById(R.id.nextDay).setOnClickListener(nextDay);
        findViewById(R.id.scrollViewLog).setOnClickListener(toggleLogFullScreen);
        findViewById(R.id.layoutLog).setOnClickListener(toggleLogFullScreen);

        /// Assign listeners for the icons.
        int[] ids = new int[]{R.id.buttonIconAttack, R.id.buttonIconDefense, R.id.buttonIconEmissions, R.id.buttonIconFood, R.id.buttonIconMaterials, R.id.buttonIconHP};
        for (int i = 0; i < ids.length; ++i)
            findViewById(ids[i]).setOnClickListener(viewStatDetails);
    }

    private View.OnClickListener viewStatDetails = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int statType = -1;
            switch(view.getId())
            {
                case R.id.buttonIconAttack: statType = Stat.ATTACK_BONUS.ordinal(); break;
                case R.id.buttonIconDefense: statType = Stat.DEFENSE_BONUS.ordinal(); break;
                case R.id.buttonIconEmissions: statType = Stat.EMISSIONS.ordinal(); break;
                case R.id.buttonIconFood: statType = Stat.FOOD.ordinal(); break;
                case R.id.buttonIconMaterials: statType = Stat.MATERIALS.ordinal(); break;
                case R.id.buttonIconHP: statType = Stat.HP.ordinal(); break;
            }
            Intent i = new Intent(getBaseContext(), StatViewActivity.class);
            i.putExtra("Stat", statType);
            startActivityForResult(i, statType);
        }
    };


    void UpdateGUI()
    {
        SetText(R.id.textViewHP, player.GetInt(Stat.HP)+"/"+player.MaxHP());
        SetText(R.id.textViewFood, player.GetInt(Stat.FOOD)+"");
        SetText(R.id.textViewMaterials, player.GetInt(Stat.MATERIALS)+"");
        SetText(R.id.textViewAttack, player.BaseAttack()+"");
        SetText(R.id.textViewDefense, player.BaseDefense()+"");
        SetText(R.id.textViewEmissions, player.GetInt(Stat.EMISSIONS) + "");

        UpdateActiveActionButton();
        UpdateDailyActionButton();
        UpdateSkillButton();
        // Update log.
        App.UpdateLog((ViewGroup) findViewById(R.id.layoutLog), getBaseContext(), 50, player.logTypesToShow);
    }

    void SetText(int viewID, String text)
    {
        ((TextView) findViewById(viewID)).setText(text);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        int type = requestCode;
        System.out.println("onActivityResult called, req: "+requestCode+" code: " + resultCode);
        if (resultCode < 0)
            return;
        switch(type)
        {
            case SelectActivity.SELECT_ACTIVE_ACTION:
                player.activeAction = resultCode;
                UpdateActiveActionButton();
                break;
            case SelectActivity.SELECT_DAILY_ACTION:
                UpdateDailyActionButton();
                break;
            case SelectActivity.SELECT_SKILL:
                UpdateSkillButton();
                break;
        }
        App.SaveLocally(getBaseContext()); // Save copy?
    }
    private void UpdateActiveActionButton() {
        int idBtn = R.id.buttonChooseActiveAction;
        TextView tv = (TextView) findViewById(idBtn);
        if (player.activeAction >= 0)
            tv.setText("Change Active Action: " + getResources().getStringArray(R.array.activeActions)[player.activeAction]);
        else
            tv.setText("Active action");
    }
    private void UpdateDailyActionButton()
    {
        TextView tv = ((TextView) findViewById(R.id.buttonChooseAction));
        if (player.dailyActions.size() >= 0)
            tv.setText("Change Action: "+player.dailyActions.size()+" queued.");
        else
            tv.setText(R.string.chooseAction);
    }
    private void UpdateSkillButton()
    {
        TextView tv = ((TextView) findViewById(R.id.buttonChooseSkill));
        if (player.skillTrainingQueue.size() >= 0)
            tv.setText("Change Skill: "+player.skillTrainingQueue.size()+" queued.");
        else
            tv.setText(R.string.chooseSkill);
    }


}
