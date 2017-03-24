/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.common.packet;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.player.AAction;
import erenik.util.Byter;

/**
 * EvergreenRequest
 * @author Emil
 */
public class EGRequest extends EGPacket {

    /// Requests without body, such as pure GETs for info.
    protected EGRequest() {
        this.type = EGPacketType.Request;
        this.reqt = null;
    }
    /// Requests without body, such as pure GETs for info.
    public EGRequest(EGRequestType reqType) {
        this.type = EGPacketType.Request;
        this.reqt = reqType;
    }
    public EGRequest(EGRequestType reqType, byte[] body) {
        this.type = EGPacketType.Request;
        this.reqt = reqType;
        this.body = body;
    }
    public static EGRequest Load(Player player) {
        player.sendAll = Player.CREDENTIALS_ONLY; // Only credentials.
        EGRequest eg = new EGRequest(EGRequestType.Load, player.toByteArr());
        return eg;
    };
    /// E-mail and encrypted password.
    public static EGPacket LoadCharacters(String email, String encPw) {
        // Separate by a space?
        String total = email+"\n"+encPw;
        EGRequest eg = new EGRequest(EGRequestType.LoadCharacters, total.getBytes(defaultCharset));
        return eg;
    }
    public static EGRequest Save(Player player) {
        player.sendLogs = Player.SEND_CLIENT_SEEN_MESSAGES;
        System.out.println("EGRequest.Save, SEND_CLIENT_SEEN_MESSAGES");
        EGRequest eg = new EGRequest(EGRequestType.Save, player.toByteArr());
        return eg;
    };
    public static EGRequest byType(EGRequestType type) {
        EGRequest eg = new EGRequest(type);
        return eg;
    }
    public static EGRequest PerformActiveActions(Player player){
        player.sendLogs = Player.SEND_CLIENT_SEEN_MESSAGES;
        EGRequest eg = new EGRequest(EGRequestType.ActiveActions, player.toByteArr());
        return eg;
    }

    public static EGPacket CreatePlayer(Player player) {
        return new EGRequest(EGRequestType.CreatePlayer, player.toByteArr());
    }
    public static EGPacket RestartSameCharacter(Player player){
        player.sendLogs = Player.SEND_NO_LOG_MESSAGES;
        return new EGRequest(EGRequestType.RestartSameCharacter, player.toByteArr());
    }

    public static EGPacket FetchWholeLog(Player player) {
        player.sendAll = Player.CREDENTIALS_ONLY;
        byte[] bytes = player.toByteArr();
        player.sendAll = Player.SEND_ALL;
        return new EGRequest(EGRequestType.FetchWholeLog, bytes);
    }

    public static EGPacket LogLength(Player player) {
        player.sendAll = Player.CREDENTIALS_ONLY;
        byte[] bytes = player.toByteArr();
        player.sendAll = Player.SEND_ALL;
        return new EGRequest(EGRequestType.LogLength, bytes);
    }

    public static class ExtraArgs implements Serializable {
        public Player player; // The player object used for authentication/verifying.
        /// Used for FetchLogs
        public int startIndex;
        public int num;
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.writeObject(player);
            out.writeInt(startIndex);
            out.writeInt(num);
        }
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            player = (Player) in.readObject();
            startIndex = in.readInt();
            num = in.readInt();
        }
        private void readObjectNoData() throws ObjectStreamException {}
    }

    public static EGPacket FetchLog(Player player, int startIndex, int num) {
        ExtraArgs fla = new ExtraArgs();
        fla.player = player;
        fla.startIndex = startIndex;
        fla.num = num;
        player.sendAll = Player.CREDENTIALS_ONLY;
        byte[] bytes = Byter.toByteArray(fla);
        player.sendAll = Player.SEND_ALL;
        return new EGRequest(EGRequestType.FetchLog, bytes);
}

    public ExtraArgs parseExtraArgs(){
        ExtraArgs fla = (ExtraArgs) Byter.toObject(body);
        return fla;
    }

}
