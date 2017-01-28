package erenik.evergreen.common.packet;

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
    public EGPacketCommunicator(String ip, int port)
    {
        this.ip = ip;
        this.port = port;
    }

    public void Send(EGPacket packet)
    {
        sender.QueuePacket(packet, ip, port);
        if (sender.isAlive() == false) {
            sender.start();
            System.out.println("Starting packetSender thread");
        }

        // Start the receiver again as well.
        if (receiver != null)
            receiver.NewPacketWaitingForResponse(packet);
        if (receiver == null || receiver.isAlive() == false)
        {
            System.out.println("Starting packet receiver thread");
            receiver = new EGPacketReceiver();
            receiver.NewPacketWaitingForResponse(packet);
            receiver.start();
        }
    }
}
