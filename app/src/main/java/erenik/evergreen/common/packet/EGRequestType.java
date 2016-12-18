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
    Save("SAVE"), // Save character, Body of message should be POJO-save-data of Player object to save.
    Load("LOAD"), // Load character, Body of message should be POJO-save-data of Player object to save.
    ;
    EGRequestType(String header)
    {
        this.text = header;
    }
    static EGRequestType fromString(String fromString)
    {
                System.out.println("EGRequestType.fromString");

        for (int i = 0; i < EGRequestType.values().length; ++i)
            if (EGRequestType.values()[i].text.equals(fromString))
                return EGRequestType.values()[i];
        return null;
    }
    String text;
}
