package evergreen.common.packet;

import evergreen.common.Player;
import evergreen.util.Byter;
import evergreen.util.Printer;

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
    public static EGPacket TurnsSurvived(float turns) {
        float turnsSurvived = turns;
        EGResponse eg = new EGResponse(EGResponseType.TurnsSurvived, Byter.toByteArray(turns));
        float turnsRead = (float) Byter.toObject(eg.body);
     //   Printer.out("Turns written: "+turns+" read: "+turnsRead);
        if (turns != turnsRead) {
            new Exception().printStackTrace();
            System.exit(15);
        }
        return eg;
    }
}
