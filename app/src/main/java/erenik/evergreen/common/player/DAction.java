package erenik.evergreen.common.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import erenik.evergreen.common.Player;

/**
 * Created by Emil on 2016-10-30.
 */
// Daily Action
public enum DAction
{
    GatherFood("Gather berries", "Gather Food. Run out of food and you will soon be starving!"),
    GatherMaterials("Gather materials", "Gather Materials used for constructing defenses, inventing, crafting and augmenting things."),
    Scout("Scout the area", "Search for food, materials, other's shelters, etc. Also carries risks of encountering enemies."),
    Recover("Recover", "Recover lost HP."),
    BuildDefenses("Build defenses", "Consume Materials to strengthen defenses."),
    AugmentTransport("Augment transport", "Consume materials to improve various aspects of your transport of choice", ActionArgument.Transport, ActionArgument.TransportAugment),
    LookForPlayer("Look for player", "Attempt to look for a specific player, or just any player's shelter", ActionArgument.TextSearchType, ActionArgument.PlayerName),
    Expedition("Expedition", "Go on an expedition to try and vanquish a stronghold of Evergreen monsters.", ActionArgument.Stronghold),
    Invent("Invent", "Invent new weapons, armor, items or shelter additions", ActionArgument.InventionCategory),
    Craft("Craft", "Craft items which you have previously invented or obtained the blueprints for", ActionArgument.InventionToCraft),
    Steal("Steal", "Steal resources, items and/or blueprints from another player", ActionArgument.Player),
    AttackAPlayer("Attack a player", "Attack a target player's shelter.", ActionArgument.Player),
    Study("Study", "Gain EXP towards skills you are currently training."),
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

    static Random randomAction = new Random(System.currentTimeMillis());
    public static DAction RandomAction(Player forPlayer)
    {
        // Randomly select one.
        DAction action = DAction.values()[randomAction.nextInt(DAction.values().length * 5) % DAction.values().length];
        // Generate appropriate arguments based on the given player requesting them?
        return action;
    }

    @Override
    public String toString() {
        if (requiredArguments.size() == 0)
            return this.text;
        String str = this.text+":";
        for (int i = 0; i < requiredArguments.size(); ++i)
        {
            ActionArgument aarg = requiredArguments.get(i);
            str += aarg.value;
            if (i < requiredArguments.size() - 1)
                str += ",";
        }
        return str;
    }

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
