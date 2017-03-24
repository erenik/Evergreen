package erenik.evergreen.common.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Emil on 2016-12-11.
 */

public class EGPacketSender extends Thread {

    public EGPacketSender()
    {
    }
    // Packets to receive updates - success? Error? Inform listeners and remove from array afterwards
    private List<EGPacket> packetsToReceiveUpdates = new ArrayList<>();
    public void QueuePacket(EGPacket pack, String ip, int port) {
        pack.SetDest(ip, port); // Set dest.
        packetsToSend.add(pack); // Add to list.
        // TODO: Make sure thread is started?
        assert(threadStarted);
        packetsToReceiveUpdates.add(pack);
    //    StartSingleton(); // Start thread sender as needed.
    }
    /// Checks for updates and informs listeners on old packets. Returns number of packets left to receive updates.
    public int CheckForUpdates(){
        for (int i = 0; i < packetsToReceiveUpdates.size(); ++i){
            EGPacket pack = packetsToReceiveUpdates.get(i);
            if (pack.error == null)
                continue;
            boolean good = false;
            if (pack.error == EGPacketError.NoError)
                good = true;
            if (!good)
                pack.InformListenersOnError();
            packetsToReceiveUpdates.remove(pack);
            --i;
        }
        return packetsToReceiveUpdates.size();
    }

    boolean stop = false;
    boolean threadStarted = false;
  //  private EGPacketSender eps;
    private List<EGPacket> packetsToSend = new ArrayList<EGPacket>();
    void Log(String s) {
        // Do nothing, later do stuff maybe?
    }

    public void run() {
        threadStarted = true;
        Log("EGPacketSender.run: Starting EGPacketSender thread.");
        int multiplier = 1;
        int packsFailed = 0;
        int packsSend = 0;
        while(stop == false) {
            try {
                for (int i = 0; i < packetsToSend.size(); ++i) {
                    EGPacket pack = packetsToSend.get(i);
                    boolean ok = pack.Send();
                    boolean remove = true;
                    if (ok) {
                        ++packsSend;
                    }
                    else {
                        ++packsFailed;
                    }
                    if (remove) {
                        packetsToSend.remove(pack); // Remove from receiving queue.
                        --i;
                        continue;
                    }
                }
                if (packsSend > 0)
                    Log("sent "+packsSend+" packs, failed: "+packsFailed);
                if (packetsToSend.size() == 0)
                    Thread.sleep(10);
                else
                    Thread.sleep(EGPacketCommunicator.retryTimeMs * multiplier);
            } catch (InterruptedException ex) {
                Logger.getLogger(EGPacketReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Log("Stopping EGPacketReceiver thread.");
   //     eps = null; // Kill self. Allow restart of the thread.
    }
};

