package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import erenik.evergreen.common.Invention.InventionType;
import erenik.evergreen.common.Player;
import erenik.util.EList;
import erenik.util.Printer;

/** Parent class of all actions?
 * Created by Emil on 2017-03-28.
 */

public class Action{
    private DAction daType = null; // Daily action.
    private AAction aaType = null; // Active action

    public DAction DailyAction(){return daType;};
    public AAction ActiveAction() { return aaType; };

    Action(){
        requiredArguments = new EList<>();
    };
    public Action(DAction dailyActionType, EList<String> args){
        SetDailyAction(dailyActionType);
        LoadArgs(args);
    }
    public Action(AAction activeActionType, EList<String> args){
        SetActiveAction(activeActionType);
        LoadArgs(args);
    }
    void SetDailyAction(DAction type){
        daType = type;
        aaType = null;
        UpdateTextDescArgs();
    }
    void SetActiveAction(AAction type){
        aaType = type;
        daType = null;
        UpdateTextDescArgs();
    }

    void LoadArgs(EList<String> fromList){
        if (fromList == null) {
            Printer.out(" null list");
            return;
        }
        for (int i = 0; i < fromList.size() && i < requiredArguments.size(); ++i){
            requiredArguments.get(i).value = fromList.get(i);
        }
    }

    void writeTo(ObjectOutputStream out) throws IOException {
        out.writeInt(daType == null? -1 : daType.ordinal());
        out.writeInt(aaType == null? -1 : aaType.ordinal());
        out.writeInt(requiredArguments.size());
        for (int i = 0; i < requiredArguments.size(); ++i)
            out.writeObject(requiredArguments.get(i).value);
    }
    boolean readFrom(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int daIndex = in.readInt();
        int aaIndex = in.readInt();
        requiredArguments = new EList<>();
        if (daIndex >= 0) {
            daType = DAction.values()[daIndex];
            DAction.SetTextDescArgs(this);
        }
        else if (aaIndex >= 0){
            aaType = AAction.values()[aaIndex];
            AAction.SetTextDescArgs(this);
        }
        else
            return false;
        int numArgs = in.readInt(); // Load saved args.
        for (int i = 0; i < numArgs; ++i){
            String str = (String) in.readObject();
            if (requiredArguments.size() > i)
                requiredArguments.get(i).value = str;
        }
        return true;
    }

    /// o-o
    public EList<ActionArgument> requiredArguments = null;
    public String text = "";
    public String description = "";

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

    public static Action ParseFrom(String s)
    {
        String[] p = s.split(":");
        String type = p[0];
        Action action = new Action();
        action.daType = DAction.GetFromString(type);
        action.aaType = AAction.GetFromString(type);
        action.UpdateTextDescArgs();
        if (action.daType == null &&
                action.aaType == null)
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
            Printer.out("Arg: "+arg.toString()+" Argstr: "+argStr);
            arg.value = argStr;
        }
        return action;
    }
    // Updates text, description and arguments based on what kind of an action this is - via DAction and AAction static methods.
    public void UpdateTextDescArgs() {
        requiredArguments = new EList<>();
        if (daType != null)
            DAction.SetTextDescArgs(this);
        else if (aaType != null)
            AAction.SetTextDescArgs(this);
    }

    public static Action GetFromString(String text) {
        String[] split = text.split(":");
        String textNoComma = split[0];
        AAction aa = AAction.GetFromString(textNoComma);                 // Open display with further options
        DAction da = DAction.GetFromString(textNoComma);
        EList<String> args = null;
        if (split.length > 1)
            args = new EList<String>(split[1].split(","));
        if (aa != null)
            return new Action(aa, args);
        if (da != null)
            return new Action(da, args);
        Printer.out("Couldn't find action by name: "+textNoComma);
        new Exception().printStackTrace();
        return null;
    }

    /// Gets player argument if any.
    public String GetPlayerName() {
        for (int i = 0; i < requiredArguments.size(); ++i){
            ActionArgument aa = requiredArguments.get(i);
            switch (aa){
                case Player:
                case PlayerName: return aa.value;
            }
        }
        return null;
    }

    public String Get(ActionArgument argType) {
        for (int i = 0; i < requiredArguments.size(); ++i){
            ActionArgument aa = requiredArguments.get(i);
            if (aa.ordinal() == argType.ordinal())
                return aa.value;
        }
        return null;
    }

    static Random raction = new Random();

    // Returns what...?
    public boolean AddRandomArguments(Player forPlayer) {
//        Printer.out("UPDATE - for client simulation");
        if (requiredArguments == null)
            return true;
        for (int i = 0; i < requiredArguments.size(); ++i) {
            ActionArgument aa = requiredArguments.get(i);
            int index;
            switch(aa) {
                default:
                    Printer.out("Unable to add argument for argument: "+aa.name());
                    break;
                case ResourceType:
                    aa.value = ResourceType.values()[raction.nextInt(ResourceType.values().length)].name();
                    break;
                case ResourceQuantity:
                    aa.value = ""+(raction.nextFloat()*5+1);
                    break;
//                case Transport:
  //                  aa.value = forPlayer.transports.get(randomAction.nextInt(forPlayer.transports.size())).name();
    //                break;
                case Item:
                    if (forPlayer.cd.inventory.size() <= 0)
                        return false;
                    aa.value = forPlayer.cd.inventory.get(raction.nextInt(forPlayer.cd.inventory.size())).name;
                    break;
                case Blueprint:
                    if (forPlayer.cd.inventionBlueprints.size() == 0)
                        return false;
                    aa.value = forPlayer.cd.inventionBlueprints.get(raction.nextInt(forPlayer.cd.inventionBlueprints.size())).name;
                    break;
                case Text:
                    switch (raction.nextInt(4)){
                        case 0: aa.value = "Hey there!"; break;
                        case 1: aa.value = "Can you give me some food?"; break;
                        default: aa.value = "Hello world."; break;
                    }
                    break;
                case Player:
                    if (forPlayer.cd.knownPlayerNames.size() == 0) {
                    //    Printer.out("Doesn't know any other players, skipping.");
                        return false;
                    }
                    aa.value = forPlayer.cd.knownPlayerNames.get(raction.nextInt(forPlayer.cd.knownPlayerNames.size()));
                //    Printer.out("Random player added as target for action: "+this.text+" "+aa.value);
                    break;
                case PlayerName:
                    aa.value = "Ere";                     // Search for any arbitrary letter combinations?
                    break;
                case InventionCategory:
                    index = raction.nextInt(InventionType.values().length);
                    Printer.out("index: "+index+" tot: "+InventionType.values().length);
                    aa.value = InventionType.values()[index].text();
                    Printer.out("Random invention category added: "+aa.value);
                    break;
                case InventionToCraft:
                    if (forPlayer.cd.inventionBlueprints.size() == 0)
                        return false;
                    index = raction.nextInt(forPlayer.cd.inventionBlueprints.size());
                    if (forPlayer.cd.inventionBlueprints.size() == 0) {
                        Printer.out("Doesn't have any inventions, skipping");
                        return false;
                    }
                    aa.value = forPlayer.cd.inventionBlueprints.get(index).name;
                    break;
            }
        }
        return true; // If e.g. it cannot add something, return false earlier.
    }

    public boolean HasValidArguments() {
        for (int i = 0; i < requiredArguments.size(); ++i){
            ActionArgument aa = requiredArguments.get(i);
            switch (aa){
                case InventionCategory:
                case InventionToCraft:
                case Player:
                case PlayerName:
                case Item:
                case Text:
                case ResourceType:
                case ResourceQuantity:
                    if (aa.value == null)
                        return false;
                    if (aa.value.length() == 0)
                        return false;
            }
        }
        return true;
    }
}
