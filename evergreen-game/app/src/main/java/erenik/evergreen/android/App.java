package erenik.evergreen.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import erenik.evergreen.GameID;
import erenik.evergreen.android.act.EvergreenActivity;
import erenik.evergreen.android.act.GameOver;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketError;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequest;
import erenik.evergreen.common.player.*;
import erenik.evergreen.R;
import erenik.util.EList;
import erenik.util.Printer;

/**
 * Created by Emil on 2016-10-26.
 */
public class App {


//    static public String ip = "www.erenik.com";
//    static public int port = 4000;

    // Static reference. Updated from listener declared in Startup.
    public static EvergreenActivity currentActivity;
    public static Activity mainScreenActivity;

    public static Application.ActivityLifecycleCallbacks actLCCallback;

    static EList<Activity> runningActivities = new EList<Activity>();

    /// EList of players.
    static private EList<Player> players = new EList<>();
    // Player, should be put or created into the list of players, based on what was loaded upon start-up.
    static private Player player = null;
    /// Used during creation of new characters ONLY. After creation the integer should be checked with the player object itself.
    public static int gameID = GameID.BadID;
    // public static boolean isLocalGame = false,
     //       isMultiplayerGame = false; // Set upon start.

    static public Player GetPlayer() {
        return player;
    }

    public static boolean Save() {
        EvergreenActivity ea = (EvergreenActivity)currentActivity;
        if (ea == null)
            return false;
        return ea.SaveLocally();
    }

    public static boolean HandleGeneratedEvents() {
        if (true) return true;
        /*
        // Returns true if there was any event to process, false if not.
        EventDialogFragment event = new EventDialogFragment();
        event.type = player.NextEvent();
        if (event.type == Finding.Nothing)
        {
            Printer.out("HandleGeneratedEvents: nothing");
            return false;
        }
        Printer.out("HandleGeneratedEvents, type: " + event.type.name());

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
            Printer.out("Activity not instance of fragmentAcitvity: "+currentActivity.getLocalClassName());
        }*/
        return false;
    };

    public static int GetColorForLogType(LogType t) {
        switch(t) {
            case ATTACK: return R.color.attack;
            case ATTACKED_MISS:
            case ACTION_FAILURE:
            case ENC_INFO_FAILED:
            case ATTACK_MISS: return R.color.attackMiss;
            case ENC_INFO:
            case INFO: return R.color.info;
            case DEFEATED_ENEMY:
            case SUCCESS: return R.color.success;
            case PROGRESS: return R.color.progress;
            case ACTION_NO_PROGRESS: return R.color.actionNoProgress;
            case EXP: return R.color.exp;
            case DEFEATED:
                return R.color.defeated;
            case OtherDamage:
            case ATTACKED:
                return R.color.attacked;
            case EVENT: return R.color.event;
            case PROBLEM_NOTIFICATION: return R.color.problemNotification;
            case PLAYER_ATTACK:
                return R.color.orange;
            case PLAYER_ATTACKED_ME: return R.color.attacked;
            case PLAYER_ATTACK_MISS:
                return R.color.grey80;
            case PLAYER_ATTACKED_ME_BUT_MISSED: return R.color.grey90;
            default:
                Printer.out("Using default color");
                return R.color.grey50;
        }
//        return R.color.black;
    }

    /// No filter, all is shown.
    /*
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay)
    {
        UpdateLog(vg, context, maxLinesToDisplay, Arrays.asList(LogType.values()));
    }
    // Specify filter.
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay, EList<LogType> typesToShow)
    {
        Printer.out("WTF?");
    }*/

    /// Utility method.
    public static Point GetScreenSize(){
        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }
    /// Go to game-over screen.
    public static void GameOver() {
        Printer.out("GaME OVER!!!");
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
//        Printer.out("currentActivity: "+currentActivity+" mainScreen: "+mainScreenActivity+" index 0: "+(runningActivities.size() > 0? runningActivities.get(0) : "no"));
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
                Printer.out("Acitivity resumed: " + activity.getLocalClassName());
                App.currentActivity = (EvergreenActivity) activity;
            }
            @Override
            public void onActivityPaused(Activity activity) {
                if (App.currentActivity == activity)
                    App.currentActivity = null;
            }
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {
                Printer.out("Acitivity created: " + activity.getLocalClassName());
                App.currentActivity = (EvergreenActivity) activity; // this?
            }
            @Override
            public void onActivityStarted(Activity activity) {
                Printer.out("Acitivity started: " + activity.getLocalClassName());
                App.currentActivity = (EvergreenActivity) activity;
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
        App.currentActivity = (EvergreenActivity) activity; // Point it right.
        activity.getApplication().registerActivityLifecycleCallbacks(App.actLCCallback); // Set lifecycle callback listener for the application/activity.
    }

    public static void OnActivityCreated(Activity activity)
    {
        App.NewActivityLifeCycleCallback(activity);
    }

    public static String GetLogText(LogTextID logTextID, EList<String> args, Context context) {
        return EString.GetLogText(logTextID, args, context);
    }

    /// Saves locally, using default preferences location.
    public static final String localFileSaveName = "Evergreen.sav";
    public static boolean SaveLocally() {
        if (currentActivity == null) {
            Printer.out("Couldn't save, no current activity D:");
            return false;
        }
        Printer.out("SaveLocally");
        Context context = currentActivity.getBaseContext();
        Player player = App.GetPlayer(); // Fetch current player to save.
        if (context == null) {
            Printer.out("Context null. Aborting");
            return false;
        }
        SharedPreferences sp = App.GetPreferences();
        if (sp == null) {
            Printer.out("Unable to save locally in preferences: Unable to fetch preferences.");
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
                p.sendAll = Player.SEND_ALL; // Save everything locally.
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
                    Printer.out("Failed to save");
                    return false;
                }
            }
        };
        editor.apply();
        Printer.out("Saved");
        return true;
    }

    /// Loads data from shared preferences to see if there is character data.
    public static boolean LoadLocally() {
        Printer.out("LoadLocally");
        Context context = currentActivity.getBaseContext();
        SharedPreferences sp = App.GetPreferences();
        if (sp == null) {
            Printer.out("Unable to save locally in preferences: Unable to fetch preferences.");
            return false;
        }
        boolean saveExists = sp.getBoolean(Constants.SAVE_EXISTS, false);
        if (saveExists == false) {
            Printer.out("No save exists in saved preferences. Returning.");
            return false;
        }
        int numPlayersSaved = sp.getInt(Constants.NUM_PLAYERS, 0);
        if (numPlayersSaved == 0) {
            Printer.out("Save exists, but 0 players..?");
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
                if (p == null)
                    continue; // Skip if it went bad.
                players.add(p);
                if (p.name.equals(activePlayerName)) // Retain active player if loading while still playing.
                    player = p;
            }
        } catch (FileNotFoundException e) {
            Printer.out("No file found with name "+localFileSaveName);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (ClassCastException e){
            e.printStackTrace();
            return false;
        }
        finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    // do nowt
                    return false;
                }
            }
        }
        Printer.out("Loaded "+numPlayersSaved+" player characters successfully.");
        return true;
    }

    // Check all saves, return the players.
    public static EList<Player> GetPlayers() {
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
        if (players.indexOf(playerToBecomeActive) == -1) // Add it to the array if needed :)
            players.add(playerToBecomeActive);
        Printer.out("GameID: "+playerToBecomeActive.gameID);
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
            case 8: return R.drawable.av_08;
            case 9: return R.drawable.av_09;
            case 10: return R.drawable.av_10;
            case 11: return R.drawable.av_11;
            case 12: return R.drawable.av_12;
            case 13: return R.drawable.av_13;
            case 14: return R.drawable.av_14;
            case 15: return R.drawable.av_15;
            case 16: return R.drawable.av_16;
            case 17: return R.drawable.av_17;
  //          case 18: return R.drawable.av_18;
//            case 19: return R.drawable.av_19;
        }
        return -1;
    }
    public static int AvatarIDs() {
        for (int i = 0; ; ++i){
            int id = GetDrawableForAvatarID(i);
            if (id == -1)
                return i;  // Means we had up until this number in total :)
        }
    }


    public static void DoQueuedActions() {
        // If local game, apply changes straight away.
        // For multiplayer, launch a Thread or Async
        EGPacket pack = EGRequest.PerformActiveActions(GetPlayer());
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                currentActivity.Toast("Active actions sent.");
            }

            @Override
            public void OnError(EGPacketError error) {
                currentActivity.Toast("Error: "+error.name());
            }
        });
        Send(pack);
        currentActivity.Toast("Active action queued.");
    }

    public static void Send(EGPacket pack){
        if (currentActivity == null) {
            Printer.out("Current activity null, failed to sent packet.");
        }
        Network.Send(pack, currentActivity);
    }



    public static void SetPlayers(EList<Player> players) {
        App.players = players;
        App.SaveLocally();
    }

    public static void UpdatePlayer(Player player) {
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            if (p.name.equals(player.name) && p.gameID == player.gameID){
                // Remove from array.
                players.remove(p);
                players.add(player);
                player.log = p.log;
                player.lastSaveTimeSystemMs = System.currentTimeMillis();
                App.player = player; // Assume it is the active one.
                return;
            }
        }
        Printer.out("FAILED TO UPDATE");
        System.exit(14);
    }

    public static boolean IsLocalGame() {
        if (player != null)
            return player.gameID == GameID.LocalGame;
        return App.gameID == GameID.LocalGame;
    }

    public static void SetLocalGame() {
        App.gameID = GameID.LocalGame;
    }

    public static void SetMultiplayer() {
        App.gameID = GameID.GlobalGame; // Can be any or all, up to the server (testing phase).
    }

    public static void SetActivePlayer(Player toBeActivePlayer) {
        player = toBeActivePlayer;
    }

    public static void RemovePlayer(Player player) {
        players.remove(player);
        SaveLocally();
    }

    public static void UpdateUI() {
        Activity act = currentActivity;
        if (act instanceof EvergreenActivity){
            EvergreenActivity ea = (EvergreenActivity) act;
            ea.UpdateUI();
        }
    }
}