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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import erenik.evergreen.android.act.EventDialogFragment;
import erenik.evergreen.android.act.EvergreenActivity;
import erenik.evergreen.android.act.GameOver;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;
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
    // Player
    static private Player player = new Player();
    public static EGPacketCommunicator comm = new EGPacketCommunicator();
    public static boolean isLocalGame = false, isMultiplayerGame = false; // Set upon start.

    static public Player GetPlayer()
    {
        return player;
    }

    // Returns true if it processed an event, false if not.
    public static boolean HandleNextEvent() {
        if (true) return false; // Skip until mini-games are implemented later.
        if (player.playEvents  // If playing all events, or
                || !player.AllMandatoryEventsHandled()) // Not all mandatory events handled,
            return HandleGeneratedEvents(); // Do event.
        return false;
    }

    public static boolean Save()
    {
        EvergreenActivity ea = (EvergreenActivity)currentActivity;
        if (ea == null)
            return false;
        return ea.Save();
    }

    public static boolean HandleGeneratedEvents()
    {
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
        }
        return false;
    };

    public static int GetColorForLogType(LogType t)
    {
        switch(t)
        {
            case ATTACK: return R.color.attack;
            case ATTACKED_MISS:
            case ATTACK_MISS: return R.color.attackMiss;
            case INFO: return R.color.info;
            case SUCCESS: return R.color.success;
            case PROGRESS: return R.color.progress;
            case EXP: return R.color.exp;
            case OtherDamage: case ATTACKED: return R.color.attacked;
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

}