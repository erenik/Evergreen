package erenik.evergreen.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Emil on 2016-12-11.
 */

public class EGPacketSender extends Thread
{
    private EGPacketSender()
    {
    }
    public static void StartSingleton()
    {
        if (eps != null)
            return;
        eps = new EGPacketSender();
        eps.start();
    }
    static void StopSingleton()
    {
        eps.stop = true;
    }
    public static void QueuePacket(EGPacket pack, String ip, int port)
    {
        pack.SetDest(ip, port); // Set dest.
        packetsToSend.add(pack); // Add to list.
        StartSingleton(); // Start thread sender as needed.
    }

    boolean stop = false;
    static private EGPacketSender eps;
    static private List<EGPacket> packetsToSend = new ArrayList<EGPacket>();
    public void run()
    {
        System.out.println("EGPacketSender.run: Starting EGPacketSender thread.");
        int multiplier = 1;
        while(stop == false)
        {
            try {
                for (int i = 0; i < packetsToSend.size(); ++i)
                {
                    EGPacket pack = packetsToSend.get(i);
                    boolean ok = pack.Send();
                    boolean remove = false;
                    if (ok) {
                        System.out.println("Sent packet: "+pack);
                        remove = true;
                    }
                    else
                    {
                        // Some problem occurred, double wait time.
                        multiplier *= 2;
                    }
                    if (remove) {
                        packetsToSend.remove(pack); // Remove from receiving queue.
                        --i;
                        continue;
                    }
                }
                if (packetsToSend.size() == 0)
                    Thread.sleep(1000);
                else
                    Thread.sleep(EGPacketCommunicator.retryTimeMs * multiplier);
            } catch (InterruptedException ex) {
                Logger.getLogger(EGPacketReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Stopping EGPacketReceiver thread.");
        eps = null; // Kill self. Allow restart of the thread.
    }
};

