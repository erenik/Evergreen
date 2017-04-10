package erenik.evergreen.common.packet;

import erenik.util.Printer;

/** Communication class for sending/receiving. Handles threads for both, as well as listeners for callbacks.
 * Created by Emil on 2016-12-11.
 */
public class EGPacketCommunicator {
    public static int retryTimeMs = 250;
    EGPacketReceiver receiver = null;
    EGPacketSender sender = new EGPacketSender();

    // Where to send stuff to.
    String ip = "127.0.0.1";
    int port = 4000;
    public EGPacketCommunicator(){}
    public EGPacketCommunicator(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void Send(EGPacket packet)
    {
        sender.QueuePacket(packet, ip, port);
        if (sender.isAlive() == false) {
            sender.start();
            Printer.out("Starting packetSender thread");
        }

        // Start the receiver again as well.
        if (receiver != null)
            receiver.NewPacketWaitingForResponse(packet);
        if (receiver == null || receiver.isAlive() == false)
        {
            Printer.out("Starting packet receiver thread");
            receiver = new EGPacketReceiver();
            receiver.NewPacketWaitingForResponse(packet);
            receiver.start();
        }
    }
    /// Sets the IP stuff should be sent to by default for requests.
    public void SetServerIP(String defaultAddress) {
        this.ip = defaultAddress;
    }
    /// Returns non-0 if there are more updates to be had later.
    public int CheckForUpdates() {
        int updatesToGet = sender.CheckForUpdates();
        updatesToGet += receiver.CheckForUpdates();
        return updatesToGet;
    }
}
