package evergreen.common.packet;

/**
 * Created by Emil on 2016-12-18.
 */

public interface EGPacketReceiverListener {
    void OnReceivedReply(EGPacket reply);
    void OnError(EGPacketError error);
}
