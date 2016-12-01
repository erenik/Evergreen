/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 * EvergreenRequest
 * @author Emil
 */
public class EGRequest extends EGPacket 
{
    public EGRequest(EGRequestType reqType, String body)
    {
        this.type = EGPacketType.Request;
        this.reqt = reqType;
        this.body = body;
    }

}
