package erenik.evergreen.android.act;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import erenik.evergreen.Player;
import erenik.evergreen.android.App;
import erenik.evergreen.packet.EGPacketSender;
import erenik.evergreen.player.Constants;
import erenik.evergreen.packet.EGPacketReceiver;
import erenik.evergreen.packet.EGRequest;

/**
 * Created by Emil on 2016-12-09.
 */
public class EvergreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set default orientation - Portrait.
        // Store common shit here?
        App.OnActivityCreated(this);  // Setup system callbacks as/if needed.
    }

    /// General Save/Load. Will determine if it needs to perform it locally or remotely.
    boolean Save()
    {
        // For now, save locally and remotely, for testing purposes.
        SaveLocally();
        SaveToServer();
        return true;
    }
    boolean Load()
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

    static String ip = "192.168.38.103";
    static int port = 4000;
    private boolean SaveToServer()
    {
        Player player = App.GetPlayer();
        System.out.println("Saving to server.");
        // Connect to server.
        EGPacketReceiver.StartSingleton();
        EGRequest req = EGRequest.Save(player);
        EGPacketSender.QueuePacket(req, ip, port);
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
        EGPacketReceiver.StartSingleton();
        EGRequest req = EGRequest.Load(player);
        EGPacketSender.QueuePacket(req, ip, port);
        req.WaitForResponse(1000);
        if (req.GetReply() == null)
        {
            System.out.println("Failed to get a reply with current timeout.");
            return false;
        }
        System.out.println("Got reply? : "+req.GetReply().toString());
        return true;
    }

}
