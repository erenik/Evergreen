/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static server.EGTCPServer.charBuffer;

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
    protected EGSocket socketSentOn; // When using EGPacketSender class to retrieve replies when available.
    protected EGPacket reply;
    protected int replyTimeout = 3000; // Default wait up to 3 seconds for a reply?
    protected int timeWaitedForReply = 0;
    static EGPacketReceiver packetReceiver = null;
    EGErrorType lastError = EGErrorType.NoError; // Default no errors, right?
    
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
        str += " body.length: "+body.length;
        str += " }";
        return str;
    }
    static EGPacket ok() {
        return new EGPacket(EGResponseType.OK);
    }
    static EGPacket error(EGErrorType errorType) 
    {
        return new EGPacket(errorType);
    }
    static EGPacket parseError() {
        return new EGPacket(EGErrorType.ParseError);
    }
    
    /// Build final packet contents to send.
    String build()
    {
        String built = "VER "+version+"\n"
                +"PT "+type.text+"\n";
        switch(type)
        {
            case Error: built += "ERR "+errType.text+"\n"; return built;
            case Request: built += "REQ "+reqt.text+"\n"; break;
            case Response: built += "RES "+rest.text+"\n"; break;
        }
        String converted = new String(body);
 //       System.out.println("converted: "+converted);
        built += converted;
        return built;
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
                    System.out.println("VER "+pack.version+" "+val);
                }
                if (key.equals("PT")) {
                    pack.type = EGPacketType.fromString(val);
                    System.out.println("PT "+pack.type+" "+val);
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
    /// Sends this packet without waiting for a reponse. The response will be set later in the EGPacket reponse variable, or errors in lastError if responses don't arrive.
    EGPacket Send(String address, int portN) 
    {
        String total = build();
        PrintWriter out = null;
        try {
            socketSentOn = new EGSocket(address, portN);
            out = socketSentOn.getPrintWriter();
            out.print(total); // Print the string.
            out.flush();
            EGPacketReceiver.NewPacketWaitingForResponse(this);
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
    
    /// Reads from socket. Returns null if any error occurs.
    EGPacket ReadFromSocket(EGSocket sock)
    {
        try {
            BufferedReader in = null;
            // Wait for a reponse.
            if (sock.isClosed())
            {
                System.out.println("Socket closed");
                lastError = EGErrorType.SocketClosed;
                return null;
            }
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            if (!in.ready())
            {
                System.out.println("Buffered reader not ready");
                return null;
            }
            
            final int BUF_LEN = 4000;
            char[] charBuffer = new char[BUF_LEN];
            int bytesRead = in.read(charBuffer, 0, BUF_LEN);
            if (bytesRead <= 0){
                System.out.println("0 bytes read from stream.");
                return null;
            }
            String inText = String.copyValueOf(charBuffer, 0, bytesRead);
            EGPacket pack = EGPacket.packetFromString(inText);
            return pack;
        } catch (IOException ex) {
            Logger.getLogger(EGPacket.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    void CheckForReply()
    {
        if (socketSentOn == null)
            return;
        reply = ReadFromSocket(socketSentOn);
    }

    void PrintBody() 
    {
        System.out.print("EGPacket Body: ");
        for (int i = 0; i < body.length; ++i)
            System.out.print(body[i]);
        System.out.println();
    }

    
}
