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
    protected String body = ""; 
    protected int version = 0;

    void EGPacket()
    {
    }
    
    /// Build final packet to send.
    String build()
    {
        String built = "VER "+version+"\n"
                +"PT "+type.text+"\n";
        if (type == EGPacketType.Request)
            built += "REQ "+reqt.text+"\n";
        else if (type == EGPacketType.Response)
            built += "RES "+rest.text+"\n";
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
                    pack.body = str.substring(i+1);
                    break;
                }
            }
        }
        return pack;
    };
    
}
