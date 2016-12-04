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
