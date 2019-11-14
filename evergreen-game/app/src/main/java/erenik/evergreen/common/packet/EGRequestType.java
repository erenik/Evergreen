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
public enum EGRequestType {
    // Save character, also used for initial Creation requests.
    CreatePlayer("CreatePlayer"), // Initial request to create player/register :)
    Save("SAVE"), // Save character, Body of message should be POJO-save-data of Player object to save.
    Load("LOAD"), // Load character, Body of message should be POJO-save-data of Player object to save.
    GetGamesList("GetGamesList"),
    ActiveActions("ExecActiveActions"), // To evaluate the active (multiplayer) actions.
    LoadCharacters("LoadPlayers"), // email and password with a line-break between '\n' as a body. Default charset from EGPacket used, encoded as String object.
    RestartSameCharacter("RestartSameCharacter"),
    FetchWholeLog("FetchWholeLog"),
    FetchLog("FetchLog"),
    LogLength("LogLength"),
    TurnSurvived("TurnSurvived"), // Requests the Stat.TurnSurvived for the player. Reply is the Stat as an Integer.
    ; // To keep name, difficulty, etc.
    EGRequestType(String header)
    {
        this.text = header;
    }
    static EGRequestType fromString(String fromString) {
         //       Printer.out("EGRequestType.fromString");
        for (int i = 0; i < EGRequestType.values().length; ++i)
            if (EGRequestType.values()[i].text.equals(fromString))
                return EGRequestType.values()[i];
        return null;
    }
    String text;
}
