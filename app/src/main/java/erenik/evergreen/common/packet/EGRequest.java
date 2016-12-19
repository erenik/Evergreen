/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.common.packet;

import erenik.evergreen.common.Player;

/**
 * EvergreenRequest
 * @author Emil
 */
public class EGRequest extends EGPacket 
{
    /// Requests without body, such as pure GETs for info.
    public EGRequest(EGRequestType reqType)
    {
        this.type = EGPacketType.Request;
        this.reqt = reqType;
    }
    public EGRequest(EGRequestType reqType, byte[] body)
    {
        this.type = EGPacketType.Request;
        this.reqt = reqType;
        this.body = body;
//        System.out.println("EGRequest.EGRequest: Body first 10 bytes:");
        for (int i = 0; i < 10; ++i)
        {
  //          System.out.print(" "+(int)body[i]);
        }
//        System.out.println();
        if (body != null) {
            //    System.out.println("Bytes in body: "+body.length);
            for (int i = body.length - 10; i < body.length; ++i) {
                //          System.out.print(" "+(int)body[i]);
            }
        }
    }
    public static EGRequest Load(Player player)
    {
        EGRequest eg = new EGRequest(EGRequestType.Load, player.toByteArr());
        return eg;
    };
    public static EGRequest Save(Player player)
    {
        EGRequest eg = new EGRequest(EGRequestType.Save, player.toByteArr());
        return eg;
    };
    public static EGRequest byType(EGRequestType type)
    {
        EGRequest eg = new EGRequest(type);
        return eg;
    }

    public static EGPacket CreatePlayer(Player player) {
        return new EGRequest(EGRequestType.CreatePlayer, player.toByteArr());
    }
}
