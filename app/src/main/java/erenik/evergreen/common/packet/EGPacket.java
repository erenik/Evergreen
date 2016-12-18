/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.common.packet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import erenik.evergreen.server.EGTCPServer;
import java.io.InputStream;
import java.nio.ByteBuffer;

// import static server.EGTCPServer.charBuffer;

/**
 *
 * @author Emil
 */
public class EGPacket 
{
    protected EGPacketType type = null;
    protected EGRequestType reqt = null;
    protected EGResponseType rest = null;
    protected EGErrorType errType = null;
    protected byte[] body = "".getBytes(); 
    protected int version = 0;
    int headerLen, bodyLen, totalLen; // Calculated after calling .build()
    protected EGSocket socketSentOn; // When using EGPacketSender class to retrieve replies when available.
    protected EGPacket reply;
    protected int replyTimeout = 3000; // Default wait up to 3 seconds for a reply?
    protected int timeWaitedForReply = 0;
    static EGPacketReceiver packetReceiver = null;
    EGErrorType lastError = EGErrorType.NoError; // Default no errors, right?

    public EGErrorType LastError(){return lastError;};
    public EGPacket GetReply(){return reply;};
    public EGPacketType Type() {return type; };
    public EGRequestType ReqType() { return reqt; };
    public EGResponseType ResType() { return rest; };
    public EGErrorType ErrType() { return errType; };
    public byte[] GetBody() { return body; };

    /// If using Send() function, call .SetDest(ip, port) first.
    private String ip;
    private int port;

    EGPacket()
    {
    }    
    EGPacket(EGResponseType resType)
    {
        type = EGPacketType.Response;
        rest = resType;
    }
    EGPacket(EGRequestType reqType)
    {
        type = EGPacketType.Request;
        reqt = reqType;
    }
    EGPacket(EGErrorType errorType)
    {
        type = EGPacketType.Error;
        errType = errorType;
    };
    @Override
    public String toString()
    {
        String str = "EGPacket{";
        if (type != null) str += " type: "+type.text;
        if (reqt != null) str += " reqt: "+reqt.text;
        if (rest != null) str += " rest: "+rest.text;
        if (errType != null) str += " errType: "+errType.text;
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
    public static EGPacket error(EGErrorType errorType)
    {
        return new EGPacket(errorType);
    }
    public static EGPacket parseError() {
        return new EGPacket(EGErrorType.ParseError);
    }
    static byte[] bytePart(byte[] bytes, int startIndex, int stopIndex)
    {
        int len = stopIndex - startIndex;
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
            case Error: header += "ERR "+errType.text+"\n"; return header.getBytes();
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
        System.out.print("Tail bytes: ");
        tailBytes(bytes, 10);
        return bytes;
    }
    public static EGPacket packetFromBytes(byte[] arr)
    {
        System.out.println("packetFromBytes arrLen: "+arr.length);
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
                if (key.equals("ERR"))
                {
   //                 System.out.println("ERR "+pack.errType+" "+val);
                    pack.errType = EGErrorType.fromString(val);
                }
                if (key.equals("LEN"))
                {
                    bodyLength = Integer.parseInt(val);
             //       System.out.println("bodyLength: "+bodyLength);
                }
                System.out.println("key: "+key+" value: "+val);
                ++argN;
                if (argN >= 4)
                {
                    if (bodyLength > 0){
                        int bodyStart = i + 1;
                        pack.body = bytePart(arr, bodyStart, bodyStart + bodyLength);
                        System.out.print("Tail bytes of parsed body: ");
                        tailBytes(pack.body, 10);
                    }
                    break;
                }
            }
        }
        System.out.println("argN found: "+argN);
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
                if (key.equals("ERR"))
                {
                    pack.errType = EGErrorType.fromString(val);
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
    public boolean Send()
    {
        return Send(ip, port);
    }

    /// Sends this packet without waiting for a reponse. The response will be set later in the EGPacket reponse variable, or errors in lastError if responses don't arrive.
    public boolean Send(String address, int portN)
    {
        byte[] total = build();
        try {
            socketSentOn = new EGSocket(address, portN);
            OutputStream out = socketSentOn.getOutputStream();
            out.write(total); // Print the string.
            out.flush();
            EGPacketReceiver.NewPacketWaitingForResponse(this);
            return true; // Success. Sent well.
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, "Unable to connect to target address/port: "+address+":"+portN, ex);
        }
        return false;
    }
    
    /// Reads from socket. Returns null if any error occurs.
    public EGPacket ReadFromSocket(EGSocket sock)
    {
        try {
            System.out.println("Read from socket");
            // Wait for a reponse.
            if (sock.isClosed())
            {
                System.out.println("Socket closed");
                lastError = EGErrorType.SocketClosed;
                return null;
            }
            InputStream is = sock.getInputStream();
            int bytesAvail = is.available();
            if (bytesAvail <= 0)
            {
                return null;
            }
            System.out.println("Reading from socket!!!!");
            
            final int BUF_LEN = 40000;
            byte[] buff = new byte[BUF_LEN];
            int bytesRead = is.read(buff, 0, BUF_LEN);
            System.out.println("Bytes avail: "+bytesAvail+" bytesRead: "+bytesRead);
            if (bytesRead <= 0){
                System.out.println("0 bytes read from stream.");
                return null;
            }
            EGPacket pack = EGPacket.packetFromBytes(buff);
            System.out.println("Got packet: "+pack);
            return pack;
        } catch (IOException ex) {
            Logger.getLogger(EGPacket.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    void CheckForReply()
    {
        if (socketSentOn == null){
            return;
        }
        reply = ReadFromSocket(socketSentOn);
    }

    
    public void PrintBody()
    {
        System.out.print("EGPacket Body: ");
        for (int i = 0; i < body.length; ++i)
            System.out.print(body[i]);
        System.out.println();
    }

    /** Waits, blocking in 10 ms intervals, until either the packet response has been received
     *  or until the timeout period is reached.
     */
    public void WaitForResponse(int milliseconds) {
        // Return response?
        boolean waitingForResponse = true;
        int msToWait = milliseconds;
        while(waitingForResponse && msToWait > 0)
        {
            switch(lastError)
            {
                case ReplyTimeoutReached:
                    waitingForResponse = false;
                    break;
            }
            if (reply != null)
                break;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            msToWait -= 10;
        }
    }
}
