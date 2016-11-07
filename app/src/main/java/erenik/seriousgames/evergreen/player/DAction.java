package erenik.seriousgames.evergreen.player;

import android.app.Notification;

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
    AUGMENT_TRANSPORT("Augment transport", "Consume materials to improve various aspects of your transport of choice", ActionArgument.Transport, ActionArgument.TransportAugment),
    LOOK_FOR_PLAYER("Look for player", "Attempt to look for a specific player, or just any player's shelter", ActionArgument.PlayerName),
    EXPEDITION("Expedition", "Go on an expedition to try and vanquish a stronghold of Evergreen monsters.", ActionArgument.Stronghold),
    Invent("Invent", "Invent new weapons, armor, items or shelter additions", ActionArgument.InventionCategory),
    Craft("Craft", "Craft items which you have previously invented or obtained the blueprints for", ActionArgument.InventionToCraft),
    STEAL("Steal", "Steal resources, items and/or blueprints from another player", ActionArgument.Player),
    ATTACK_A_PLAYER("Attack a player", "Attack a target player's shelter.", ActionArgument.Player),
    Study("Study", "Study a specific skill. Gain EXP towards that specific skill.", ActionArgument.SkillToStudy),
    ;
    DAction(String txt, String description)
    {
        this.text = txt;
        this.description = description;
    }
    DAction(String txt, String description, ActionArgument arg1)
    {
        this.text = txt;
        this.description = description;
        this.requiredArguments.add(arg1);
    }
    DAction(String txt, String description, ActionArgument arg1, ActionArgument arg2)
    {
        this.text = txt;
        this.description = description;
        this.requiredArguments.add(arg1);
        this.requiredArguments.add(arg2);
    }
    /// o-o
    public List<ActionArgument> requiredArguments = new ArrayList<ActionArgument>();
    public String text = "";
    public String description = "";

    public static DAction ParseFrom(String s)
    {
        String[] p = s.split(":");
        String type = p[0];
        DAction action = DAction.GetFromString(type);
        if (action == null)
            return null;
        if (p.length <= 1) // No arguments? Return now.
            return action;
        String[] args = p[1].split(",");
        int argumentsParsed = 0;
        for (int j = 0; j < action.requiredArguments.size(); ++j)
        {
            if (argumentsParsed >= args.length)
                break;
            String argStr = args[argumentsParsed];
            ++argumentsParsed;

            ActionArgument arg = action.requiredArguments.get(j);
            System.out.println("Arg: "+arg.toString()+" Argstr: "+argStr);
            arg.value = argStr;
        }
        return action;
    }

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
