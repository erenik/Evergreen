package erenik.evergreen.common.packet;

import erenik.evergreen.common.Player;
import erenik.util.Byter;

/**
 * Created by Emil on 2017-03-23.
 */
public class EGResponse extends EGPacket {
    public EGResponse() {
        this.type = EGPacketType.Request;
        this.rest = null;
    }
    /// Requests without body, such as pure GETs for info.
    public EGResponse(EGResponseType resType) {
        this.type = EGPacketType.Request;
        this.rest = resType;
    }
    public EGResponse(EGResponseType resType, byte[] body) {
        this.type = EGPacketType.Response;
        this.rest = resType;
        this.body = body;
    }

    public static EGResponse numLogMessages(int num) {
        EGResponse eg = new EGResponse(EGResponseType.NumLogMessages, Byter.toByteArray(num));
        return eg;
    };

    public static EGPacket clientPlayerData(Player playerInSystem) {
        EGResponse eg = new EGResponse(EGResponseType.PlayerClientData, Byter.toByteArray(playerInSystem.GetClientData()));
        return eg;
    }


}
