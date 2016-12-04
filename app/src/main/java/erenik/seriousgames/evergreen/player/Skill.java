package erenik.seriousgames.evergreen.player;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Emil on 2016-10-30.
 */
public enum Skill implements Serializable
{
    Foraging("Foraging", AddLinear(5,5,4), "Increases food acquired while foraging. Increases chance to find good foraging spots."),
    FleetRetreat("Fleet retreat", AddLinearAccum(5, 4, 4), "Makes fleeing easier. Gains more EXP from encounters on successful retreats."),
    Survival("Survival", AddLinearAccum(5,5,5), "Increases Max HP, HP recovery gain while using the Recover action."), // Add the HP recovery upon KO for regular mode later (when regular/Hardcore options are there).
    Architecting("Architecting", AddLinear(10,10,6), "Increases building speed when building shelter defenses and other structures"), // Add for Shelter additions later.
    MaterialEfficiency("Material efficiency",  Quadratic(5, 2, 5), "Adds a chance to retain some materials that would otherwise have been spent. Saved materials reduce emissions generated as well."),
    Inventing("Inventing", AddLinear(10,10,8), "Improves success chance while inventing new items. Increases maximum bonuses that new inventions may have."), // Not added yet.
    DefensiveTraining("Defensive training", AddLinearAccum(5,5,7), "Increases overall defense during combat. Increases survivability"),
    UnarmedCombat("Unarmed combat", AddLinearAccum(3,2,9), "Increases attack, damage and amount of attacks while fighting unarmed."),
    WeaponizedCombat("Weaponized combat", AddLinearAccum(3,1,6), "Increases attack and damage of attacks while using weapons."),
    Marksmanship("Marksmanship", AddLinear(5,5,6), "If you have a ranged weapon: Enables ranged attacks before melee combat starts. Increases ranged attack, damage and amount of attacks."), // Not added yet.
    Parrying("Parrying", AddLinearAccum(2, 5, 6), "Enables parrying of melee attacks. Increases probability at higher levels."), // Not added yet.
    Thief("Thief", AddLinear(5, 5, 5), "Reduces risks and increases profits when stealing from other players"), // Not added yet.
    GroupCombatTraining("Group combat training", AddLinear(5,5,5), "Increases attack and defense while fighting with an ally."), // Not added yet.
    Studious("Studious", AddLinearAccum(20,10,5), "Grants additional EXP each turn. Increases EXP gained when choosing the Study action"),
/*
*/
    ;
    Skill()
    {
        this.text = "textToReplace";
    };
    Skill (String txt, int[] expRequired, String briefDescription)
    {
        this.text = txt;
        this.expRequired = expRequired;
        this.briefDescription = briefDescription;
    }
    
    // Serialization version.
    public static final long serialVersionUID = 1L;
 
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.writeFloat(totalExp);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        totalExp = in.readFloat();
    }
    private void readObjectNoData() throws ObjectStreamException
    {

    }
    
    // E.g. 2,3,4 -> 2,5,8,11
    static int[] AddLinear(int base, int plusEachLevel, int maxLevel)
    {
        int[] a = new int[maxLevel];
        for (int i = 0; i < a.length; ++i)
            a[i] = base + plusEachLevel * i;
        return a;
    }
    // E.g. 2,3,4 -> 2,5,11,20              3,2,9 -> 3,5,9,15,23,33,45,59,75           2,5,6 -> 2,7,17,32,52,77
    static int[] AddLinearAccum(int base, int plusEachLevel, int maxLevel)
    {
        int[] a = new int[maxLevel];
        int previousPlusAccum = 0;
        for (int i = 0; i < maxLevel; ++i)
        {
            a[i] = base + previousPlusAccum;
            previousPlusAccum += plusEachLevel;
        }
        return a;
    }
    // E.g. 2,3,4 -> 2,6,18,54
    static int[] Quadratic(int base, float multiplier, int maxLevel)
    {
        int[] a = new int[maxLevel];
        int previous = 0;
        a[0] = base;
        for (int i = 1; i < maxLevel; ++i)
        {
            a[i] = (int) (a[i-1] * multiplier);
        }
        return a;
    }
    public String text = "text";
    String briefDescription = "Desc";
    // Exp aquired in this skill towards the next level?
    int EXPToNext()
    {
        int level = Level();
        int totalNeeded = EXPToLevelFrom0(Level() + 1);
//        System.out.println("totalNeeded: "+totalNeeded+" and totalExp: "+totalExp);
        return (int) (totalNeeded - this.totalExp);
    }

    public static List<String> Names()
    {
        List<String> l = new ArrayList<String>();
        for (int i = 0; i < values().length; ++i)
        {
            l.add(values()[i].text);
        }
        return l;
    }

    public static Skill GetFromString(String s)
    {
        for (int i = 0; i < Skill.values().length; ++i)
        {
            if (Skill.values()[i].text.equals(s))
                return Skill.values()[i];
        }
        return null;
    }

    public class LeveledUpException extends  Exception
    {
        // Exp wasted used if reaching max level.
        LeveledUpException(int levelReached, int expWasted)
        {
            super("Leveled up");
            this.levelReached = levelReached;
            this.expWasted = expWasted;
        };
        int expWasted = 0;
        int levelReached = 0;
    }

    //    // Throws exception on level up of the skill. Returns amount of exp that was exceeding the maximum (potentially wasted).
    // Returns -1 usually. Returns 0 or positive number if that level is reached.
    int GainExp(float amount)
    {
        int level = Level();
        totalExp += amount;
        int newLevel = Level();
        if (newLevel != level)
        {
            System.out.println("Skill.GainExp: new level reached");
            return newLevel;
        }
        return -1;
    }

    public int TotalExp()
    {
        return (int) totalExp;
    }
    private float totalExp = 0;
    public void setTotalEXP(int toSet)
    {
        totalExp = toSet;
    }
    /// Max XP total from L0 to max level.
    public int EXPMax()
    {
        return EXPToLevelFrom0(99);
    }
    public int EXPToLevelFrom0(int level)
    {
        int total = 0;
        for (int i = 0; i < expRequired.length && i < level; ++i)
        {
            total += expRequired[i];
        }
    //    System.out.println("Exp to level "+level+" of skill "+text+": "+total);
        return total;
    }
    // Current level in this skill.
    public int Level()
    {
        float exp = totalExp;
        int level = 0;
        while (exp >= 0 && level < expRequired.length)
        {
            exp -= expRequired[level];
            if (exp >= 0)
                ++level;
        }
        return level;
    };
    int[] expRequired;


}
