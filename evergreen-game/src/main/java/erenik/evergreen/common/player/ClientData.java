package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;

import erenik.evergreen.StringUtils;
import erenik.evergreen.common.Invention.Invention;
import erenik.util.Byter;
import erenik.util.EList;

/**
 * Created by Emil on 2017-03-24.
 */

// Data modifiable and viewable by the client - any client.
public class ClientData implements Serializable {
    public ClientData(){
        Init();
    }
    public ClientData Copy(){
        ClientData newData = (ClientData) Byter.toObject(Byter.toByteArray(this)); // Clone that also ensures that the byte-conversion works locally.
        return  newData;
    }
    public void Init(){
        statArr = new float[Stat.values().length];
        configArr = new float[Config.values().length];
        skills = new EList<Skill>(Skill.values());
        dailyActions = new EList<>();
        skillTrainingQueue = new EList<>();
        queuedActiveActions = new EList<>();
        inventions = new EList<>();
        inventory = new EList<>();
        knownPlayerNames = new EList<>();
    }
    public int totalLogMessagesOnServer = 0; // Just to see if we should update it here or not.
    // All that is relevant to the client.
    public float[] statArr;
    public float[] configArr;
    public EList<Skill> skills = new EList<>();        /// Array of exp in each Skill.
    public EList<Action> dailyActions = new EList<>();    /// EList of actions this player will take during the day/turn. optionally with extra arguments after a colon?
    public EList<Action> queuedActiveActions = new EList<>();
    public EList<String> skillTrainingQueue = new EList<>();        /// Queued skills to be leveled up.
    public EList<Invention> inventions = new EList<>(); // Blueprints, 1 of each.
    public EList<Invention> inventory = new EList<>(); // Inventory, may have duplicates of items that can be traded etc.
    public EList<String> knownPlayerNames = new EList<>();

    /// SERIALIZABLE - DO NOT CHANGE METHOD PARAMS OR RETURN-TYPE
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        System.out.println("ClientData writeObject");
        out.writeInt(totalLogMessagesOnServer);

        out.writeObject(statArr);
        out.writeObject(configArr);
        out.writeObject(skills);

        out.writeInt(dailyActions.size());
        for (int i = 0; i < dailyActions.size(); ++i)
            dailyActions.get(i).writeTo(out);
        out.writeInt(queuedActiveActions.size());
        for (int i = 0; i < queuedActiveActions.size(); ++i)
            queuedActiveActions.get(i).writeTo(out);

        out.writeObject(skillTrainingQueue);
        out.writeObject(inventions);
        out.writeObject(inventory);

        out.writeObject(knownPlayerNames);

    }
    /// SERIALIZABLE - DO NOT CHANGE METHOD PARAMS OR RETURN-TYPE
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
        System.out.println("ClientData readObject");
        Init();

        totalLogMessagesOnServer = in.readInt();

        statArr = (float[]) in.readObject();
        configArr = (float[]) in.readObject();
        skills = (EList<Skill>) in.readObject();

        int numDA = in.readInt();
        dailyActions = new EList<>();
        for (int i = 0; i < numDA; ++i){
            Action a = new Action();
            a.readFrom(in);
            dailyActions.add(a);
        }
        int numQAA = in.readInt();
        queuedActiveActions = new EList<>();
        for (int i = 0; i < numQAA; ++i){
            Action a = new Action();
            a.readFrom(in);
            queuedActiveActions.add(a);
        }

        skillTrainingQueue  = (EList<String>) in.readObject();
        inventions = (EList<Invention>)in.readObject();
        inventory = (EList<Invention>) in.readObject();

        knownPlayerNames = (EList<String>) in.readObject();

    }
    private void readObjectNoData() throws ObjectStreamException {

    }

    public boolean readFrom(ObjectInputStream in) {
        try {
            readObject(in);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean writeTo(ObjectOutputStream out) {
        try {
            writeObject(out);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void PrintDetails() {
        System.out.println("totalLogMesgsonserver: "+totalLogMessagesOnServer+" statArr: "+statArr+" confArr: "+configArr+" skills: "+skills+" dActions: "+dailyActions+
                " queedAActions"+queuedActiveActions+" skills"+skillTrainingQueue+" invents"+inventions+" items"+inventory);
    }
}
