package erenik.seriousgames.evergreen.player;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emil on 2016-10-30.
 */
// Daily Action
public enum DAction
{
    FOOD("Gather berries", "Gather Food. Run out of food and you will soon be starving!"),
    MATERIALS("Gather materials", "Gather Materials used for constructing defenses, inventing, crafting and augmenting things."),
    SCOUT("Scout the area", "Search for food, materials, other's shelters, etc. Also carries risks of encountering enemies."),
    RECOVER("Recover", "Recover lost HP."),
    BUILD_DEF("Build defenses", "Consume Materials to strengthen defenses."),
    AUGMENT_TRANSPORT("Augment transport", "Consume materials to improve various aspects of your transport of choice"),
    LOOK_FOR_PLAYER("Look for player", "Attempt to look for a specific player, or just any player's shelter"),
    EXPEDITION("Expedition", "Go on an expedition to try and vanquish a stronghold of Evergreen monsters."),
    INVENT("Invent", "Invent new weapons, armor, items or shelter additions"),
    CRAFT("Craft", "Craft items which you have previously invented or obtained the blueprints for"),
    STEAL("Steal", "Steal resources, items and/or blueprints from another player"),
    ATTACK_A_PLAYER("Attack a player", "Attack a target player's shelter."),
    Study("Study", "Study a specific skill. Gain EXP towards that specific skill."),
    ;
    DAction(String txt, String description)
    {
        this.text = txt;
        this.description = description;
    }
    public String text = "";
    public String description = "";

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
