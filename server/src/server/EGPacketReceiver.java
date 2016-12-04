/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Emil
 */
class EGPacketReceiver extends Thread
{
    private EGPacketReceiver()
    {
    }
    static void StartSingleton()
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
    static void NewPacketWaitingForResponse(EGPacket pack)
    {
        packetsWaitingForReponses.add(pack);
    }
    boolean stop = false;
    static private EGPacketReceiver epr;
    static private List<EGPacket> packetsWaitingForReponses = new ArrayList<EGPacket>();
    public void run()
    {
        System.out.println("EGPacketReceiver.run: Starting EGPacketReceiver thread.");
        while(stop == false)
        {
            try {
                if (packetsWaitingForReponses.size() == 0)
                    Thread.sleep(1000);
                else
                    Thread.sleep(10);
                for (int i = 0; i < packetsWaitingForReponses.size(); ++i)
                {
                    EGPacket pack = packetsWaitingForReponses.get(i);
                    pack.CheckForReply();
                    boolean remove = false;
                    // Reply received, 
                    if (pack.reply != null)
                        remove = true;
                    if (pack.lastError != EGErrorType.NoError){
                        remove = true;
                        System.out.println("EGPacketReceiver.run: An error occurred: "+pack.lastError.text);
                    }
                    pack.timeWaitedForReply += 10;
                    // Wait time out?
            //        System.out.println("pack.timeWaited "+pack.timeWaitedForReply+" timeout "+pack.replyTimeout);
                    if (pack.timeWaitedForReply > pack.replyTimeout){
                        pack.lastError = EGErrorType.ReplyTimeoutReached;
                        remove = true;
                    }
                    if (remove)
                    {
                        packetsWaitingForReponses.remove(pack); // Remove from receiving queue.
                        --i;
                        continue;
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(EGPacketReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Stopping EGPacketReceiver thread.");
    }
    
};
