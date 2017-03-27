package erenik.evergreen.common.player;

import java.util.Random;

import erenik.evergreen.common.Invention.InventionType;
import erenik.evergreen.common.Player;
import erenik.util.EList;

/**
 * Created by Emil on 2016-10-30.
 */
// Daily Action
public enum DAction {
    GatherFood("Gather berries", "Gather Food. Run out of food and you will soon be starving!"),
    GatherMaterials("Gather materials", "Gather Materials used for constructing defenses, inventing, crafting and augmenting things."),
    Scout("Scout the area", "Search for food, materials, other's shelters, etc. Also carries risks of encountering enemies."),
    Recover("Recover", "Recover lost HP."),
    BuildDefenses("Build defenses", "Consume Materials to strengthen defenses."),
//    AugmentTransport("Augment transport", "Consume materials to improve various aspects of your transport of choice", ActionArgument.Transport, ActionArgument.TransportAugment),
    LookForPlayer("Look for player", "Attempt to look for a specific player, or just any player's shelter", ActionArgument.TextSearchType, ActionArgument.PlayerName),
//    Expedition("Expedition", "Go on an expedition to try and vanquish a stronghold of Evergreen monsters.", ActionArgument.Stronghold),
    Invent("Invent", "Invent new weapons, armor, items or shelter additions", ActionArgument.InventionCategory),
    Craft("Craft", "Craft items which you have previously invented or obtained the blueprints for", ActionArgument.InventionToCraft),
    Steal("Steal", "Steal resources, items and/or blueprints from another player", ActionArgument.Player), // Name of player, yeah.
    AttackAPlayer("Attack a player", "Attack a target player's shelter.", ActionArgument.Player), // Name of player, yeah.
    Study("Study", "Gain EXP towards skills you are currently training."),
    ReduceEmissions("Reduce emissions", "Collect and process waste, plant trees and other actions to reduce your total emission footprint."),
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
    public EList<ActionArgument> requiredArguments = new EList<ActionArgument>();
    public String text = "";
    public String description = "";

    static Random randomAction = new Random(System.currentTimeMillis());
    public static DAction RandomAction(Player forPlayer)
    {
        while (true) {
            // Randomly select one.
            DAction action = DAction.values()[randomAction.nextInt(DAction.values().length * 5) % DAction.values().length];
            boolean ok = action.AddRandomArguments(forPlayer); // Retursn true if valid arguments could be added.
            if (!ok)
                continue;
            // Check if the action has its requirements fulfilled?
            if (!action.HasValidArguments())
                continue;
            // Generate appropriate arguments based on the given player requesting them?
            return action;
        }
    }
    /// Adds randomly generated arguments for this given action, based on what the player has and knows.
    private boolean AddRandomArguments(Player forPlayer) {
        for (int i = 0; i < requiredArguments.size(); ++i) {
            ActionArgument aa = requiredArguments.get(i);
            int index;
            switch(aa) {
                case Transport:
                    aa.value = forPlayer.transports.get(randomAction.nextInt(forPlayer.transports.size())).name();
                    break;
                case Player:
                    if (forPlayer.knownPlayerNames.size() == 0) {
                        System.out.println("Doesn't know any other players, skipping.");
                        return false;
                    }
                    aa.value = forPlayer.knownPlayerNames.get(randomAction.nextInt(forPlayer.knownPlayerNames.size()));
                    System.out.println("Random player added as target for action: "+name()+" "+aa.value);
                    break;
                case InventionCategory:
                    index = randomAction.nextInt(InventionType.values().length);
                    System.out.println("index: "+index+" tot: "+InventionType.values().length);
                    aa.value = InventionType.values()[index].text();
                    System.out.println("Random invention category added: "+aa.value);
                    break;
                case InventionToCraft:
                    if (forPlayer.cd.inventions.size() == 0)
                        return false;
                    index = randomAction.nextInt(forPlayer.cd.inventions.size());
                    if (forPlayer.cd.inventions.size() == 0) {
                       System.out.println("Doesn't have any inventions, skipping");
                        return false;
                    }
                    aa.value = forPlayer.cd.inventions.get(index).name;
                    break;
            }
        }
        // Check what the player currently has available.
        switch(this) {
            case AttackAPlayer:
            case Steal:
            case Invent:
        }
        return true; // If e.g. it cannot add something, return false earlier.
    }

    private boolean HasValidArguments() {
        for (int i = 0; i < requiredArguments.size(); ++i) {
            ActionArgument aa = requiredArguments.get(i);
            // TODO: Add actual requirements here, to satisfy e.g. random generation from above.
        }
        return true;
    }

    String firstArgument()
    {
        /*
        String arg = "";
        if (arg.size() > 0)
            arg = requiredArguments.get(0);
            */
        return ""; //arg;
    }
    private boolean HasInventionTypeArgument() {
//        if (InventionType.GetFromString(requiredArguments.get(0)) != null;
        return true;
    }

    private boolean HasPlayerArgument() {
        System.out.println("Lacking player argument for action "+this.name());
        return false;
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

    public static EList<String> Names()
    {
        EList<String> l = new EList<String>();
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
