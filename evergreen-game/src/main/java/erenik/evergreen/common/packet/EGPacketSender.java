package erenik.evergreen.common.packet;

import erenik.util.EList;
import erenik.util.EList;
import erenik.util.Printer;

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
    private EList<EGPacket> packetsToReceiveUpdates = new EList<>();
    public void QueuePacket(EGPacket pack, String ip, int port) {
        pack.SetDest(ip, port); // Set dest.
        pack.sendTimeMs = System.currentTimeMillis(); // Set it to now.
        packetsToSend.add(pack); // Add to list.
        // TODO: Make sure thread is started?
        assert(threadStarted);
        packetsToReceiveUpdates.add(pack);
    //    StartSingleton(); // Start thread sender as needed.
    }
    /// Checks for updates and informs listeners on old packets. Returns number of packets left to receive updates.
    public int CheckForUpdates(){
        long now = System.currentTimeMillis();
        for (int i = 0; i < packetsToReceiveUpdates.size(); ++i){
            EGPacket pack = packetsToReceiveUpdates.get(i);
           // Printer.out("Send time: "+pack.sendTimeMs +" diff: "+(now - pack.sendTimeMs));
            if (now - pack.sendTimeMs > 10000){ // Allow 10 seconds before deeming it a failure.
                if (pack.replies.size() > 0){
                    pack.error = EGPacketError.NoError; // If we have at least 1 reply, consider it a success.
                }
                else {
                    pack.error = EGPacketError.NoResponse; // discard it anyway?
                    pack.error.extraText = "Req: "+pack.ReqType().name();
                }
            }
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
    private EList<EGPacket> packetsToSend = new EList<EGPacket>();
    void Log(String s) {
        // Do nothing, later do stuff maybe?
    }

    public void run() {
        stop = false;
        threadStarted = true;
        Log("EGPacketSender.run: Starting EGPacketSender thread.");
        int multiplier = 1;
        int packsFailed = 0;
        int packsSend = 0;
        while(stop == false) {
            try {
                for (int i = 0; i < packetsToSend.size(); ++i) {
                    EGPacket pack = packetsToSend.get(i);
                    boolean ok = pack.Send(); // Just remove it no matter if it succeeded or failed.
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
                    stop = true; // End the thread whenever
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

