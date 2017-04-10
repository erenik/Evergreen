package erenik.evergreen.android;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;

import erenik.evergreen.common.Player;
import erenik.util.Printer;

/**
 * Queries the server every X minutes or hours for new updates and presents notifications if there are any new updates to be had.
 * Created by Emil on 2017-04-10.
 */

public class BackgroundUpdateService extends Service {

    public static final int START_SERVICE = 1;
    public static final int STOP_SERVICE = 2;
    public static String REQUEST_TYPE = "RequestType";

    static BackgroundUpdateService instance = null;
    private static boolean stop = false;
    static UpdaterThread thread = null;


    static String notificationPresent = "NotificationPresent";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){ // Wtf even?
            return super.onStartCommand(intent, flags, startId);
        }
        /// Uhhh.. Idk.
        int cmd = intent.getIntExtra(BackgroundUpdateService.REQUEST_TYPE, -1);
        if (cmd == -1){
            Printer.out("Lacking request type int extra.");
            new Exception().printStackTrace();
            return super.onStartCommand(intent, flags, startId);
        }
//        Printer.out("onStartCommand: "+cmd);
        switch (cmd) {
            case START_SERVICE:
                StartService();
                Printer.out("Starting up BackgroundUpdateService");
                break;
            case STOP_SERVICE:
                StopService();
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void StartService() {
        if (IsNotificationPresent())
            return;

        Printer.out("Starting service BackgroundUpdateService");
        stop = false;
        // Start the thread.
        if (thread == null){
            thread = new UpdaterThread(this);
            thread.start();
        }
    }
    private void StopService(){
        Printer.out("Stopping service BackgroundUpdateService");
        stop = true;
    }

    public static boolean IsNotificationPresent() {
        SharedPreferences pref = App.GetPreferences();
        if (pref == null)
            return true;
        return App.GetPreferences().getBoolean(BackgroundUpdateService.notificationPresent, false);
    }

    private class UpdaterThread extends Thread {
        BackgroundUpdateService callingService;
        UpdaterThread(BackgroundUpdateService callingService){
            this.callingService = callingService;
        }
        @Override
        public void run() {
            super.run();
            Printer.out("BackgroundUpdateService thread start");

            while (BackgroundUpdateService.stop == false){
                Printer.out("BackgroundUpdateService thread loop");
                Network.CheckForUpdates(callingService);
                // Query.
                Player player = App.GetPlayer();
                if (player == null){
                    App.LoadLocally();
                    player = App.GetPlayer();
                    if (player == null){
                        Printer.out("No player saved locally, no point querying server for updates.");
                        BackgroundUpdateService.stop = true;
                        break;
                    }
                }
                Printer.out("Querying server for updates");
                Network.CheckForNewDataOnServer(player, getBaseContext());
                try {
                    Thread.sleep(180000); // Sleep 3 minutes between polls? at least
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Printer.out("Updater thread ending");
            BackgroundUpdateService.thread = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.exit(1);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Printer.out("TransportDetectionService onCreate");
        StartService();
        instance = this;
    }

    @Override
    public void onDestroy() {
        Printer.out("onDestroy called D:");
        StopService();
        super.onDestroy();
        instance = null;
    }

}
