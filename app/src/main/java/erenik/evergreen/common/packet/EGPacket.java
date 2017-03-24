/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import erenik.evergreen.Game;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.player.ClientData;
import erenik.evergreen.server.EGTCPServer;
import erenik.util.Byter;

import java.io.InputStream;
import java.nio.ByteBuffer;

// import static server.EGTCPServer.charBuffer;

/**
 *
 * @author Emil
 */
public class EGPacket {
    /// Used for encoding/decoding String objects into bytes and back.
    public static final Charset defaultCharset = StandardCharsets.UTF_8;
    public static final int BUF_LEN = 40000;

    protected EGPacketType type = null;
    protected EGRequestType reqt = null;
    protected EGResponseType rest = null;
    protected byte[] body = "".getBytes();
    protected int version = 0;
    int headerLen, bodyLen, totalLen; // Calculated after calling .build()
    protected EGSocket socketSentOn; // When using EGPacketSender class to retrieve replies when available.
//    protected EGPacket reply; // Replies received.
    protected ArrayList<EGPacket> replies = new ArrayList<>();
    protected int replyTimeout = 3000; // Default wait up to 3 seconds for a reply?
    protected int timeWaitedForReplyMs = 0;

    /// Receiver which should be added/set before sending packet.
    EGResponseType lastError = EGResponseType.NoError; // Default no errors, right?
    /// List of receiving listeners, to interpret any response that is received..?
    private List<EGPacketReceiverListener> receiverListeners = new ArrayList<>();
    public long lastAttemptSystemMillis = System.currentTimeMillis();
    public long receiveTime = 0; // Sys curr Millis.
    public long sendTime = 0;
    public boolean informedListeners = false;

    public EGResponseType LastError(){return lastError;};
    public EGPacket GetReply(){return replies.get(0);};
    public EGPacketType Type() {return type; };
    public EGRequestType ReqType() { return reqt; };
    public EGResponseType ResType() { return rest; };
    public EGResponseType ErrType() { return rest; };
    public byte[] GetBody() { return body; };

    /// If using Send() function, call .SetDest(ip, port) first.
    private String ip;
    private int port;
    /// Null until set!
    EGPacketError error;

    EGPacket() {
    }    
    EGPacket(EGResponseType resType) {
        type = EGPacketType.Response;
        rest = resType;
    }
    EGPacket(EGRequestType reqType) {
        type = EGPacketType.Request;
        reqt = reqType;
    }
    public void addReceiverListener(EGPacketReceiverListener eprl) {
        receiverListeners.add(eprl);
    }

    @Override
    public String toString()
    {
        String str = "EGPacket{";
        if (type != null) str += " type: "+type.text;
        if (reqt != null) str += " reqt: "+reqt.text;
        if (rest != null) str += " rest: "+rest.text;
        if (headerLen > 0)
            str += " headerLen: "+headerLen;
        str += " body.length: "+body.length;
        if (totalLen > 0)
            str += " totalLen: "+totalLen;
        str += " }";
        return str;
    }
    public static EGPacket ok() {
        return new EGPacket(EGResponseType.OK);
    }
    public static EGPacket error(EGResponseType errorType)
    {
        return new EGPacket(errorType);
    }
    public static EGPacket parseError() {
        return new EGPacket(EGResponseType.ParseError);
    }
    public static EGPacket player(Player playerInSystem) { // Pack with info on 1 player. Sent as reply for Load packets.
        EGPacket pack = new EGPacket(EGResponseType.Player);
        playerInSystem.sendLogs = Player.SEND_SERVER_NEW_MESSAGES; // Send only 1 day of data. Player can always request more log data later as needed/wanted.
        pack.body = playerInSystem.toByteArr();
        return pack;
    }
    public static EGPacket logMessages(List<Log> logMessages){
        ArrayList<Log> lm = new ArrayList();
        lm.addAll(logMessages);
        EGPacket pack = new EGPacket(EGResponseType.LogMessages);
        pack.body = Byter.toByteArray(lm);
        if (pack.body == null)
            return null;
        return pack;
    }
    public static EGPacket players(ArrayList<Player> players){
        EGPacket pack = new EGPacket(EGResponseType.Player);
        pack.type = EGPacketType.Response;
        pack.rest = EGResponseType.Players;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(baos);
            oos.writeObject(players);
            oos.close();
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        pack.body = baos.toByteArray();
        System.out.println("Total bytes: "+pack.body.length);
        return pack;
    }
    // Parse players from the body.
    public ArrayList<Player> parsePlayers(){
        // First byte, write amount of
        ByteArrayInputStream in = new ByteArrayInputStream(body);
        ArrayList<Player> players = new ArrayList<>();
        try {
            ObjectInputStream ois = null;
            ois = new ObjectInputStream(in);
            players = (ArrayList<Player>) ois.readObject();
            ois.close();
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Read players: "+players.size());
        return players;
    }

    static byte[] bytePart(byte[] bytes, int startIndex, int stopIndex) throws Exception {
        int len = stopIndex - startIndex;
        if (stopIndex > bytes.length)
            throw new Exception("Will read outside array, bad operation while builting packet.");
        byte[] b = new byte[stopIndex - startIndex];
        for (int i = 0; i < len; ++i)
            b[i] = bytes[i+startIndex];
        return b;
    }
    static void mv(byte[] bytes, byte[] intoArray, int atIndex)
    {
        for (int i = 0; i < bytes.length; ++i)
            intoArray[i+atIndex] = bytes[i];
    }
    static void tailBytes(byte[] bytes, int numTail)
    {
        for (int i = bytes.length - numTail; i < bytes.length; ++i)
        {
            System.out.print(" "+(int)bytes[i]);
        }
        System.out.println("");
    }
    /// Build final packet contents to send.
    public byte[] build()
    {
        ByteBuffer bb;
        //bytep[]
        String header = "VER "+version+"\n"
                +"PT "+type.text+"\n";
        switch(type)
        {
            case Request: header += "REQ "+reqt.text+"\n"; break;
            case Response: header += "RES "+rest.text+"\n"; break;
        }
        bodyLen = body != null? body.length : 0;
        header += "LEN "+bodyLen+"\n"; // Add Body length to the header where it is relevant.
        headerLen = header.getBytes().length;
        totalLen = headerLen + bodyLen;
        byte[] bytes = new byte[headerLen + bodyLen];
        mv(header.getBytes(), bytes, 0); // Copy in header.
        if (body != null)
            mv(body, bytes, header.getBytes().length); // And body.
     //   System.out.print("Tail bytes: ");
//        tailBytes(bytes, 10);
        return bytes;
    }
    public static EGPacket packetFromBytes(byte[] arr) {
//        System.out.println("packetFromBytes arrLen: "+arr.length);
        EGPacket pack = new EGPacket();
        int argN = 0, 
            startIndexKey = 0, 
            startIndexValue = 0;
        String key = "", val = "";
        int bodyLength = -1;
        for (int i = 0; i < arr.length; ++i)
        {
            byte c = arr[i];
            if (c == ' '){
                startIndexValue = i + 1;
                key = new String(arr, startIndexKey, i - startIndexKey);
            //    System.out.println("Key: "+key);
            }
            if (c == '\n')
            {
                val = new String(arr, startIndexValue, i - startIndexValue);
                val = val.trim();
                startIndexKey = i + 1;
//                System.out.println("Val: "+val);
                if (key.equals("VER"))
                {
                    pack.version = Integer.parseInt(val);
           //         System.out.println("VER "+pack.version+" "+val);
                }
                if (key.equals("PT")) {
                    pack.type = EGPacketType.fromString(val);
                    EGPacket newPack = null;
                    switch (pack.type){
                        case Request: newPack = new EGRequest(); break;
                        case Response: newPack = new EGResponse(); break;
                    }
                    // Copy over the data so far.
                    newPack.version = pack.version;
                    newPack.type = pack.type;
                    newPack.body = pack.body;
                    newPack.bodyLen = pack.bodyLen;
                    pack = newPack;
         //           System.out.println("PT "+pack.type+" "+val);
                }
                if (key.equals("REQ")) {
                    pack.reqt = EGRequestType.fromString(val);
       //             System.out.println("REQ "+pack.reqt+" "+val);
                }
                if (key.equals("RES")) {
                    pack.rest = EGResponseType.fromString(val);
     //               System.out.println("RES "+pack.rest+" "+val);
                }
                if (key.equals("LEN"))
                {
                    bodyLength = Integer.parseInt(val);
             //       System.out.println("bodyLength: "+bodyLength);
                }
//                System.out.println("key: "+key+" value: "+val);
                ++argN;
                if (argN >= 4)
                {
                    if (bodyLength > 0){
                        int bodyStart = i + 1;
                        try {
                            pack.body = bytePart(arr, bodyStart, bodyStart + bodyLength);
                        } catch (Exception e) {
                            System.out.println("Error: "+e.getMessage());
                            e.printStackTrace();
                        }
                        //                   System.out.print("Tail bytes of parsed body: ");
       //                 tailBytes(pack.body, 10);
                    }
                    break;
                }
            }
        }
//        System.out.println("argN found: "+argN);
        pack.receiveTime = System.currentTimeMillis(); // Received timestamp.
        if (pack.type != null)
            return pack;
        return null;        
    }
    // Returns null if not decent arguments are given.
    public static EGPacket packetFromString(String str)
    {
        EGPacket pack = new EGPacket();
        int argN = 0, 
            startIndexKey = 0, 
            startIndexValue = 0;
        String key = "", val = "";
        for (int i = 0; i < str.length(); ++i)
        {
            char c = str.charAt(i);
            if (c == ' '){
                startIndexValue = i + 1;
                key = str.substring(startIndexKey, i);
  //              System.out.println("Key: "+key);
            }
            if (c == '\n')
            {
                val = str.substring(startIndexValue, i);
                val = val.trim();
                startIndexKey = i + 1;
//                System.out.println("Val: "+val);
                if (key.equals("VER"))
                {
                    pack.version = Integer.parseInt(val);
//                    System.out.println("VER "+pack.version+" "+val);
                }
                if (key.equals("PT")) {
                    pack.type = EGPacketType.fromString(val);
  //                  System.out.println("PT "+pack.type+" "+val);
                }
                if (key.equals("REQ")) {
                    pack.reqt = EGRequestType.fromString(val);
                }
                if (key.equals("RES")) {
                    pack.rest = EGResponseType.fromString(val);
                }
                ++argN;
                if (argN >= 3)
                {
                    pack.body = str.substring(i+1).getBytes();
                    break;
                }
            }
        }
        if (pack.type != null)
            return pack;
        return null;
    };

    /// If using Send() function, call .SetDest(ip, port) first.
    public void SetDest(String ip, int port)
    {
        this.ip = ip;
        this.port = port;
    }
    /// Sends this packet without waiting for a reponse. The response will be set later in the EGPacket reponse variable, or errors in lastError if responses don't arrive.
    boolean Send() {
        return Send(ip, port);
    }

    /// Sends this packet without waiting for a reponse. The response will be set later in the EGPacket reponse variable, or errors in lastError if responses don't arrive.
    boolean Send(String address, int portN) {
        byte[] total = build();
        try {
            socketSentOn = new EGSocket(address, portN);
            OutputStream out = socketSentOn.getOutputStream();
            out.write(total); // Print the string.
            out.flush();
            sendTime = System.currentTimeMillis();
            return true; // Success. Sent well.
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, "Unable to connect to target address/port: "+address+":"+portN, ex);
            error = EGPacketError.CouldNotEstablishConnection;
        }
        return false;
    }
    
    /// Reads from socket. Returns null if any error occurs.
    public EGPacket ReadFromSocket(EGSocket sock) {
        try {
            if (sock.isClosed()) {
                System.out.println("Socket closed");
                lastError = EGResponseType.SocketClosed;
                return null;
            }
            InputStream is = sock.getInputStream();
            int bytesAvail = is.available();
            if (bytesAvail <= 0) {
                return null;
            }
            byte[] buff = new byte[BUF_LEN];
            int bytesRead = is.read(buff, 0, BUF_LEN);
            if (bytesAvail > (BUF_LEN / 10)) // More than a tenth of max? 4kB? then maybe worth spamming about it...
                System.out.print("\nsocket read bytesAvail: "+bytesAvail+" bytesRead: "+bytesRead);
            if (bytesRead <= 0){
                System.out.println("0 bytes read from stream.");
                return null;
            }
            System.out.println("read "+bytesRead+" bytes");
            EGPacket pack = EGPacket.packetFromBytes(buff);
      //      System.out.println("Got packet: "+pack);
            return pack;
        } catch (IOException ex) {
            Logger.getLogger(EGPacket.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    void CheckForReply() {
        if (socketSentOn == null){
            return;
        }
        EGPacket reply = ReadFromSocket(socketSentOn);
        if (reply != null) {
            System.out.println("Got a reply: " + reply);
            replies.add(reply);
        }
    }

    
    public void PrintBody()
    {
        System.out.print("EGPacket Body: ");
        for (int i = 0; i < body.length; ++i)
            System.out.print(body[i]);
        System.out.println();
    }

    public static EGPacket gamesList(List<Game> games)
    {
        EGPacket pack = new EGPacket(EGResponseType.GamesList);
        String bodyStr = "NumGames:"+games.size()+"\n";
        for (int i = 0; i < games.size(); ++i)
        {
            Game g = games.get(i);
            bodyStr += g.toJsonBrief() +"\n";
        }
        pack.body = bodyStr.getBytes();
        return pack;
    }
    public List<Game> parseGamesList()
    {
        String bodyAsStr = new String(body);
        System.out.println("bodyASStr: "+bodyAsStr);
        String[] lines = bodyAsStr.split("\n");
        List<Game> games = new ArrayList<>();
        for (int i = 0; i < lines.length; ++i)
        {
            String line = lines[i];
            Game g = new Game();
            boolean ok = g.parseFromJson(line);
            if (!ok)
                continue;
            games.add(g);
        }
        return games;
    }
    void InformListenersOnError() {
        for (int i = 0; i < receiverListeners.size(); ++i){
            receiverListeners.get(i).OnError(error);
        }
    }
    public void InformListenersOnReply(EGPacket reply) {
        // Notify listeners of the reply we received..?
        for (int i = 0; i < receiverListeners.size(); ++i)
            receiverListeners.get(i).OnReceivedReply(reply);
    }
    // Assuming the body contains only 1 player's worth of data...
    public Player GetPlayer() throws Exception {
        return Player.fromByteArray(GetBody());
    }

    public EGPacket LastReply() {
        return replies.get(replies.size()-1);
    }

    public ClientData GetClientData() {
        return (ClientData) Byter.toObject(body);
    }
}
