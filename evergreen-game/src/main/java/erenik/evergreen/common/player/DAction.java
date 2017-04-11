package erenik.evergreen.common.player;

import java.util.Random;

import erenik.evergreen.common.Invention.InventionType;
import erenik.evergreen.common.Player;
import erenik.util.EList;
import erenik.util.Printer;

/**
 * Created by Emil on 2016-10-30.
 */
// Daily Action
public enum DAction {
    GatherFood,
    GatherMaterials,
    Scout,
    Recover,
    BuildDefenses,
    LookForPlayer, // o-o specific!
    LookForPlayers, // Looks for anyone.
    Invent,
    Craft,
    Steal, // Name of player, yeah.
    AttackAPlayer, // Name of player, yeah.
    Study,
    ReduceEmissions,
;

    static public String GetText(DAction forAction){
        switch (forAction){
            case GatherFood: return "Gather berries";
            case GatherMaterials: return "Gather materials";
            case Scout: return "Scout the area";
            case Recover: return "Recover";
            case BuildDefenses: return "Build defenses";
            case LookForPlayer: return "Look for player";
            case LookForPlayers: return "Look for players";
            case Invent: return "Invent";
            case Craft: return "Craft";
            case Steal: return "Steal";
            case AttackAPlayer: return "Attack a player";
            case Study: return "Study";
            case ReduceEmissions: return "Reduce emissions";
        }
        return null;
    }

    static public void SetTextDescArgs(Action forAction){
        Action a = forAction;
        a.requiredArguments = new EList<>();
        switch (a.DailyAction()){
            case GatherFood: a.text = "Gather berries";a.description = "Gather Food. Run out of food and you will soon be starving!"; break;
            case GatherMaterials: a.text = "Gather materials"; a.description = "Gather Materials used for constructing defenses, inventing, crafting and augmenting things.";break;
            case Scout:
                a.text = "Scout the area";
                a.description = "Search for food, materials, other's shelters, etc. Also carries risks of encountering enemies.";
                break;
            case Recover: a.text = "Recover"; a.description = "Recover lost HP.";
                break;
            case BuildDefenses: a.text = "Build defenses"; a.description =  "Consume Materials to strengthen defenses.";
                break;
            case LookForPlayer: a.text = "Look for player"; a.description = "Attempt to look for a specific player";
                a.requiredArguments.add(ActionArgument.PlayerName);
                break;
            case LookForPlayers: a.text = "Look for players"; a.description = "Look for any survivor nearby";
                break;
            case Invent: a.text = "Invent";
                a.description  = "Invent new weapons, armor, items or shelter additions";
                a.requiredArguments.add(ActionArgument.InventionCategory);
                break;
            case Craft: a.text = "Craft";
                a.description = "Craft items which you have previously invented or obtained the blueprints for";
                a.requiredArguments.add(ActionArgument.InventionToCraft);
                break;
            case Steal:
                a.text = "Steal";
                a.description = "Steal resources, items and/or blueprints from another player";
                a.requiredArguments.add(ActionArgument.Player);
                break;
            case AttackAPlayer: a.text = "Attack a player";
                a.description = "Attack a target player's shelter.";
                a.requiredArguments.add(ActionArgument.Player);
                break;
            case Study: a.text = "Study";
                a.description = "Gain EXP towards skills you are currently training.";
                break;
            case ReduceEmissions: a.text = "Reduce emissions";
                a.description = "Collect and process waste, plant trees and other actions to reduce your total emission footprint.";
                break;
        }
//    AugmentTransport("Augment transport", "Consume materials to improve various aspects of your transport of choice", ActionArgument.Transport, ActionArgument.TransportAugment),
//    Expedition("Expedition", "Go on an expedition to try and vanquish a stronghold of Evergreen monsters.", ActionArgument.Stronghold),
    }

    static Random randomAction = new Random(System.currentTimeMillis());
    /// Generates a random daily action for the given player. Returns null if nothing could be generated.
    public static Action RandomDailyAction(Player forPlayer) {
        if (!forPlayer.IsAliveOutsideCombat())
            return null;
        for(int i = 0; i < 10; ++i) { // Give N attempts per request.
            // Randomly select one.
            DAction dAction = DAction.values()[randomAction.nextInt(DAction.values().length * 5) % DAction.values().length];
            // Add an increased chance for certain actions - inventing and crafting, since they require iterated attempts to achieve success?
            if (randomAction.nextInt(100) > 90){
                dAction = DAction.Invent;
            }
            if (forPlayer.cd.inventionBlueprints.size() > 0 && randomAction.nextInt(100) > 50 + forPlayer.cd.inventory.size()){ // And if we have some inventions, try actually crafting them, until we have filled a decent size of our inventory?
                dAction = DAction.Craft;
            }
            Action action = new Action();
            action.SetDailyAction(dAction);
            boolean ok = action.AddRandomArguments(forPlayer); // Retursn true if valid arguments could be added.
            if (!ok)
                continue;
            // Check if the action has its requirements fulfilled?
            if (!action.HasValidArguments()) {
                Printer.out("Action "+dAction.name()+" has BAD arguments.");
                continue;
            }
            // Generate appropriate arguments based on the given player requesting them?
            return action;
        }
        return null;
    }

    private boolean HasValidArguments() {
        Printer.out("Not implemented");
        /*
        for (int i = 0; i < requiredArguments.size(); ++i) {
            ActionArgument aa = requiredArguments.get(i);
            // TODO: Add actual requirements here, to satisfy e.g. random generation from above.
        }*/
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
        Printer.out("Lacking player argument for action "+this.name());
        return false;
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

    public static EList<String> Names() {
        EList<String> l = new EList<String>();
        for (int i = 0; i < values().length; ++i)
            l.add(GetText(values()[i]));
        return l;
    }
    public static DAction GetFromString(String s) {
        for (int i = 0; i < values().length; ++i)
            if (GetText(values()[i]).equals(s))
                return values()[i];
        return null;
    }
};
