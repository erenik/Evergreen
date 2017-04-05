package erenik.evergreen.android.act;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;

import erenik.evergreen.Game;
import erenik.evergreen.android.ui.EvergreenButton;
import erenik.evergreen.android.ui.EvergreenTextView;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Invention.InventionStat;
import erenik.evergreen.common.Invention.InventionType;
import erenik.evergreen.common.Invention.Weapon;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketError;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequestType;
import erenik.evergreen.common.packet.EGRequest;
import erenik.evergreen.common.packet.EGResponse;
import erenik.evergreen.common.packet.EGResponseType;
import erenik.evergreen.common.packet.OnPacketReplyListener;
import erenik.evergreen.common.player.ClientData;
import erenik.evergreen.common.player.Config;
import erenik.evergreen.common.player.Stat;
import erenik.util.Byter;
import erenik.util.EList;
import erenik.weka.transport.TransportDetectionService;


/**
 * Created by Emil on 2016-12-09.
 */
public class EvergreenActivity extends AppCompatActivity
{
    View scrollViewLog = null, layoutLog = null;
    protected int maxLogLinesInEventLog = 50; // Default, change if the activity should show more.
    boolean focusLastLogMessageUponUpdate = false;

    /// Displays a toast-style message briefly (i.e. 3 second notification in the bottom of the screen).
    protected void NotImplemented() {
        Toast("Not implemented yet");
    }
    protected void ToastLong(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
    public void Toast(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
    public void ToastUp(String text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
    void GoToMainScreen() {
        System.out.println("Starting new activity");
        Intent i = new Intent(getBaseContext(), MainScreen.class);
        startActivity(i);
    }
    protected void SetSpinnerArray(Spinner spinner, int arrayID) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, arrayID, android.R.layout.simple_spinner_item);// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.evergreen_spinner_dropdown_item);
        spinner.setAdapter(adapter);// Apply the adapter to the spinner
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set default orientation - Portrait.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Request full-screen.
//        setSystemUiVisibility(SYSTEM_UI_FLAG_FULLSCREEN);
        // Store common shit here?
        App.OnActivityCreated(this);  // Setup system callbacks as/if needed.


    }
    /* After creation, Cause hideControls of system controls after 500 ms? */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Set up full-screen flags now that everything should be created (assuming sub-class loaded the views properly.
        setupFullscreenFlags();

        /// Find views after they have been loaded earlier in the sub-class' onCreate method.
        scrollViewLog = findViewById(R.id.scrollViewLog);
        layoutLog = findViewById(R.id.layoutLog);
        if (scrollViewLog != null) {
            System.out.println("Event log found.");
            scrollViewLog.setOnClickListener(toggleLogFullScreen);
            scrollViewLog.setClickable(true);
        }
        if (layoutLog != null) {
            System.out.println("Layout for Event log found.");
            layoutLog.setOnClickListener(toggleLogFullScreen);
        }
        UpdateLog(); // Update log after subclass has adjusted how it should be presented.
        UpdateBackground();

        // Launch the transport-sensing service if not already running.
        Intent serviceIntent = new Intent(getBaseContext(), TransportDetectionService.class);
        serviceIntent.putExtra(TransportDetectionService.REQUEST_TYPE, TransportDetectionService.START_SERVICE);
        getBaseContext().startService(serviceIntent);
    }

    protected void UpdateBackground() {
        // Update background as needed?
        View bg = findViewById(R.id.fullscreen_view_background);
        if (bg != null) {
            UpdateBackgroundView(bg);
        }
    }

    protected void UpdateBackgroundView(View bgv){
        int id = 0;
        int stage = 0;
        if (App.GetPlayer() != null)
            stage = (int) App.GetPlayer().Get(Stat.EMISSIONS) / 25; // Divide by 20 for stage?
        if (bgv != null){
            switch (stage){
                case 0: id = R.drawable.bg0; break;
                case 1: id = R.drawable.bg1; break;
                case 2: id = R.drawable.bg2; break;
                case 3: id = R.drawable.bg3; break;
                case 4: id = R.drawable.bg4; break;
                case 5: id = R.drawable.bg5; break;
                case 6:
                default:
                    id = R.drawable.bg6;
            }
        }
        bgv.setBackgroundResource(id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupFullscreenFlags();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        setupFullscreenFlags();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    static public EList<Game> gameList = new EList<>();

    void GetGamesList() {
        System.out.println("Requesting games list from server...");
        EGPacket pack = EGRequest.byType(EGRequestType.GetGamesList);
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                gameList = reply.parseGamesList();
            }

            @Override
            public void OnError(EGPacketError error) {
                Toast("PacketError: "+error.name());
            }
        });
        App.Send(pack);
//        pack.SendToServer(); // Send to default server.
    }

    void setupFullscreenFlags() {
        // hideControls()?
        mVisible = true; // Default controls are visible.
        // Set up the user interaction to manually showControls or hideControls the system UI.
        mContentView = findViewById(R.id.fullscreen_view_background);
        if (mContentView != null) {
            mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggle();
                }
            });
            /// Set up full-screen flags.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
        else {
            System.out.println("Unable to find fullscreen_view, cannot set full-screen flags accordingly.");
            Toast("Unable to find fullscreen_view, cannot set full-screen flags accordingly.");
        }


        /*
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        */
        // Upon interacting with UI controls, delay any scheduled hideControls()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    protected View.OnClickListener openMenu = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            // Go to menu activity.
            Intent i = new Intent(getBaseContext(), MenuActivity.class);
            startActivity(i);
        }
    };



    /// General Save/Load. Generally saves locally only? Will determine if it needs to perform it locally or remotely.
    public boolean SaveLocally() {
        // For now, save locally and remotely, for testing purposes.
        App.SaveLocally();
//        if (App.GetPlayer().gameID != GameID.LocalGame)
  //      SaveToServer(null); // Save changes, ignoring any new updates for now.
        return true;
    }
    /// Ease of use method, just loads..?
    public boolean Load() {
        App.LoadLocally();
    //    if (App.GetPlayer().gameID != GameID.LocalGame)
      //      LoadFromServer();
        return true;
    }

    protected boolean SaveToServer() {
        ShowProgressBar();
        int secondsToAnalyze = 3600 * 24; // 24 hours?

        Intent intent = new Intent(getBaseContext(), TransportDetectionService.class);
        intent.putExtra(TransportDetectionService.REQUEST_TYPE, TransportDetectionService.GET_TOTAL_STATS_FOR_DATA_SECONDS);
        intent.putExtra(TransportDetectionService.DATA_SECONDS, secondsToAnalyze);
        startService(intent);

        IntentFilter intentFilter = new IntentFilter(); // Intent.ACTION_ATTACH_DATA
        getBaseContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("BroadcastReceiver onReceive: "+intent);
//                if (intent.getClass() instanceof TransportDetectionService)
                Serializable data = intent.getSerializableExtra(TransportDetectionService.DATA_SECONDS);
                System.out.println("Yeahhhh: "+data);
            }
        }, intentFilter);

        // TODO: Fix this.
/*
        Player player = App.GetPlayer();
        player.UpdateTransportMinutes(TransportDetectionService.GetInstance().GetTotalStatsForDataSeconds(secondsToAnalyze));
        System.out.println("Saving to server, ");// Connect to server.
        player.PrintTopTransports(3);

        // SErvice Intent send stuffs.
//        player.UpdateTransportMinutes(TransportDetectionService.GetInstance().GetTotalStatsForDataSeconds(secondsToAnalyze));

        // Wait until we receive a response to actually save?


        // Load this player?
        /// Extract transport information from the TransportDetectorService and add its summary data into player.
        EGRequest req = EGRequest.Save(player);
        req.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    HideProgressBar();
                    HandleClientData(reply);
                }
                @Override
                public void OnError(EGPacketError error) {
                    Toast("Error: "+error.name());
                    HideProgressBar();
                }
            });
        App.Send(req);//        req.WaitForResponse(1000); // No waiting allowed. Use callbacks only!
        */
        return true;
    }
    /// Performs default actions for some responses, such as removing the active player if the data is no longer valid (e.g. old player/game ID).
    protected void HandleResponse(EGResponseType egResponseType) {
        switch (egResponseType){
            case NoSuchPlayer:
                Toast("No such player exists. Removing it.");
                App.GetPlayer();
                App.RemovePlayer(App.GetPlayer());
                finish(); // Go back to title screen.
                break;
            default:
                Toast("Res: "+egResponseType.name());
                break;
        }
    }

    protected static boolean LoadFromServer(EGPacketReceiverListener responseListener) {
        // Connect to server.
        Player player = App.GetPlayer();
        System.out.println("Loading from server.");
        // Send shit.
        EGRequest req = EGRequest.Load(player);
        if (responseListener != null)
            req.addReceiverListener(responseListener);
        App.Send(req);
        return true;
    }


    void ShowProgressBar(){
        View pb = findViewById(R.id.progressBar);
        if (pb != null)
            pb.setVisibility(View.VISIBLE);
    }
    void HideProgressBar(){
        View pb = findViewById(R.id.progressBar);
        if (pb != null)
            pb.setVisibility(View.INVISIBLE);
    }



    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.

//            View anyView = App.currentActivity.
            System.out.println("mHidePart2Runnable whatevers");
            if (mContentView != null)
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
//    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
  //          mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("Hiding the stuff..");
            hideControls();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private void toggle() {
        /// Fuck the toggle.
        /*
        if (mVisible) {
            hideControls();
        } else {
            showControls();
        }*/
    }

    private void hideControls()
    {
//        System.out.println("Hiding in plain sight -o-o- content: "+mContentView);
        mContentView = findViewById(R.id.fullscreen_view_background);
        if (mContentView != null)
        {
            System.out.println("goin fullscreen yo: ");
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            mVisible = false;
            // Schedule a runnable to remove the status and navigation bar after a delay .. hm.
            mHideHandler.removeCallbacks(mShowPart2Runnable);
            mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
            // Hide UI first
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }
        else
        {
            System.out.println("Failed to fullscreen");
        }
    }

    @SuppressLint("InlinedApi")
    private void showControls() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hideControls() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis)
    {
        System.out.println("Delayed hideControls of "+delayMillis);
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    /// For log-management, if it is present somewhere in the activity (it may be).
    /// For the log...? Or open in a new activity? Since it's just readng?
    boolean logFullScreen;
    View.OnClickListener toggleLogFullScreen = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Check current activity, if this is the LogViewer, then end it just.
            if (App.currentActivity.getClass() == EventLogViewer.class) {
                finish();
                return;
            }
            Intent i = new Intent(getBaseContext(), EventLogViewer.class);
            startActivity(i);
            System.out.println("Toggle fullscreen");
        }
    };

    // Update log with relevant filters.
    void UpdateLog() {
        if (layoutLog == null) {
         //   Toast("Nooo looog!");
            return;
        }
        Player player = App.GetPlayer();
        if (player == null) {
            return;
        }
        if (player.log == null)
            return;
//        App.UpdateLog((ViewGroup) findViewById(R.id.layoutLog), getBaseContext(), maxLogLinesInEventLog, player.logTypesToShow);
        System.out.println("Log.UpdateLog");
        ViewGroup v = (ViewGroup) layoutLog;
        // Remove children.
        v.removeAllViews();
        // Add new ones?
        int numDisplay = player.log.size();
        numDisplay = numDisplay > maxLogLinesInEventLog ? maxLogLinesInEventLog : numDisplay;
        int startIndex = player.log.size() - numDisplay + 1;
        System.out.println("Start index: "+startIndex+" log size: "+player.log.size());
        View lastAdded = null;
        EList<Log> newLogMessages = player.log.subList(startIndex);
        System.out.println("Log messages to show: "+newLogMessages.size());
        Log.PrintLastLogMessages(newLogMessages, newLogMessages.size());
        PrintLastLogMessages(newLogMessages.size());
        EList<Log> filteredLogMessages = Log.ApplyFilters(newLogMessages, player.logTypesToFilter);
        if (filteredLogMessages.size() == 0) {
            System.out.println("Filters mis-behaving, reverting to display all types.");
            filteredLogMessages = newLogMessages;
        }
        for (int i = 0; i < filteredLogMessages.size(); ++i) {
            Log l = filteredLogMessages.get(i);
            View t = GetViewForLogMessage(l);
            v.addView(t); // Insert at index 0 always.
        }
        lastAdded = v.getChildAt(v.getChildCount()-1);
        if (lastAdded != null && focusLastLogMessageUponUpdate)
            lastAdded.requestFocus(); // Request focus, make visible?
    }
    boolean alternateLogMessageColor = false;

    /// Creates and returns a new TextView for the larget log-message.
    TextView GetViewForLogMessage(Log l){
        String s = l.text;
        if (l.BasicStringVersion() == false)
            s = App.GetLogText(l.TextID(), l.Args());
        TextView t = new TextView(getBaseContext());
        t.setText(s);
        int hex = ContextCompat.getColor(getBaseContext(), App.GetColorForLogType(l.type));
        // System.out.println("Colorizing: "+Integer.toHexString(hex));
        t.setTextColor(hex);
        alternateLogMessageColor = !alternateLogMessageColor;
        int colorID = alternateLogMessageColor? R.color.logColor1 : R.color.logColor2;
        int bgHex = ContextCompat.getColor(getBaseContext(), colorID);
        t.setBackgroundColor(bgHex);
//            t.setBackgroundResource(R.drawable.chatlogbg);
        t.setFocusable(true); // Focusable.
        t.setFocusableInTouchMode(true);
        return t;
    }



    protected void UpdateItemList(ViewGroup vg, InventionType type) {
        // Clear it.
        vg.removeAllViews();
        Player player = App.GetPlayer();
        int added = 0;
        for (int i = 0; i < player.cd.inventory.size(); ++i) { // EList all items of specific type.
            Invention item = player.cd.inventory.get(i);
            if (item.type != type)
                continue;
            /// First add a LinearLayout (horizontal)
            LinearLayout ll = new LinearLayout(getBaseContext());
            // Give it an ID? Skip?
            int id  = 0;
            switch(i) {
                case 0: id = R.id.queueLayout0; break; case 1: id = R.id.queueLayout1; break;case 2: id = R.id.queueLayout2; break;case 3: id = R.id.queueLayout3; break;case 4: id = R.id.queueLayout4; break;case 5: id = R.id.queueLayout5; break;case 6: id = R.id.queueLayout6; break;case 7: id = R.id.queueLayout7; break;
            }
            ll.setId(id); // ID for .. idk.
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, App.GetScreenSize().y / 10);
            layoutParams.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.listSelectionMargin));
            ll.setLayoutParams(layoutParams);
            ll.setBackgroundResource(R.drawable.small_button);
            int padding = 35;
            ll.setPadding(padding,0,padding,0);
            vg.addView(ll);

            ll.setOnClickListener(itemClicked);

            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            b.setText(item.name);
            // Screen div 10 height per element?
            layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 0.1f);
            layoutParams.gravity = Gravity.LEFT;
            b.setLayoutParams(layoutParams);
            b.setOnClickListener(itemClicked);
            b.setBackgroundColor(0x00);
            b.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.mainTextColor));
            ll.addView(b);

            // Add some image views of the stats, depending on the item?
            if (item.type == InventionType.RangedWeapon || item.type == InventionType.Weapon) {
//                AddIconAndBonus(InventionStat.AttackBonus, item, ll);
    //            AddIconAndBonus(InventionStat.AttackDamageBonus, item, ll);
  //              AddIconAndBonus(InventionStat.BonusAttacks, item, ll);
            }
            if (item.type == InventionType.Armor){
                AddIconAndBonus(InventionStat.DefenseBonus, item, ll);
            }
            ++added;
        }
        if (added == 0){
            Toast("Found no items.");
        }
    }

    // Does what?
    private void AddIconAndBonus(InventionStat stat, Invention item, LinearLayout ll) {
        // Add a button in the button to remove it.
        ImageButton statImageButton = new ImageButton(getBaseContext());
        statImageButton.setBackgroundColor(0x00); // SEe-through?
        int did = 0; // Drawable ID.
        String num = "";
        Weapon w = null;
        if (item instanceof Weapon)
            w = (Weapon) item;
        switch(stat) {
            case AttackBonus: did = R.drawable.weapon_accuracy; num = ""+item.AttackBonus(); break;
            case AttackDamageBonus:
                did = R.drawable.weapon_damage;
                num = ""+w.MinimumDamage()+"-"+w.MaximumDamage();
                break;
            case BonusAttacks: did = R.drawable.weapon_attacks; num = ""+(1+item.Get(InventionStat.BonusAttacks)); break;
        }
        statImageButton.setImageResource(did);
//        statImageButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 4.0f)); // Weight at the end?
        LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(0, 0);
//        lllp.weight = 0.5f;
        lllp.width = App.GetScreenSize().x / 8;
        lllp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lllp.gravity = Gravity.CENTER;
        statImageButton.setLayoutParams(lllp);
        statImageButton.setScaleType(ImageView.ScaleType.FIT_CENTER); // Scale to fit?
//        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 2.f);

        ll.addView(statImageButton);
        // Add text next to it.
        EvergreenTextView tv = new EvergreenTextView(getBaseContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.1f)); // Weight at the end?
        tv.setText(num);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTextSize(14);
        ll.addView(tv);
    }

    private final View.OnClickListener itemClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button b = null;
            if (v instanceof Button){
                b = (Button)v;
            }
            else { // Layout was pressed.
                ViewGroup vg = (ViewGroup) v;
                b = (Button) vg.getChildAt(0);
            }
            String itemName = (String) b.getText();
            System.out.println("Item clicked: "+itemName);
            // Equip it?
            Player player = App.GetPlayer();
            for (int i = 0; i < player.cd.inventory.size(); ++i) {
                Invention item = player.cd.inventory.get(i);
                if (item.name.equals(itemName))
                {
                    // Equip it?
                    System.out.println("Found the item.");
                    // Open screen for this item?
                    Intent intent = new Intent(getBaseContext(), ItemDetails.class);
                    intent.putExtra("ItemIndex", i);
                    startActivity(intent);

//                    player.Equip(item);
  //                  System.out.println("Equipped it.");
    //                UpdateUI();
                }
            }

           // clicked(b.getText());
        }
    };

    public void UpdateUI() {
        // Should be subclassed?
//        System.out.println("EvergreenActivity.UpdateUI Should be subclassed to take care of updates to UI from various events (network packets, changing gear, etc).");
        UpdateLog(); // Update the log, if it exists there.
    };


    // For handling Activity Tracking.
    void LoadTransportEvents(){

    }

    /*
    int numMostRecentMessagesToFetch = 0;
    protected void RequestNumLogMessages(final int numMostRecentMessagesToFetch) {
        this.numMostRecentMessagesToFetch = numMostRecentMessagesToFetch;
        EGPacket pack = EGRequest.LogLength(App.GetPlayer());
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                switch (reply.ResType()){
                    case NumLogMessages:
                        int num = (int) Byter.toObject(reply.GetBody());
                        logMessagesOnServer = num;
                        Toast("Log messages on server: "+logMessagesOnServer);
                        System.out.println("Num log msgs on server: "+num);
                        App.GetPlayer().log.clear();
                        // Request them.
                        RequestLogMessages(numMostRecentMessagesToFetch);
                }
            }
            @Override
            public void OnError(EGPacketError error) {

            }
        });
        App.Send(pack);
    }*/
/*
    int numMostRecentMessagesToFetch = 0;
    protected void RequestNumLogMessages(final int numMostRecentMessagesToFetch) {
        this.numMostRecentMessagesToFetch = numMostRecentMessagesToFetch;
        EGPacket pack = EGRequest.LogLength(App.GetPlayer());
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                switch (reply.ResType()){
                    case NumLogMessages:
                        int num = (int) Byter.toObject(reply.GetBody());
                        logMessagesOnServer = num;
                        Toast("Log messages on server: "+logMessagesOnServer);
                        System.out.println("Num log msgs on server: "+num);
                        App.GetPlayer().log.clear();
                        // Request them.
                        RequestLogMessages(numMostRecentMessagesToFetch);
                }
            }
            @Override
            public void OnError(EGPacketError error) {

            }
        });
        App.Send(pack);
    }
*/
    int totalLogMessagesReceived = 0;
    static long lastLogRequest = 0;
    ArrayList<EGPacket> replies = new ArrayList<>();
    ArrayList<EGPacket> packetsSent = new ArrayList<>();
    protected boolean RequestLogMessages(final int numMostRecentOnes, final long oldestIDtoInclude) {
        if (System.currentTimeMillis() == lastLogRequest + 3000)
            return false;
        packetsSent.clear();
        replies.clear();
        System.out.println("Requesting log messages: "+numMostRecentOnes);
        lastLogRequest = System.currentTimeMillis();
        int numPerBatch = 50;
        ShowProgressBar();
        int numBatches = numMostRecentOnes / numPerBatch;
        int logMessagesOnServer = App.GetPlayer().cd.totalLogMessagesOnServer;
        int startIndex = logMessagesOnServer - numBatches * numPerBatch + 1;
        totalLogMessagesReceived = 0;
        for (int i = startIndex; i < logMessagesOnServer; i += numPerBatch){
            // Send request to update the whole log.
            EGPacket pack = EGRequest.FetchLog(App.GetPlayer(), i, numPerBatch, oldestIDtoInclude);
            packetsSent.add(pack);
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    HideProgressBar();                        // Hide as long as we get something?
                    switch (reply.ResType()){
                        case LogMessages: {
                            replies.add(reply);
                            break;
                        }
                        default:
                            HideProgressBar(); // Some error?
                    }
                }
                @Override
                public void OnError(EGPacketError error) {
                    // Hide as long as we get something?
                    HideProgressBar();
                    Toast("RequestLogMessages: An error occurred");
                }
            });
            App.Send(pack);
        }
        System.out.println("Sent "+packetsSent.size()+" req packets");
        logMessagesUpdatedHandler.sendMessageDelayed(new Message(), 1000);
        return true;
    }

    Handler logMessagesUpdatedHandler = new Handler(new Handler.Callback() {
        void UpdateSelf(int ms){
            logMessagesUpdatedHandler.sendMessageDelayed(new Message(), ms);
        };
        @Override
        public boolean handleMessage(Message msg) {
            System.out.println("logMessagesUpdatedHandler?");
            // Check if all replies have been received?
            int packsWaitingUpdates = 0;
            for (int i = 0; i < packetsSent.size(); ++i){
                EGPacket pack = packetsSent.get(i);
                if (pack.GetReplies().size() == 0) {
                    ++packsWaitingUpdates;
                }
            }
            if (packsWaitingUpdates > 0) {
                UpdateSelf(packsWaitingUpdates * 100);
                Toast("Waiting for "+packsWaitingUpdates+" packets..");
                return false;
            }
            // yeah o.o
            HideProgressBar();
            EList<Log> newLog = new EList<>();
            for (int i = 0; i < replies.size(); ++i){
                EList<Log> messages = null;
                EGPacket reply = replies.get(i);
                switch (reply.ResType()){
                    case LogMessages:
                        System.out.println("LogMessages received");
                        messages = reply.GetLogMessages();
                        break;
                    default:
                        System.out.println("Reply: "+reply.ResType().name());
                        continue;
                }
                if (messages == null){
                    Toast("Some bad data when parsing.");
                }
                else {
                    newLog.addAll(messages); // UpdateLogMessages(messages);
                    totalLogMessagesReceived += messages.size();
                }
            }
            if (newLog.size() > 0) {
                Player player = App.GetPlayer();
                player.log = newLog;
            }
            UpdateLog(); // Update UI once we have them all.
            System.out.println("Total messages received: "+totalLogMessagesReceived);
            PrintLastLogMessages(5);
            // Update the gui?
            OnLogMessagesUpdated();
            return false;
        }
    });

    private void PrintLastLogMessages(int num) {
        Log.PrintLastLogMessages(App.GetPlayer().log, num);
    }

    void OnLogMessagesUpdated(){
        UpdateLog();
        EList<Log> ell = NewLogMessagesToPresent();
        if (ell != null && ell.size() > 0) {
            GoToResultsScreen();
        }
    }


    static long lastRCD = 0;
    protected void RequestClientData() {
        long now = System.currentTimeMillis();
        if (now < lastRCD + 5000)
            return;
        lastRCD = now;
        System.out.println("Requesting client data... queuedActiveActions: "+App.GetPlayer().cd.queuedActiveActions.size());
        ToastUp("Saving/Updating...");
        try {
            EGPacket pack = EGRequest.Save(App.GetPlayer());
            ShowProgressBar();
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    HandleClientData(reply);
                }
                @Override
                public void OnError(EGPacketError error) {
                    HideProgressBar();
                }
            });
            App.Send(pack);;
        } catch (NullPointerException e){
            ToastUp(e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    // Returns true if it was a success - got some data, or false - if an error was replied or parsing errors occurred.
    protected boolean HandleClientData(EGPacket reply) {
        if (reply instanceof EGResponse){
            switch (reply.ResType()){
                case PlayerClientData:
                    ClientData cd = reply.GetClientData();
                    if (cd == null) {
                        HideProgressBar();
                        ToastUp("Errors parsing client data");
                        return false;
                    }
                    Toast("Client data received");
                    cd.PrintDetails();
                    App.GetPlayer().UpdateFrom(cd);
                    UpdateUI();
                    // Request new log messages if relevant.
                    if (!RequestLogMessages(100, (long) 0)) // Add option to only request new messages? App.GetPlayer().Get(Config.LatestLogMessageIDSeen)
                        HideProgressBar(); // If not requesting log messages, then hide progress bar straight away, otherwise wait for the incoming log-messages.
                    return true;
                default:
                    ToastUp("Other reply: "+reply.ResType().name());
            }
        }
        return false;
    }

    /// To determine if we should open the Results-screen.
    private EList<Log> NewLogMessagesToPresent(){
        // Populate it with those log messages which we are interested in viewing here in the results screen? All log messages which haven't been flagged as "displayed" yet.
        Player p = App.GetPlayer();
        if (p.log == null){
            ToastUp("log messages null");
            p.log = new EList<>();
            return null;
        }
        int logMessagesAdded = 0;
        long lastLogMessageIDSeen = (long) p.Get(Config.LatestLogMessageIDSeen);
        System.out.println("LastID seen: "+lastLogMessageIDSeen);
        EList<Log> toDisplay = new EList<>();
        for (int i = 0; i < p.log.size(); ++i){
            Log l = p.log.get(i);
            if (l.LogID() <= lastLogMessageIDSeen)
                continue;
            // The old check, where it bases relevant messages on what is saved server-side, which was bugging more or less....
            //     if (player.log.get(i).displayedToEndUser == 0)
            //            return true;
            System.out.println("l.LogID(): "+l.LogID());
            toDisplay.add(l);
        }
        return toDisplay;
    }

    void GoToResultsScreen(){
        Intent i = new Intent(getBaseContext(), ResultsScreen.class);// Check the log for new messages -... will be there, so wtf just go to the walla walla.
        startActivity(i);
    }

    protected void LoadPlayerData(Player player, final OnPacketReplyListener onPacketReplyListener) {
        System.out.println("Load player data...");
        EGPacket pack = EGRequest.Load(player);
        ShowProgressBar();
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                HideProgressBar();
                if (HandleClientData(reply))
                    onPacketReplyListener.onSuccess();
            }
            @Override
            public void OnError(EGPacketError error) {
                HideProgressBar();
            }
        });
        App.Send(pack);;
    }

}