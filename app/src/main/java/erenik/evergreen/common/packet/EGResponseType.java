/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.common.packet;

/**
 *
 * @author Emil
 */
public enum EGResponseType {
    /// Data-containing responses. Body has contents in these.
    OK("OK"),
    GamesList("GamesList"),
    Player("Player"),
    Failed("NO"),

    /// ERROR responses
    NoError("NoError"),
    SocketClosed("SocketClosed"),
    BadRequest("BadRequest"),
    ParseError("ParseError"),
    NoSuchGame("NoSuchGame"), // When ID of game does not exist.
    NoSuchPlayer("NoSuchPlayer"), // When the Player with given name/gameID doesn't exist.
    PlayerWithNameAlreadyExists("PlayerWithNameAlreadyExists"),
    BadPassword("BadPassword"),
    ReplyTimeoutReached("ReplyTimeoutReached"),
    ;
    EGResponseType(String displayText)
    {
        text = displayText;
    }
    static EGResponseType fromString(String fromString)
    {
       //                 System.out.println("EGResponseType.fromString");
        for (int i = 0; i < EGResponseType.values().length; ++i)
            if (EGResponseType.values()[i].text.equals(fromString))
                return EGResponseType.values()[i];
        return null;
    }
    String text;
}
