/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import erenik.seriousgames.evergreen.player.Player;

/**
 * EvergreenRequest
 * @author Emil
 */
public class EGRequest extends EGPacket 
{
    public EGRequest(EGRequestType reqType, byte[] body)
    {
        this.type = EGPacketType.Request;
        this.reqt = reqType;
        this.body = body;
        System.out.println("EGRequest.EGRequest: Body first 10 bytes:");
        for (int i = 0; i < 10; ++i)
        {
            System.out.print(" "+(int)body[i]);
        }
        System.out.println();
        System.out.println("Bytes in body: "+body.length);
        for (int i = body.length - 10; i < body.length; ++i)
        {
            System.out.print(" "+(int)body[i]);
        }

    }
    static EGRequest Load(Player player)
    {
        EGRequest eg = new EGRequest(EGRequestType.Load, player.toByteArr());
        return eg;
    };
    static EGRequest Save(Player player)
    {
        EGRequest eg = new EGRequest(EGRequestType.Load, player.toByteArr());
        return eg;
    };
}
