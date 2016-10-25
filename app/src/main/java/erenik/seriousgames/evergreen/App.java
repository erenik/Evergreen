package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Created by Emil on 2016-10-26.
 */
public class App {
    // Static reference. Updated from listener declared in Startup.
    static Activity currentActivity;

    static Application.ActivityLifecycleCallbacks actLCCallback;


    static SharedPreferences GetPreferences()
    {
        return currentActivity.getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
    }
    /// Makes a new singleton activity lc callback listener.
    static void NewActivityLifeCycleCallback() {
        actLCCallback = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityDestroyed(Activity activity) {
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
            }
            @Override
            public void onActivityStopped(Activity activity) {
                if (App.currentActivity == activity)
                    App.currentActivity = null;
            }
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }
        };
    }
}