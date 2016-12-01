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
public enum EGResponseType {
    OK("OK"),
    Failed("NO");
    EGResponseType(String displayText)
    {
        text = displayText;
    }
    static EGResponseType fromString(String fromString)
    {
                        System.out.println("EGResponseType.fromString");

        for (int i = 0; i < EGResponseType.values().length; ++i)
            if (EGResponseType.values()[i].text.equals(fromString))
                return EGResponseType.values()[i];
        return null;
    }
    String text;
}
