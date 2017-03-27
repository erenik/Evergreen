package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;

import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogType;
import erenik.util.EList;

/**
 * Created by Emil on 2016-11-11.
 */
public enum AAction implements Serializable {
    ReadReceivedMessages("Read received messages", "Reads/processes one of the messages or requests that you have received from other players", ActionArgument.ReceivedMessage),
 //   ExploreAbandonedShelter("Explore Abandoned shelter", "Explores one of the shelters you've found previously. It may yield resources, but could also contain "),
//    ExplorePlayerInhabitedShelter("Explore Inhabited shelter", "Explores one of the inhabited shelters you found previously"),
    CoTransport("Co-Transport", "Share a transport with target player, reducing emissions and increasing social support bonuses if you get attacked during transport.", ActionArgument.Player),
    GiveResources("Give resources", "Gives resources to target player", new ActionArgument[]{ActionArgument.Player, ActionArgument.ResourceType, ActionArgument.ResourceQuantity}),
    RequestResources("Request resources", "Request resources from target player", new ActionArgument[]{ActionArgument.Player,ActionArgument.ResourceType, ActionArgument.ResourceQuantity}),
    SendMessage("Send message", "Send a message to some player", ActionArgument.Player, ActionArgument.Text),
//    SendTreatyRequest("Send treaty request", "Request a treaty with target player.\n- Knowledge-Sharing enables automatic sharing of inventions.\n- Alliance/Base-Sharing agreement will join your shelters, and winning conditions will become shared.", ActionArgument.TreatyType);
    ;
    /// o-o
    public EList<ActionArgument> requiredArguments = new EList<ActionArgument>();
    public String text = "";
    public String description = "";

    public String message = ""; // For the SendMessage.
    public String targetPlayer = "";
    public String transportToShare = ""; // Add later? if at all.
    public String resourceToGive = ""; // Can be used to request resources as well.
    public float quantity = 0;
    boolean processed = false; // Set to true on server when it has actually been processed.

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

    AAction(String txt, String description)
    {
        this.text = txt;
        this.description = description;
    }
    AAction(String txt, String description, ActionArgument arg1)
    {
        this.text = txt;
        this.description = description;
        this.requiredArguments.add(arg1);
    }
    AAction(String txt, String description, ActionArgument arg1, ActionArgument arg2)
    {
        this.text = txt;
        this.description = description;
        this.requiredArguments.add(arg1);
        this.requiredArguments.add(arg2);
    }
    AAction(String txt, String description, ActionArgument[] args)
    {
        this.text = txt;
        this.description = description;
        for (int i = 0; i < args.length; ++i)
            requiredArguments.add(args[i]);
    }

    public static AAction ParseFrom(String s)
    {
        String[] p = s.split(":");
        String type = p[0];
        AAction action = AAction.GetFromString(type);
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
    public static AAction GetFromString(String s)
    {
        for (int i = 0; i < values().length; ++i)
            if (values()[i].text.equals(s))
                return values()[i];
        return null;
    }
}
