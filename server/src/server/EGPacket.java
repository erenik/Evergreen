/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

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
        built += body;
        return built;
    }
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
                startIndexKey = i + 1;
//                System.out.println("Val: "+val);
                if (key.equals("VER")) pack.version = Integer.parseInt(val);
                if (key.equals("PT")) pack.type = EGPacketType.fromString(val);
                if (key.equals("REQ")) pack.reqt = EGRequestType.fromString(val);
                if (key.equals("RES")) pack.rest = EGResponseType.fromString(val);
                ++argN;
                if (argN >= 3)
                {
                    pack.body = str.substring(i+1).getBytes();
                    break;
                }
            }
        }
        return pack;
    };
    
}
