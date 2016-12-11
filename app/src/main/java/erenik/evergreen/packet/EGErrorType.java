/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.packet;

/**
 *
 * @author Emil
 */
public enum EGErrorType {
    NoError("NoError"),
    SocketClosed("SocketClosed"),
    BadRequest("BadRequest"),
    ParseError("ParseError"),
    BadPassword("BadPassword"), 
    ReplyTimeoutReached("ReplyTimeoutReached"),
    ;    
    EGErrorType(String errType)
    {
        text = errType;
    }
    static EGErrorType fromString(String fromString)
    {
        System.out.println("EGErrorType.fromString");
        for (int i = 0; i < EGErrorType.values().length; ++i)
            if (EGErrorType.values()[i].text.equals(fromString))
                return EGErrorType.values()[i];
        return null;
    }

    
    String text;
}
