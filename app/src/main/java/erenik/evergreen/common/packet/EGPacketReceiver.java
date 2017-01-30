/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.common.packet;

import java.util.ArrayList;
import java.util.List;
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
    /*
    public static void StartSingleton()
    {
        if (epr != null)
            return;
        epr = new EGPacketReceiver();
        epr.start();
    }
    static void StopSingleton()
    {
        epr.stop = true;
    }
    */
    void NewPacketWaitingForResponse(EGPacket pack) {
        packetsWaitingForReponses.add(pack);
    }
//    public static boolean HasPacketsToReceive(){
  //      return packetsWaitingForReponses.size() > 0;
    //}

    boolean stop = false;
   // private EGPacketReceiver epr;
    private List<EGPacket> packetsWaitingForReponses = new ArrayList<EGPacket>();

    void Log(String s) {
        // Do nothing for the moment. Pass it onto a file or listener later perhaps.
    }
    public void run()
    {
        Log("EGPacketReceiver.run: Starting EGPacketReceiver thread.");
        int multiplier = 1;
        while(stop == false)
        {
            try {
                if (packetsWaitingForReponses.size() == 0)
                    Thread.sleep(1000);
                else
                {
                    int sleepTime = EGPacketCommunicator.retryTimeMs * multiplier;
                    ++multiplier;
                    Log("EGPacketReceiver sleeping for "+sleepTime+"ms");
                    Thread.sleep(sleepTime);
                }
                for (int i = 0; i < packetsWaitingForReponses.size(); ++i)
                {
                    EGPacket pack = packetsWaitingForReponses.get(i);
                    pack.CheckForReply();
                    boolean remove = false;
                    // Reply received,
                    if (pack.reply != null){
               //         System.out.println("Got a reply?");
                        remove = true;
                    }
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
                    if (remove) {
                        packetsWaitingForReponses.remove(pack); // Remove from receiving queue.
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
};
