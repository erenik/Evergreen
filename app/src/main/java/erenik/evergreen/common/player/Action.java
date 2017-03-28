package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import erenik.evergreen.common.Player;
import erenik.util.EList;

/** Parent class of all actions?
 * Created by Emil on 2017-03-28.
 */

public class Action{
    public DAction daType = null; // Daily action.
    public AAction aaType = null; // Active action

    Action(){};
    public Action(DAction dailyActionType, EList<String> args){
        daType = dailyActionType;
        requiredArguments = new EList<>();
        DAction.SetTextDescArgs(this);
        LoadArgs(args);
    }
    public Action(AAction activeActionType, EList<String> args){
        aaType = activeActionType;
        requiredArguments = new EList<>();
        AAction.SetTextDescArgs(this);
        LoadArgs(args);
    }

    void LoadArgs(EList<String> fromList){
        if (fromList == null) {
            System.out.println(" null list");
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
        if (action.daType != null)
            DAction.SetTextDescArgs(action);
        else if (action.aaType != null)
            AAction.SetTextDescArgs(action);
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
            System.out.println("Arg: "+arg.toString()+" Argstr: "+argStr);
            arg.value = argStr;
        }
        return action;
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
        System.out.println("Couldn't find action by name: "+textNoComma);
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
}
