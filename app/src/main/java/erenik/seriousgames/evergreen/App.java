package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.app.Application;
import android.app.FragmentManager;
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

import erenik.seriousgames.evergreen.act.EventDialogFragment;
import erenik.seriousgames.evergreen.act.GameOver;
import erenik.seriousgames.evergreen.act.Startup;
import erenik.seriousgames.evergreen.logging.Log;
import erenik.seriousgames.evergreen.logging.LogType;
import erenik.seriousgames.evergreen.player.Constants;
import erenik.seriousgames.evergreen.player.Finding;
import erenik.seriousgames.evergreen.player.Player;

/**
 * Created by Emil on 2016-10-26.
 */
public class App {
    // Static reference. Updated from listener declared in Startup.
    public static Activity currentActivity;
    public static Activity mainScreenActivity;

    public static Application.ActivityLifecycleCallbacks actLCCallback;

    static List<Activity> runningActivities = new ArrayList<Activity>();
    // Player
    static private Player player = new Player();
    static public Player GetPlayer()
    {
        return player;
    }

    // Returns true if it processed an event, false if not.
    public static boolean HandleNextEvent() {
        if (player.playEvents  // If playing all events, or
                || !player.AllMandatoryEventsHandled()) // Not all mandatory events handled,
            return HandleGeneratedEvents(); // Do event.
        return false;
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

    public static final String localFileSaveName = "Evergreen.sav";
    public static boolean SaveLocally(Context context)
    {
        if (context == null) // Fetch current one?
            context = currentActivity.getApplicationContext();
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

    public static boolean LoadLocally(Context context) {

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

    /// No filter, all is shown.
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay)
    {
        UpdateLog(vg, context, maxLinesToDisplay, Arrays.asList(LogType.values()));
    }
    // Specify filter.
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay, List<LogType> typesToShow)
    {
        System.out.println("Log.UpdateLog");
        Player player = App.GetPlayer();
        ViewGroup v = vg;
        // Remove children.
        v.removeAllViews();
        // Add new ones?
        int numDisplay = player.log.size();
        numDisplay = numDisplay > maxLinesToDisplay ? maxLinesToDisplay : numDisplay;
        int startIndex = player.log.size() - numDisplay;
        System.out.println("Start index: "+startIndex+" log size: "+player.log.size());
        View lastAdded = null;
        for (int i = player.log.size() - 1; i >= 0; --i)
        {
            Log l = player.log.get(i);
            boolean show = false;
            for (int j = 0; j < typesToShow.size(); ++j)
            {
                if (l.type.ordinal() == typesToShow.get(j).ordinal())
                    show = true;
            }
            if (!show)
                continue;
            String s = l.text;
            TextView t = new TextView(context);
            t.setText(s);
            int hex = ContextCompat.getColor(context, GetColorForLogType(l.type));
            // System.out.println("Colorizing: "+Integer.toHexString(hex));
            t.setTextColor(hex);
            v.addView(t, 0); // Insert at index 0 always.
            t.setFocusable(true); // Focusable.
            t.setFocusableInTouchMode(true);
            if (v.getChildCount() >= maxLinesToDisplay)
                break;
        }
        lastAdded = v.getChildAt(v.getChildCount()-1);
        if (lastAdded != null)
            lastAdded.requestFocus(); // Request focus, make visible?
    }


    /// Utility method.
    public static Point GetScreenSize(){
        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }
    /// Go to game-over screen.
    public static void GameOver()
    {
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