package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.util.Random;

import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogType;
import erenik.util.EList;

/**
 * Created by Emil on 2016-11-11.
 */
public enum AAction {
    GiveResources,
    SendMessage,
    GiveItem, // Oh, yeah!
    GiveBlueprint,
//     ReadReceivedMessages,
//     RequestResources,
//    SendTreatyRequest("Send treaty request", "Request a treaty with target player.\n- Knowledge-Sharing enables automatic sharing of inventions.\n- Alliance/Base-Sharing agreement will join your shelters, and winning conditions will become shared.", ActionArgument.TreatyType);
    //   ExploreAbandonedShelter("Explore Abandoned shelter", "Explores one of the shelters you've found previously. It may yield resources, but could also contain "),
//    ExplorePlayerInhabitedShelter("Explore Inhabited shelter", "Explores one of the inhabited shelters you found previously"),
    //  CoTransport("Co-Transport", "Share a transport with target player, reducing emissions and increasing social support bonuses if you get attacked during transport.", ActionArgument.Player),
    ;
    /// o-o

//    public String message = ""; // For the SendMessage.
  //  public String targetPlayer = "";
    //public String transportToShare = ""; // Add later? if at all.
//    public String resourceToGive = ""; // Can be used to request resources as well.
//    public float quantity = 0;
 //   boolean processed = false; // Set to true on server when it has actually been processed.

    /*
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        int version = 0; // Initial version.
        out.writeInt(version);
        out.writeObject(message);
        out.writeObject(targetPlayer);
        out.writeObject(transportToShare);
        out.writeObject(resourceToGive);
        out.writeFloat(quantity);
        out.writeBoolean(processed);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
        int version = in.readInt();
        message = (String) in.readObject();
        targetPlayer = (String) in.readObject();
        transportToShare = (String) in.readObject();
        resourceToGive = (String) in.readObject();
        quantity = in.readFloat();
        processed = in.readBoolean();
    }
*/
    static String GetText(AAction aa) {
        switch (aa) {
//            case ReadReceivedMessages: return "Read received messages";
            case GiveResources: return "Give resources";
            case SendMessage: return "Send message";
            case GiveItem: return "Give item";
            case GiveBlueprint: return "Give blueprint";
        }
        return null;
    }
//            case RequestResources: return "Request resources";
//            case CoTransport: return "Co-Transport";

    public static void SetTextDescArgs(Action action) {
        action.text = GetText(action.ActiveAction());
        Action a = action;
        a.requiredArguments = new EList<>();
        switch (action.ActiveAction()){
            case GiveResources: a.description = "Gives resources to target player";
                a.requiredArguments.add(ActionArgument.Player);
                a.requiredArguments.add(ActionArgument.ResourceType);
                a.requiredArguments.add(ActionArgument.ResourceQuantity);
                break;
            case SendMessage: a.description = "Send a message to some player";
                a.requiredArguments.add(ActionArgument.Player);
                a.requiredArguments.add(ActionArgument.Text);
                break;
            case GiveItem: a.description = "Give an item to some player";
                a.requiredArguments.add(ActionArgument.Item);
                break;
            case GiveBlueprint: a.description = "Give an invention blueprint to some player";
                a.requiredArguments.add(ActionArgument.Blueprint);
                break;
        }
        //   ExploreAbandonedShelter("Explore Abandoned shelter", "Explores one of the shelters you've found previously. It may yield resources, but could also contain "),
//    ExplorePlayerInhabitedShelter("Explore Inhabited shelter", "Explores one of the inhabited shelters you found previously"),
//            case CoTransport: (, "Share a transport with target player, reducing emissions and increasing social support bonuses if you get attacked during transport.", ActionArgument.Player),
    }

    static Random randomAction = new Random(System.currentTimeMillis());
    public static Action RandomActiveAction(Player forPlayer) {
        if (forPlayer.IsAlive() == false)
            return null;
        for(int i = 0; i < 10; ++i) { // Give N attempts per request.
            // Randomly select one.
            AAction aAction = AAction.values()[randomAction.nextInt(AAction.values().length * 5) % AAction.values().length];
            Action action = new Action();
            action.SetActiveAction(aAction);
            boolean ok = action.AddRandomArguments(forPlayer); // Retursn true if valid arguments could be added.
            if (!ok)
                continue;
            // Check if the action has its requirements fulfilled?
            if (!action.HasValidArguments()) {
                System.out.println("Action "+action.ActiveAction().name()+" has BAD arguments.");
                continue;
            }
            // Generate appropriate arguments based on the given player requesting them?
            return action;
        }
        return null;
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
            l.add(GetText(values()[i]));
        return l;
    }
    public static AAction GetFromString(String s)
    {
        for (int i = 0; i < values().length; ++i)
            if (GetText(values()[i]).equals(s))
                return values()[i];
        return null;
    }
}
