/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package evergreen.common.packet;

/**
 *
 * @author Emil
 */
public enum EGPacketType {
    Request("REQ"), 
    Response("RES"),
    Error("ERR"),
    ;
    EGPacketType(String packetText)
    {
        this.text = packetText;
    }
    static EGPacketType fromString(String fromString)
    {
     //   Printer.out("EGPacketType.fromString");
        for (int i = 0; i < EGPacketType.values().length; ++i)
            if (EGPacketType.values()[i].text.equals(fromString))
                return EGPacketType.values()[i];
        return null;
    }
    public String text;
}
