package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import erenik.evergreen.common.Invention.Invention;
import erenik.util.Byter;

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
        skills = new ArrayList<Skill>(Arrays.asList(Skill.values()));
        dailyActions = new ArrayList<>();
        skillTrainingQueue = new ArrayList<>();
    }
    public int totalLogMessagesOnServer = 0; // Just to see if we should update it here or not.
    // All that is relevant to the client.
    public float[] statArr;
    public float[] configArr;
    public ArrayList<Skill> skills = new ArrayList<>();        /// Array of exp in each Skill.
    public ArrayList<String> dailyActions = new ArrayList<>();    /// List of actions this player will take during the day/turn. optionally with extra arguments after a colon?
    public ArrayList<AAction> queuedActiveActions = new ArrayList<>();
    public ArrayList<String> skillTrainingQueue = new ArrayList<>();        /// Queued skills to be leveled up.
    public ArrayList<Invention> inventions = new ArrayList<>(); // Blueprints, 1 of each.
    public ArrayList<Invention> inventory = new ArrayList<>(); // Inventory, may have duplicates of items that can be traded etc.


    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        System.out.println("ClientData writeObject");
        out.writeInt(totalLogMessagesOnServer);
        out.writeObject(statArr);
        out.writeObject(configArr);
        out.writeObject(skills);
        out.writeObject(dailyActions);
        out.writeObject(queuedActiveActions);
        out.writeObject(skillTrainingQueue);
        out.writeObject(inventions);
        out.writeObject(inventory);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
        System.out.println("ClientData writeObject");
        totalLogMessagesOnServer = in.readInt();
        statArr = (float[]) in.readObject();
        configArr = (float[]) in.readObject();
        skills = (ArrayList<Skill>) in.readObject();
        dailyActions = (ArrayList<String>) in.readObject();
        queuedActiveActions = (ArrayList<AAction>) in.readObject();
        skillTrainingQueue  = (ArrayList<String>) in.readObject();
        inventions = (ArrayList<Invention>) in.readObject();
        inventory = (ArrayList<Invention>) in.readObject();
    }
    private void readObjectNoData() throws ObjectStreamException
    {}
}
