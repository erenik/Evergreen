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
public enum EGRequestType {
    // Save character, also used for initial Creation requests.
    Save("SAVE"),
    Load("LOAD"), // Load character, provide password to get access to it?
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
