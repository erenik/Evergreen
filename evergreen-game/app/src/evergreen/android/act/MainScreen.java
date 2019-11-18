package evergreen.android.act;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.Serializable;

import evergreen.GameID;
import evergreen.android.BackgroundUpdateService;
import evergreen.common.Player;
import evergreen.android.App;
import evergreen.Simulator;
import evergreen.common.player.*;
import evergreen.R;
import evergreen.util.EList;
import evergreen.util.Printer;
import weka.transport.SensingFrame;
import weka.transport.TransportDetectionService;
import transport.TransportOccurrence;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainScreen extends EvergreenActivity //AppCompatActivity
{
    Simulator simulator = Simulator.getSingleton();
    private boolean checkOnUpdatesOnResume = false; // Add a config for this in some options-screen?

    @Override
    protected void onResume() {
        super.onResume();
        UpdateUI(); // Always update GUI upon resuming.
        if (checkOnUpdatesOnResume)
            SaveChangesAndCheckForUpdates(); // Check for updates?
        // Pause service to check for updates.
        Intent serviceIntent = new Intent(getBaseContext(), BackgroundUpdateService.class);
        serviceIntent.putExtra(BackgroundUpdateService.REQUEST_TYPE, BackgroundUpdateService.STOP_SERVICE);
        getBaseContext().startService(serviceIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Start service to check for updates.
        Intent serviceIntent = new Intent(getBaseContext(), BackgroundUpdateService.class);
        serviceIntent.putExtra(BackgroundUpdateService.REQUEST_TYPE, BackgroundUpdateService.START_SERVICE);
        getBaseContext().startService(serviceIntent);
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
            switch(view.getId()) {
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
            Printer.out("Next day! GameID: "+player.gameID);
            // Local game? less to think about.
            if (player.gameID == GameID.LocalGame) {
                Printer.out("Requesting player: "+player.name);
                int playersSimulated = simulator.RequestNextDay(player, true);
                focusLastLogMessageUponUpdate = false;
                if (playersSimulated <= 0) { // If not a new day, no need to update GUI, etc.
                    Toast("Simulator encountering some issues.. D:");
                    return;
                }
                // Save progress locally?
                App.SaveLocally();
                UpdateUI();
                /// Go to the results-screen in Single-player as well!
                OnLogMessagesUpdated();
            }
            else { // Save to server?
                SaveChangesAndCheckForUpdates();
            }
        }
    };

    private void PresentSystemMessageIfNewOne() {
        Player player = App.GetPlayer();
        final String sysmsg = player.sysmsg;
        if (sysmsg == null)
            return;
        if (sysmsg.equals(LastDisplayedSystemMessage())){
            return;
        }

        if (sysmsg.length() < 3)
            return; // Nothing to present really.
        // New one? present it.
        TextView tv = (TextView)findViewById(R.id.textView_sysMsg);
        tv.setText(sysmsg);
        if (sysmsg.contains("http")){
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int urlStartIndex = sysmsg.indexOf("http");
                    String url = sysmsg.substring(urlStartIndex);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });
        }
        findViewById(R.id.layout_sysmsg).setVisibility(View.VISIBLE);
        UpdateBackgroundView(findViewById(R.id.layout_sysmsg));
        findViewById(R.id.button_sysmsgConfirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetLastDisplayedSystemMessage(sysmsg);
                findViewById(R.id.layout_sysmsg).setVisibility(View.INVISIBLE);
            }
        });
//            SetLastDisplayedSysMsg(sysmsg);
    }

    private final String SYS_MSG_KEY = "EG_SYS_MSG";
    private String LastDisplayedSystemMessage() {
        SharedPreferences sp = App.GetPreferences();
        Printer.out("Previous message: "+sp.getString(SYS_MSG_KEY, ""));
        return sp.getString(SYS_MSG_KEY, "");
    }
    private void SetLastDisplayedSystemMessage(String msg){
        SharedPreferences sp = App.GetPreferences();
        sp.edit().putString(SYS_MSG_KEY, msg).apply(); // Set new message and save.
    }

    /// Main init function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        App.mainScreenActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        Player player = App.GetPlayer();
        if (App.GetPlayer() == null){ // Auto-load from local save if player is still null when entering the main screen.
            boolean ok = Load();
            // And if it fails, abort and show a message about it?
            Toast("Unable to auto-load player when entering MainScreen. Aborting");
            finish();
            return;
        }

        UpdateUI();
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.buttonChooseActiveAction).setOnClickListener(selectActionSkill);
        findViewById(R.id.buttonChooseAction).setOnClickListener(selectActionSkill);
        findViewById(R.id.buttonChooseSkill).setOnClickListener(selectActionSkill);
        findViewById(R.id.inventory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), InventoryScreen.class);
                intent.putExtra(InventoryScreen.DisplayFilter, InventoryScreen.DisplayAll);
                startActivity(intent);
            }
        });
        Button buttonNextDay = (Button) findViewById(R.id.nextDay);
        buttonNextDay.setOnClickListener(nextDay);
        buttonNextDay.setText(App.IsLocalGame()? "Next day" : "Save/Update");
        findViewById(R.id.buttonMenu).setOnClickListener(openMenu);
        /// Assign listeners for the icons.
        int[] ids = new int[]{R.id.buttonIconAttack, R.id.buttonIconDefense, R.id.buttonIconEmissions, R.id.buttonIconFood, R.id.buttonIconMaterials, R.id.buttonIconHP};
        for (int i = 0; i < ids.length; ++i)
            findViewById(ids[i]).setOnClickListener(viewStatDetails);

        // Check for updates if returning to this screen?
     //   SaveChangesAndCheckForUpdates();
    }
    /// Add check so we don't double check when state changes happen and at least 2 functions call this at the same time..?
    static long lastCheckMs = 0;
    /// Saves changes and checks for updates in one go - 1 packet with SavePlayer, after which it may query for log messages if the server notifies that new log-messages are available.
    void SaveChangesAndCheckForUpdates(){
        long now = System.currentTimeMillis();
        if (now < lastCheckMs + 3000) { // OK.
            Printer.out("Skipping check since we already checked 3 second ago.");
            return;
        }
        lastCheckMs = now;
        if (App.IsLocalGame()){ // Then don't
            Printer.out("Local game. Skipping checking updates.");
            return;
        }
        if (System.currentTimeMillis() < App.GetPlayer().lastSaveTimeSystemMs + 15000) {
            Printer.out("CheckForUpdates - skipping since we checked it like 5 seconds ago, yo.");
            return;
        }
        final long secondsPerDay = 60 * 60 * 24;
        Printer.out("Requesting TransportDetectionService for data seconds: "+secondsPerDay);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_ATTACH_DATA); // Create the broadcast receiver that will intercept the data sent by the service.
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //              Printer.out("BroadcastReceiver onReceive: "+intent);
                int request = intent.getIntExtra(TransportDetectionService.REQUEST_TYPE, -1);
                Serializable data = intent.getSerializableExtra(TransportDetectionService.SERIALIZABLE_DATA);
                //                Printer.out("Req/Res: "+request+" data: "+data);
                switch (request){
                    default:
                    case -1: break;
                    case TransportDetectionService.GET_TOTAL_STATS_FOR_DATA_SECONDS:
                        long dataSeconds = intent.getLongExtra(TransportDetectionService.DATA_SECONDS, 0);
                        if (dataSeconds == secondsPerDay) {
                            EList<TransportOccurrence> transportOccurences = (EList<TransportOccurrence>) data;
                            Player player = App.GetPlayer();
                            player.UpdateTransportMinutes(transportOccurences);
                            Printer.out("Got transport data from service, now preparing to save to server");// Connect to server.
                            player.PrintTopTransports(3);
                            RequestClientData();
                        }
                        else {
                            Printer.out("Service replied data-seconds not requested: "+dataSeconds);
                        }
                        break;
                }
                // Unregister ourselves after each save, or it will become a big fffff mess.
                getBaseContext().unregisterReceiver(this);
            }
        };
        getBaseContext().registerReceiver(broadcastReceiver, intentFilter);

        // Request last x sensing frames.
        Intent intent = new Intent(getBaseContext(), TransportDetectionService.class);
        intent.putExtra(TransportDetectionService.REQUEST_TYPE, TransportDetectionService.GET_TOTAL_STATS_FOR_DATA_SECONDS);
        intent.putExtra(TransportDetectionService.DATA_SECONDS, secondsPerDay); // 60 seconds, 60 minutes, 24 hours
        startService(intent);
    }

/*    private void OnPlayerUpdated() {
        UpdateGUI();
    }
*/
    private View.OnClickListener viewStatDetails = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int statType = -1;
            switch(view.getId()) {
                case R.id.buttonIconAttack: statType = Stat.BASE_ATTACK.ordinal(); break;
                case R.id.buttonIconDefense: statType = Stat.BASE_DEFENSE.ordinal(); break;
                case R.id.buttonIconEmissions: statType = Stat.AccumulatedEmissions.ordinal(); break;
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
    public void UpdateUI() {
        Player player = App.GetPlayer();
        if (player == null){
            Toast("Trying to update gui with null player D:");
            return;
        }
        if (!player.IsAliveOutsideCombat()) {
            App.GameOver();
            finish(); // Finish this activity.
            return;
        }

        ((ImageView) findViewById(R.id.imageView_avatar)).setImageResource(App.GetDrawableForAvatarID((int) player.Get(Config.Avatar)));
        SetText(R.id.textViewName, player.name);
        SetText(R.id.textViewHP, player.GetInt(Stat.HP)+"/"+player.MaxHP());
        SetText(R.id.textViewFood, player.GetInt(Stat.FOOD)+"");
        SetText(R.id.textViewMaterials, player.GetInt(Stat.MATERIALS)+"");
        SetText(R.id.textViewAttack, (int) player.AggregateAttackBonus()+"");
        SetText(R.id.textViewDefense, (int) player.AggregateDefenseBonus()+"");
        SetText(R.id.textViewEmissions, player.GetInt(Stat.AccumulatedEmissions) + "");
        UpdateActiveActionButton();
        UpdateDailyActionButton();
        UpdateSkillButton();
        UpdateShelterRepresentation();
        // Set max log messages?
        maxLogLinesInEventLog = 10;
        UpdateLog();
        UpdateBackground();

        PresentSystemMessageIfNewOne(); // Sys msg overlay?
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
        Printer.out("onActivityResult called, req: "+requestCode+" code: " + resultCode);
        if (resultCode < 0)
            return;
        switch(type)
        {
            case SelectActivity.SELECT_ACTIVE_ACTION:
                UpdateActiveActionButton();
                break;
            case SelectActivity.SELECT_DAILY_ACTION:
                UpdateDailyActionButton();
                break;
            case SelectActivity.SELECT_SKILL:
                UpdateSkillButton();
                break;
        }
        SaveLocally();
    }

    /// Upates the text of the active action button based on what is currently being done/selected by the player.
    private void UpdateActiveActionButton() {
        Player player = App.GetPlayer();
        int idBtn = R.id.buttonChooseActiveAction;
        TextView tv = (TextView) findViewById(idBtn);
        if (player.cd.queuedActiveActions.size() > 0)
            tv.setText(getString(R.string.ActiveAction)+" (" +player.cd.queuedActiveActions.size()+")");
        else
            tv.setText(getString(R.string.ActiveAction));
    }
    /// Upates the text of the daily actions button based on what is currently being done/selected by the player.
    private void UpdateDailyActionButton() {
        Player player = App.GetPlayer();
        TextView tv = ((TextView) findViewById(R.id.buttonChooseAction));
        if (player.cd.dailyActions.size() > 0)
            tv.setText(getString(R.string.DailyActions)+" ("+player.cd.dailyActions.size()+")");
        else
            tv.setText(getString(R.string.DailyActions));
    }
    /// Upates the text of the skill traning button based on what is currently being done/selected by the player.
    private void UpdateSkillButton() {
        Player player = App.GetPlayer();
        TextView tv = ((TextView) findViewById(R.id.buttonChooseSkill));
        if (player.cd.skillTrainingQueue.size() > 0)
            tv.setText(getString(R.string.SkillTraining)+" ("+player.cd.skillTrainingQueue.size()+")");
        else
            tv.setText(getString(R.string.SkillTraining));
    }

}
