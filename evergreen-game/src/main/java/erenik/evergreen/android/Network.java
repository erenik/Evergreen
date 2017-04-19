package erenik.evergreen.android;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.android.BackgroundUpdateService;
import erenik.evergreen.android.act.MainScreen;
import erenik.evergreen.android.act.TitleScreen;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketCommunicator;
import erenik.evergreen.common.packet.EGPacketError;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequest;
import erenik.evergreen.common.player.Stat;
import erenik.util.Byter;
import erenik.util.Printer;

/** A handler for network operations, disregarding which activity operations are requested from
 * Created by Emil on 2017-03-27.
 */
public class Network {
    private static final String LAST_TURN = "LAST_TURN";
    private static int newTurnNotificationId = 16;
    static private EGPacketCommunicator comm = null;

    /// Sends the packet to its destination - probably the default server?
    static int handlerUpdateDelayMs = 20;

    static void InitCommunicator(Context context){
        if (comm != null)
            return;
        Printer.out("Init communicator called");
        comm = new EGPacketCommunicator();
        // Do some initial tests to see which IP to use? no?
        comm.SetServerIP("192.168.0.11"); // Home/local address
//        comm.SetServerIP("10.104.33.248"); // School address
     //   comm.SetServerIP("www.erenik.com"); // Public address
//
        if (context instanceof Service){
            Printer.out("Packet communicator initialized from a service. Remember to call CheckForUpdates() to gain any updates!");
        }
        else {
            Printer.out("Couldn't determine where context was from D:");
        }
    }

    // Service or Activity should be provided as argument.
    public static int CheckForUpdates(Context context){
        InitCommunicator(context);
        return comm.CheckForUpdates();
    }
    // Service or Activity should be provided as 2nd argument.
    public static void Send(EGPacket pack, Context context) {
        InitCommunicator(context);
        comm.Send(pack);
    }


    public static void HideNewTurnNotification(Context context){
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(newTurnNotificationId);         // mId allows you to update the notification later on.

        BackgroundUpdateService.IsNotificationPresent();

    }

    /// Merely polls if there exists data. If there exists new data, it will create a notification and also a Toast depending on if this app is the active one or not?
    public static void CheckForNewDataOnServer(Player player, final Context context) {
        // Query if there are any updates to be had.
        if (player != null) {
            EGRequest req = EGRequest.TurnSurvived(player);
            req.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    Object turns = Byter.toObject(reply.GetBody());
                    Printer.out("Turns received: "+turns);
                    if (turns instanceof Float){
                        Float fTurns = (Float) turns;
                        Player p = App.GetPlayer();
                        if (p == null)
                            return;
                        if (fTurns != p.Get(Stat.TurnSurvived)){
                            // Check last saved
                            SharedPreferences pref = App.GetPreferences();
                            float lastShownNewTurn = 0;
                            if (pref != null){
                                lastShownNewTurn = pref.getFloat(LAST_TURN, 0);
                                Printer.out("Last shown new turn: "+lastShownNewTurn);
                            }
                            else {
                                Printer.out("Couldn't open preferences");
                                return;
                            }
                            /// Present the notification or Toast if the turn has changed and has not been displayed previously!
                            if (lastShownNewTurn != fTurns){
                                if (App.currentActivity != null) {
                                    App.currentActivity.ToastUp("New turn - new updates available");
                                }
                                // TODO: Add Notifications here as well?

                                Intent resultIntent = new Intent(context, MainScreen.class);

                                // The stack builder object will contain an artificial back stack for the
                                // started Activity.
                                // This ensures that navigating backward from the Activity leads out of
                                // your application to the Home screen.
                                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                                stackBuilder.addParentStack(TitleScreen.class); // Adds the back stack for the Intent (but not the Intent itself)
                                stackBuilder.addNextIntent(resultIntent); // Adds the Intent that starts the Activity to the top of the stack
                                PendingIntent resultPendingIntent =
                                        stackBuilder.getPendingIntent(
                                                0,
                                                PendingIntent.FLAG_UPDATE_CURRENT
                                        );
                                Notification noti = new Notification.Builder(context)
                                        .setContentTitle("Updates in the Evergreen")
                                        .setContentText("New turn, new updates available")
                                        .setSmallIcon(R.drawable.notification_icon)
                                        .setContentIntent(resultPendingIntent)
//                                    .setLargeIcon()
                                        .build();
                                NotificationManager mNotificationManager =
                                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                // mId allows you to update the notification later on.
                                mNotificationManager.notify(newTurnNotificationId, noti);
                                // Saving last shown new turn.
                                pref.edit().putFloat(LAST_TURN, lastShownNewTurn).commit();

                                /// If we actually put a notification, then kill the background updater-service - it won't be needed now until the user decides what to do with the notification.
                                Printer.out("Killing background updater service since notification has been pushed.");
                                Intent intent = new Intent(context, BackgroundUpdateService.class);
                                intent.putExtra(BackgroundUpdateService.REQUEST_TYPE, BackgroundUpdateService.STOP_SERVICE);
                                context.startService(intent);

                                if (pref == null)
                                    return;
                                App.GetPreferences().edit().putBoolean(BackgroundUpdateService.notificationPresent, true).commit();
                            }
                        }
                    }
                }
                @Override
                public void OnError(EGPacketError error) { // Ignore errors here for now.
                }
            });
            Send(req, context);
        }
    }
}
