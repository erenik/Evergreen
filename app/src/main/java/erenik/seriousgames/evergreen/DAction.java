package erenik.seriousgames.evergreen;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emil on 2016-10-30.
 */
// Daily Action
enum DAction
{
    FOOD("Gather berries"),
    MATERIALS("Gather materials"),
    SCOUT("Scout the area"),
    RECOVER("Recover"),
    BUILD_DEF("Build defenses"),
    AUGMENT_TRANSPORT("Augment transport"),
    LOOK_FOR_PLAYER("Look for player"),
    EXPEDITION("Expedition"),
    INVENT("Invent"),
    CRAFT("Craft"),
    STEAL("Steal"),
    ATTACK_A_PLAYER("Attack a player"),
    ;
    DAction(String txt)
    {
        this.text = txt;
    }
    String text = "";

    class Argument
    {
        static final int STRING = 0;
        static final int INT = 1;
        Argument(String sData)
        {
            type = STRING;
            this.sData = sData;
        }
        int type;
        String sData;
        int iData;
    }

    public static List<String> Names()
    {
        List<String> l = new ArrayList<String>();
        for (int i = 0; i < values().length; ++i)
            l.add(values()[i].text);
        return l;
    }
    public static DAction GetFromString(String s)
    {
        for (int i = 0; i < values().length; ++i)
            if (values()[i].text.equals(s))
                return values()[i];
        return null;
    }
};
