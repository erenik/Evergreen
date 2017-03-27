/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.common.packet;

import erenik.util.EList;
import erenik.util.EList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Emil
 */
public class EGPacketReceiver extends Thread
{
    public EGPacketReceiver()
    {
    }
    void NewPacketWaitingForResponse(EGPacket pack) {
        packetsWaitingForReponses.add(pack);
        packetsToBeUpdated.add(pack);
    }

    boolean stop = false;
   // private EGPacketReceiver epr;
    private EList<EGPacket> packetsWaitingForReponses = new EList<EGPacket>();
    private EList<EGPacket> packetsToBeUpdated = new EList<>();

    void Log(String s) {
        // Do nothing for the moment. Pass it onto a file or listener later perhaps.
    }
    public void run()
    {
        Log("EGPacketReceiver.run: Starting EGPacketReceiver thread.");
        int multiplier = 1;
        while(stop == false) {
            try {
                Thread.sleep(10);
                if (packetsWaitingForReponses.size() == 0)
                    Thread.sleep(10);
                for (int i = 0; i < packetsWaitingForReponses.size(); ++i) {
                  //  System.out.println("Waiting for response...");
                    EGPacket pack = packetsWaitingForReponses.get(i);
                    pack.CheckForReply();
                    boolean remove = false;
                    if (pack.lastError != EGResponseType.NoError){
                        remove = true;
                        Log("EGPacketReceiver.run: An error occurred: "+pack.lastError.text);
                    }
                    long now = System.currentTimeMillis();
                    long diff = now - pack.lastAttemptSystemMillis;
                    pack.lastAttemptSystemMillis = System.currentTimeMillis();
                    pack.timeWaitedForReplyMs += diff;
                    // Wait time out?
//                    System.out.println("pack.timeWaited "+pack.timeWaitedForReplyMs+" timeout "+pack.replyTimeout);
                    if (pack.timeWaitedForReplyMs > pack.replyTimeout){
                        pack.lastError = EGResponseType.ReplyTimeoutReached;
                        remove = true;
                        Log("Timeout reached. Remove packet from queue");
                    }
                    // Reply received,
                    if (pack.replies.size() > 0){
                        //         System.out.println("Got a reply?");
                        // Check last reply time?
                        long msAgo = System.currentTimeMillis() - pack.LastReply().receiveTime;
                        if (msAgo > 3000){ // Wait at most 1 second more for each packet received on the socket.
                           // System.out.println("Last response was "+msAgo+"ms ago, removing this packet now from the listener.");
                            remove = true;
                        }
                        else { // Wait some more if we already had replies...
                            remove = false;
                        }
                    }
                    if (remove) {
                        packetsWaitingForReponses.remove(pack); // Remove from receiving queue.
//                        System.out.println("Removing packet from receiver queue.");
                        --i;
                        continue;
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(EGPacketReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Log("Stopping EGPacketSender thread.");
 //       epr = null; // Kill self. Allow restart of the thread.
    }

    /// Does what exactly?
    public int CheckForUpdates() {
        for (int i = 0; i < packetsToBeUpdated.size(); ++i){
            EGPacket pack = packetsToBeUpdated.get(i);
            for (int j = 0; j < pack.replies.size(); ++j){
                EGPacket reply = pack.replies.get(j);
                if (!reply.informedListeners) {
                    pack.InformListenersOnReply(reply);
                    reply.informedListeners = true;
                }
            }
            if (pack.sendTime > System.currentTimeMillis() + 3000) { // Wait at most 10 seconds for all replies from a specific packet, then discard it from the array to be checked further.
                packetsToBeUpdated.remove(pack);
                --i;
            }
        }
        return packetsToBeUpdated.size();
    }
};
