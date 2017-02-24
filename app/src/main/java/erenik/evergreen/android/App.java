package erenik.evergreen.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import erenik.evergreen.android.act.EventDialogFragment;
import erenik.evergreen.android.act.EvergreenActivity;
import erenik.evergreen.android.act.GameOver;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.packet.EGPacketCommunicator;
import erenik.evergreen.common.player.*;
import erenik.evergreen.R;

/**
 * Created by Emil on 2016-10-26.
 */
public class App {

    static public String ip = "www.erenik.com";
    static public int port = 4000;

    // Static reference. Updated from listener declared in Startup.
    public static Activity currentActivity;
    public static Activity mainScreenActivity;

    public static Application.ActivityLifecycleCallbacks actLCCallback;

    static List<Activity> runningActivities = new ArrayList<Activity>();

    /// List of players.
    static private List<Player> players = new ArrayList<>();
    // Player, should be put or created into the list of players, based on what was loaded upon start-up.
    static private Player player = null;
    public static EGPacketCommunicator comm = new EGPacketCommunicator();
    public static boolean isLocalGame = false, isMultiplayerGame = false; // Set upon start.

    static public Player GetPlayer()
    {
        return player;
    }

    public static boolean Save() {
        EvergreenActivity ea = (EvergreenActivity)currentActivity;
        if (ea == null)
            return false;
        return ea.Save();
    }

    public static boolean HandleGeneratedEvents() {
        if (true) return true;
        /*
        // Returns true if there was any event to process, false if not.
        EventDialogFragment event = new EventDialogFragment();
        event.type = player.NextEvent();
        if (event.type == Finding.Nothing)
        {
            System.out.println("HandleGeneratedEvents: nothing");
            return false;
        }
        System.out.println("HandleGeneratedEvents, type: " + event.type.name());

        /// For now, just skip the event, act as if it was already handled.
        player.PopEvent(event.type);
        if (true)
            return true; // Mark that all have been completed now.

        /// Old code for initiating a fragment activity, requesting the user to do something. Will use later.
        if (currentActivity instanceof android.support.v4.app.FragmentActivity)
        {
            FragmentActivity fa = (FragmentActivity) currentActivity;
            android.support.v4.app.FragmentManager fragMan = fa.getSupportFragmentManager();
            event.show(fragMan, "event");
            return true;
        }
        else {
            System.out.println("Activity not instance of fragmentAcitvity: "+currentActivity.getLocalClassName());
        }*/
        return false;
    };

    public static int GetColorForLogType(LogType t)
    {
        switch(t)
        {
            case ATTACK: return R.color.attack;
            case ATTACKED_MISS:
            case ACTION_FAILURE:
            case ATTACK_MISS: return R.color.attackMiss;
            case INFO: return R.color.info;
            case DEFEATED_ENEMY:
            case SUCCESS: return R.color.success;
            case PROGRESS: return R.color.progress;
            case EXP: return R.color.exp;
            case DEFEATED:
                return R.color.defeated;
            case OtherDamage:
            case ATTACKED:
                return R.color.attacked;
            case EVENT: return R.color.event;
            case PROBLEM_NOTIFICATION: return R.color.problemNotification;
        }
        return R.color.black;
    }

    /// No filter, all is shown.
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay)
    {
        UpdateLog(vg, context, maxLinesToDisplay, Arrays.asList(LogType.values()));
    }
    // Specify filter.
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay, List<LogType> typesToShow)
    {

    }

    /// Utility method.
    public static Point GetScreenSize(){
        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }
    /// Go to game-over screen.
    public static void GameOver() {
        System.out.println("GaME OVER!!!");
        Intent i = new Intent(currentActivity.getBaseContext(), GameOver.class);
        currentActivity.startActivity(i);
    }
    public static SharedPreferences GetPreferences()
    {
        Activity ac = currentActivity;
        if (ac == null)
            ac = mainScreenActivity;
        else if (ac == null && runningActivities.size() > 0)
            ac = runningActivities.get(0);
        System.out.println("currentActivity: "+currentActivity+" mainScreen: "+mainScreenActivity+" index 0: "+(runningActivities.size() > 0? runningActivities.get(0) : "no"));
        if (ac == null)
            return null;
        return GetPreferences(ac);
    }
    public static SharedPreferences GetPreferences(Activity forActivity)
    {
        return forActivity.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
    }
    /// Makes a new singleton activity lc callback listener.
    public static void NewActivityLifeCycleCallback(Activity activity)
    {
        if (actLCCallback != null) // Create it once only.
            return;
        actLCCallback = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityDestroyed(Activity activity) {
                runningActivities.remove(activity);
                if (App.currentActivity == activity)
                    App.currentActivity = null;
            }
            @Override
            public void onActivityResumed(Activity activity) {
                System.out.println("Acitivity resumed: " + activity.getLocalClassName());
                App.currentActivity = activity;
            }
            @Override
            public void onActivityPaused(Activity activity) {
                if (App.currentActivity == activity)
                    App.currentActivity = null;
            }
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {
                System.out.println("Acitivity created: " + activity.getLocalClassName());
                App.currentActivity = activity; // this?
            }
            @Override
            public void onActivityStarted(Activity activity) {
                System.out.println("Acitivity started: " + activity.getLocalClassName());
                App.currentActivity = activity;
                runningActivities.add(activity);
            }
            @Override
            public void onActivityStopped(Activity activity) {
                if (App.currentActivity == activity)
                    App.currentActivity = null;
                runningActivities.remove(activity);
            }
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }
        };
        App.currentActivity = activity; // Point it right.
        activity.getApplication().registerActivityLifecycleCallbacks(App.actLCCallback); // Set lifecycle callback listener for the application/activity.
    }

    public static void OnActivityCreated(Activity activity)
    {
        App.NewActivityLifeCycleCallback(activity);
    }

    public static String GetLogText(LogTextID logTextID, List<String> args) {
        return EString.GetLogText(logTextID, args);
    }

    /// Saves locally, using default preferences location.
    public static final String localFileSaveName = "Evergreen.sav";
    public static boolean SaveLocally() {
        if (currentActivity == null) {
            System.out.println("Couldn't save, no current activity D:");
            return false;
        }
        System.out.println("SaveLocally");
        Context context = currentActivity.getBaseContext();
        Player player = App.GetPlayer(); // Fetch current player to save.
        if (context == null) {
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
        editor.putInt(Constants.NUM_PLAYERS, players.size());
        editor.apply();
        ObjectOutputStream objectOut = null;
        try {
            FileOutputStream fileOut = null;
            try {
                fileOut = context.openFileOutput(localFileSaveName, Activity.MODE_PRIVATE);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            objectOut = new ObjectOutputStream(fileOut);
            // Write # of players?
            for (int i = 0; i < players.size(); ++i) {
                Player p = players.get(i);
                objectOut.writeObject(p);
            }
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
        editor.apply();
        System.out.println("Saved");
        return true;
    }

    /// Loads data from shared preferences to see if there is character data.
    public static boolean LoadLocally() {
        System.out.println("LoadLocally");
        Context context = currentActivity.getBaseContext();
        SharedPreferences sp = App.GetPreferences();
        if (sp == null) {
            System.out.println("Unable to save locally in preferences: Unable to fetch preferences.");
            return false;
        }
        boolean saveExists = sp.getBoolean(Constants.SAVE_EXISTS, false);
        if (saveExists == false) {
            System.out.println("No save exists in saved preferences. Returning.");
            return false;
        }
        int numPlayersSaved = sp.getInt(Constants.NUM_PLAYERS, 0);
        if (numPlayersSaved == 0) {
            System.out.println("Save exists, but 0 players..?");
            return true;
        }
        String activePlayerName = player != null? player.name : "";
        // Clear old players in list.
        players.clear();
        player = null; // reset pointer
        ObjectInputStream objectIn = null;
        Object object = null;
        try {
            FileInputStream fileIn = context.getApplicationContext().openFileInput(localFileSaveName);
            objectIn = new ObjectInputStream(fileIn);
            for (int i = 0; i < numPlayersSaved; ++i) {
                Player p = (Player) objectIn.readObject();
                players.add(p);
                if (p.name.equals(activePlayerName)) // Retain active player if loading while still playing.
                    player = p;
            }
        } catch (FileNotFoundException e) {
            System.out.println("No file found with name "+localFileSaveName);
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
        System.out.println("Loaded "+numPlayersSaved+" player characters successfully.");
        return true;
    }

    // Check all saves, return the players.
    public static List<Player> GetPlayers() {
        return players;
    }
    /// Adds a player to the list of players.
    public static void RegisterPlayer(Player player) {
        players.add(player);
        SaveLocally();
    }

    /// Makes the player active, and changes app config to enable swift handling of its associated game, locality, etc.
    public static void MakeActivePlayer(Player playerToBecomeActive) {
        App.player = playerToBecomeActive;
        System.out.println("GameID: "+playerToBecomeActive.gameID);
        if (playerToBecomeActive.gameID <= 0){
            App.isLocalGame = true;
        }
        else
            App.isLocalGame = false;
        App.isMultiplayerGame = !App.isLocalGame;
    }

    public static Player GetMostRecentlyEditedPlayer() {
        Player mostRecent = players.get(0);
        for (int i = 1; i < players.size(); ++i) {
            Player p = players.get(i);
            if (p.lastEditSystemMs > mostRecent.lastEditSystemMs)
                mostRecent = p;
        }
        return mostRecent;
    }
    /// Deletes all player characters and saves locally.
    public static void DeletePlayers() {
        players.clear();
        SaveLocally();
    }

    public static int GetDrawableForAvatarID(int i) {
        switch (i){
            case 0: return R.drawable.av_00;
            case 1: return R.drawable.av_01;
            case 2: return R.drawable.av_02;
            case 3: return R.drawable.av_03;
            case 4: return R.drawable.av_04;
            case 5: return R.drawable.av_05;
            case 6: return R.drawable.av_06;
            case 7: return R.drawable.av_07;
        }
        return R.drawable.icon;
    }
}