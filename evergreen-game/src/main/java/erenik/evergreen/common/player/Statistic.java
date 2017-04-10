package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.Serializable;

import erenik.util.EList;
import erenik.util.Printer;

/**
 * Created by Emil on 2017-04-07.
 */

public enum Statistic implements Serializable {

    // Statistical values which will impact the game to some extent - to be determined.
    TimesKnockedOut, // Reached 0 HP but revived.
    MonsterKills, // Added
    PlayerKills, // Added
    TotalDamageTaken, // Added
    TotalDamageDealt, // added
    ItemsCrafted, // added
    ItemsInvented, // added
    FoodGathered, // added
    MaterialsGathered, // added
    TotalEmissionsGenerated, // Added
    TotalEmissionsReduced, // via the reduction actions and walking/cycling.
    ResourcesGiven,
    ResourcesReceived,
    ResourcesFoundByScouting,
    ItemsFoundByScouting,
    MessagesSent,
    SkillLevelUps,
    SkillLevelDowns,
    InheritedEmissions,
    ItemsGiven,
    BlueprintsGiven,
    EquipmentChanges,
    PlayerDiscoveries,
    InventionsLevel_0,
    InventionsLevel_1,
    InventionsLevel_2,
    InventionsLevel_3,
    InventionsLevel_4,
    InventionsLevel_5,
    ItemCrafted_level_0,
    ItemCrafted_level_1,
    ItemCrafted_level_2,
    ItemCrafted_level_3,
    ItemCrafted_level_4,
    ItemCrafted_level_5,
    ;
    private static final long serialVersionUID = 1L;

    Statistic()
    {
        this.defaultValue = 0;
    }
    public long defaultValue;
    public long value;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeLong(value);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        value = in.readLong();
        Printer.out("Statistic readObject");
    }

    static String Format(String arg, int maxLetters) {
        return String.format("%"+maxLetters+"s",arg);
    }
    public static String Print(String header, long[] statistics) {
        String s = "\n"+header+
                "\n============================================================================";
        for (int j = 0; j < statistics.length; ++j){
            s += "\n- "+Format(Statistic.values()[j].name(),25)+": "+String.format("%7d", statistics[j]);
        }
        return s;
    }

    public static long[] Add(long[] statistics, long[] statistics2) {
        long[] newList = new long[Statistic.values().length];
        for (int i = 0; i < statistics.length; ++i){
            newList[i] = statistics[i] + statistics2[i];
        }
        return newList;
    }
}
