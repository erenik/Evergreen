package erenik.evergreen.android.act;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import erenik.evergreen.Game;
import erenik.evergreen.android.ui.EvergreenButton;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Invention.InventionType;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequestType;
import erenik.evergreen.common.player.Constants;
import erenik.evergreen.common.packet.EGRequest;


/**
 * Created by Emil on 2016-12-09.
 */
public class EvergreenActivity extends AppCompatActivity
{
    View scrollViewLog = null, layoutLog = null;
    protected int maxLogLinesInEventLog = 50; // Default, change if the activity should show more.
    boolean focusLastLogMessageUponUpdate = false;

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
        }
        if (layoutLog != null) {
            System.out.println("Layout for Event log found.");
            layoutLog.setOnClickListener(toggleLogFullScreen);
        }
        UpdateLog(); // Update log after subclass has adjusted how it should be presented.
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

    static public List<Game> gameList = new ArrayList<>();

    void GetGamesList() {
        System.out.println("Requesting games list from server...");
        EGPacket pack = EGRequest.byType(EGRequestType.GetGamesList);
        pack.addReceiverListener(new EGPacketReceiverListener()
        {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                gameList = reply.parseGamesList();
            }
        });
        App.comm.Send(pack);
//        pack.SendToServer(); // Send to default server.
    }

    void setupFullscreenFlags() {
        // hideControls()?
        mVisible = true; // Default controls are visible.
        // Set up the user interaction to manually showControls or hideControls the system UI.
        mContentView = findViewById(R.id.fullscreen_view);
        if (mContentView != null) {
            mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggle();
                }
            });
        }
        /// Set up full-screen flags.
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);


        /*
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        */
        // Upon interacting with UI controls, delay any scheduled hideControls()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    /// General Save/Load. Will determine if it needs to perform it locally or remotely.
    public boolean Save() {
        // For now, save locally and remotely, for testing purposes.
        SaveLocally();
        if (App.isMultiplayerGame)
            SaveToServer();
        return true;
    }
    /// Ease of use method, just loads..?
    public boolean Load() {
        LoadLocally();
        if (App.isMultiplayerGame)
            LoadFromServer();
        return true;
    }

    /// Saves locally, using default preferences location.
    public static final String localFileSaveName = "Evergreen.sav";
    private boolean SaveLocally()
    {
        System.out.println("SaveLocally");
        Context context = getBaseContext();
        Player player = App.GetPlayer(); // Fetch current player to save.
        if (context == null) // Fetch current one?
            context = getApplicationContext();
        if (context == null)
        {
            System.out.println("Context null. Aborting");
            return false;
        }
        SharedPreferences sp = App.GetPreferences();
        if (sp == null) {
            System.out.println("Unable to save locally in preferences: Unable to fetch preferences.");
            return false;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.SAVE_EXISTS, true);
        editor.commit();
        ObjectOutputStream objectOut = null;
        try {

            FileOutputStream fileOut = null;
            try {
                fileOut = context.openFileOutput(localFileSaveName, Activity.MODE_PRIVATE);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(player);
            fileOut.getFD().sync();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException e2) {
                    // do nowt
                    System.out.println("Failed to save");
                    return false;
                }
            }
        };
        System.out.println("Saved");
        return true;
    }
    private boolean LoadLocally() {
        System.out.println("LoadLocally");
        Context context = getBaseContext();
        ObjectInputStream objectIn = null;
        Object object = null;
        try {

            FileInputStream fileIn = context.getApplicationContext().openFileInput(localFileSaveName);
            objectIn = new ObjectInputStream(fileIn);
            object = objectIn.readObject();

        } catch (FileNotFoundException e) {
            // Do nothing
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    // do nowt
                    return false;
                }
            }
        }
        return true;
    }

    private boolean SaveToServer() {
        Player player = App.GetPlayer();
        System.out.println("Saving to server.");
        // Connect to server.
//        EGPacketReceiver.StartSingleton();
        EGRequest req = EGRequest.Save(player);
        App.comm.Send(req);
//        req.WaitForResponse(1000); // No waiting allowed. Use callbacks only!
        req.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                System.out.println("Got reply? : "+reply.toString());
            }
        });
        return true;
    }
    public static boolean LoadFromServer() {
        // Connect to server.
        Player player = App.GetPlayer();
        System.out.println("Loading from server.");
        // Send shit.
        EGRequest req = EGRequest.Load(player);
        App.comm.Send(req);
        req.WaitForResponse(1000);
        if (req.GetReply() == null)
        {
            System.out.println("Failed to get a reply with current timeout.");
            return false;
        }
        System.out.println("Got reply? : "+req.GetReply().toString());
        return true;
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
        mContentView = findViewById(R.id.fullscreen_view);
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
        if (layoutLog == null)
            return;
        Player player = App.GetPlayer();
//        App.UpdateLog((ViewGroup) findViewById(R.id.layoutLog), getBaseContext(), maxLogLinesInEventLog, player.logTypesToShow);

        System.out.println("Log.UpdateLog");
        ViewGroup v = (ViewGroup) layoutLog;
        // Remove children.
        v.removeAllViews();
        // Add new ones?
        int numDisplay = player.log.size();
        numDisplay = numDisplay > maxLogLinesInEventLog ? maxLogLinesInEventLog : numDisplay;
        int startIndex = player.log.size() - numDisplay;
        System.out.println("Start index: "+startIndex+" log size: "+player.log.size());
        View lastAdded = null;
        for (int i = player.log.size() - 1; i >= 0; --i)
        {
            Log l = player.log.get(i);
            boolean show = false;
            for (int j = 0; j < player.logTypesToShow.size(); ++j)
            {
                if (l.type.ordinal() == player.logTypesToShow.get(j).ordinal())
                    show = true;
            }
            if (!show)
                continue;
            String s = l.text;
            TextView t = new TextView(getBaseContext());
            t.setText(s);
            int hex = ContextCompat.getColor(getBaseContext(), App.GetColorForLogType(l.type));
            // System.out.println("Colorizing: "+Integer.toHexString(hex));
            t.setTextColor(hex);
            v.addView(t, 0); // Insert at index 0 always.
            t.setFocusable(true); // Focusable.
            t.setFocusableInTouchMode(true);
            if (v.getChildCount() >= maxLogLinesInEventLog)
                break;
        }
        lastAdded = v.getChildAt(v.getChildCount()-1);
        if (lastAdded != null && focusLastLogMessageUponUpdate)
            lastAdded.requestFocus(); // Request focus, make visible?
    }

    protected void UpdateItemList(ViewGroup vg, InventionType type) {
        // Clear it.
        vg.removeAllViews();
        Player player = App.GetPlayer();
        for (int i = 0; i < player.inventory.size(); ++i)
        {
            Invention item = player.inventory.get(i);
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
            vg.addView(ll);

            // Make a button out of it.
            EvergreenButton b = new EvergreenButton(getBaseContext());
            b.setText(item.name);
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
//            removeButton.setOnClickListener(removeParentFromQueue);
            removeButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 4.0f)); // Weight at the end?
            ll.addView(removeButton);
        }

    }
    private final View.OnClickListener itemClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Button b = (Button)v;
           // clicked(b.getText());
        }
    };


}
