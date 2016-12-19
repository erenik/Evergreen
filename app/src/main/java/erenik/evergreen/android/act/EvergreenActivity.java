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
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import erenik.evergreen.Game;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGPacketSender;
import erenik.evergreen.common.packet.EGRequestType;
import erenik.evergreen.common.player.Constants;
import erenik.evergreen.common.packet.EGPacketReceiver;
import erenik.evergreen.common.packet.EGRequest;


/**
 * Created by Emil on 2016-12-09.
 */
public class EvergreenActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set default orientation - Portrait.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Request full-screen.
//        setSystemUiVisibility(SYSTEM_UI_FLAG_FULLSCREEN);
        // Store common shit here?
        App.OnActivityCreated(this);  // Setup system callbacks as/if needed.

        setupFullscreenFlags();
//        delayedHide(0); // Hide ASAP.
        hideControls();
    }
    /* After creation, Cause hideControls of system controls after 500 ms? */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Trigger the initial hideControls() shortly after the activity has been // created, to briefly hint to the user that UI controls // are available.
//        delayedHide(100);
        hideControls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideControls();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        hideControls();
//        delayedHide(0);
//        hideControls();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        hideControls();
    }

    static public List<Game> gameList = new ArrayList<>();

    void GetGamesList()
    {
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

    void setupFullscreenFlags()
    {
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
        /*
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        */
        // Upon interacting with UI controls, delay any scheduled hideControls()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    /// General Save/Load. Will determine if it needs to perform it locally or remotely.
    public boolean Save()
    {
        // For now, save locally and remotely, for testing purposes.
        SaveLocally();
        SaveToServer();
        return true;
    }
    public boolean Load()
    {
        LoadLocally();
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

    private boolean SaveToServer()
    {
        Player player = App.GetPlayer();
        System.out.println("Saving to server.");
        // Connect to server.
//        EGPacketReceiver.StartSingleton();
        EGRequest req = EGRequest.Save(player);
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
        if (mVisible) {
            hideControls();
        } else {
            showControls();
        }
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


}
