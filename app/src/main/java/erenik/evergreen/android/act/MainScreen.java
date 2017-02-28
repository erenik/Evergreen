package erenik.evergreen.android.act;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import erenik.evergreen.common.Player;
import erenik.evergreen.android.App;
import erenik.evergreen.Simulator;
import erenik.evergreen.common.player.*;
import erenik.evergreen.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainScreen extends EvergreenActivity //AppCompatActivity
{
    Simulator simulator = Simulator.getSingleton();

    @Override
    protected void onResume() {
        super.onResume();
        UpdateGUI(); // Always update GUI upon resuming.
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
            Player player = App.GetPlayer();
            player.lastEditSystemMs = System.currentTimeMillis(); // Update so auto-loaded works for the most recently-played player-character. :)
            System.out.println("Next day!");
            // Local game? less to think about.
            if (App.isLocalGame) {
                System.out.println("Requesting player: "+player.name);
                int playersSimulated = simulator.RequestNextDay(player);
                focusLastLogMessageUponUpdate = false;
                if (playersSimulated <= 0) { // If not a new day, no need to update GUI, etc.
                    Toast("Simulator encountering some issues.. D:");
                    return;
                }
                // Save progress locally?
                App.SaveLocally();
            }
            if (!player.IsAlive())
            {
                App.GameOver();
                finish(); // Finish this activity.
                return;
            }
            // Update UI? lol
            focusLastLogMessageUponUpdate = true;
            UpdateGUI();
            focusLastLogMessageUponUpdate = false;
        }
    };

    /// Main init function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        App.mainScreenActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        Load();

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
        findViewById(R.id.buttonMenu).setOnClickListener(openMenu);

        /// Assign listeners for the icons.
        int[] ids = new int[]{R.id.buttonIconAttack, R.id.buttonIconDefense, R.id.buttonIconEmissions, R.id.buttonIconFood, R.id.buttonIconMaterials, R.id.buttonIconHP};
        for (int i = 0; i < ids.length; ++i)
            findViewById(ids[i]).setOnClickListener(viewStatDetails);
    }

    private View.OnClickListener viewStatDetails = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int statType = -1;
            switch(view.getId()) {
                case R.id.buttonIconAttack: statType = Stat.BASE_ATTACK.ordinal(); break;
                case R.id.buttonIconDefense: statType = Stat.BASE_DEFENSE.ordinal(); break;
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

    /// Updates all GUI, stats, logs, etc. Called after any change / returning to this activity from another one, pretty much.
    void UpdateGUI() {
        Player player = App.GetPlayer();
        if (player == null){
            Toast("Trying to update gui with null player D:");
            return;
        }
        ((ImageView) findViewById(R.id.imageView_avatar)).setImageResource(App.GetDrawableForAvatarID((int) player.Get(Stat.Avatar)));
        SetText(R.id.textViewName, player.name);
        SetText(R.id.textViewHP, player.GetInt(Stat.HP)+"/"+player.MaxHP());
        SetText(R.id.textViewFood, player.GetInt(Stat.FOOD)+"");
        SetText(R.id.textViewMaterials, player.GetInt(Stat.MATERIALS)+"");
        SetText(R.id.textViewAttack, (int) player.AggregateAttackBonus()+"");
        SetText(R.id.textViewDefense, (int) player.AggregateDefenseBonus()+"");
        SetText(R.id.textViewEmissions, player.GetInt(Stat.EMISSIONS) + "");
        UpdateActiveActionButton();
        UpdateDailyActionButton();
        UpdateSkillButton();
        UpdateShelterRepresentation();
        UpdateLog();
    }

    private void UpdateShelterRepresentation() {
        Player player = App.GetPlayer();
        int resourceID = 0;
        switch(player.GetInt(Stat.SHELTER_DEFENSE)) {
            case 1: resourceID = R.drawable.def_level_1; break;
            case 2: resourceID = R.drawable.def_level_2; break;
            case 3: resourceID = R.drawable.def_level_3; break;
            case 4: resourceID = R.drawable.def_level_4; break;
            case 5: resourceID = R.drawable.def_level_5; break;
            default: return;
        }
        ImageView iv = (ImageView) findViewById(R.id.imageView_defenseLevel);
        iv.setImageResource(resourceID);
    }

    void SetText(int viewID, String text)
    {
        ((TextView) findViewById(viewID)).setText(text);
    }

    /// After everything has been created - i.e. GUI updated, perhaps logic should actually be placed here? Like saving, connecting to server, etc.?
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /// Checks any result from activities spawned from this one, and updates action if they were confirmed (e.g. hitting back should cancel/nullify any potential change).
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Player player = App.GetPlayer();
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
        Save();
    }

    /// Upates the text of the active action button based on what is currently being done/selected by the player.
    private void UpdateActiveActionButton() {
        Player player = App.GetPlayer();
        int idBtn = R.id.buttonChooseActiveAction;
        TextView tv = (TextView) findViewById(idBtn);
        if (player.activeAction > 0)
            tv.setText(getString(R.string.ActiveAction)+" (" +player.activeAction+")");
        else
            tv.setText(getString(R.string.ActiveAction));
    }
    /// Upates the text of the daily actions button based on what is currently being done/selected by the player.
    private void UpdateDailyActionButton() {
        Player player = App.GetPlayer();
        TextView tv = ((TextView) findViewById(R.id.buttonChooseAction));
        if (player.dailyActions.size() > 0)
            tv.setText(getString(R.string.DailyActions)+" ("+player.dailyActions.size()+")");
        else
            tv.setText(getString(R.string.DailyActions));
    }
    /// Upates the text of the skill traning button based on what is currently being done/selected by the player.
    private void UpdateSkillButton() {
        Player player = App.GetPlayer();
        TextView tv = ((TextView) findViewById(R.id.buttonChooseSkill));
        if (player.skillTrainingQueue.size() > 0)
            tv.setText(getString(R.string.SkillTraining)+" ("+player.skillTrainingQueue.size()+")");
        else
            tv.setText(getString(R.string.SkillTraining));
    }

}
