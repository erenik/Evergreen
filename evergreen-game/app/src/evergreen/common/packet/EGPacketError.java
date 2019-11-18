package evergreen.common.packet;

/**
 * Created by Emil on 2017-03-15.
 */

public enum EGPacketError {
    NoError, // OK. The rest are not OK lol.
    CouldNotEstablishConnection,
    ConnectionTimedOut,
    NoResponse,
    BadVersion,;

    public String extraText = "";
}
