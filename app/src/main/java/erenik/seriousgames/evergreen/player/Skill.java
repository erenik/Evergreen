package erenik.seriousgames.evergreen.player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Emil on 2016-10-30.
 */
public enum Skill {
    Foraging("Foraging", new int [] {5,10,15,20}, "Increases food acquired while foraging. Increases chance to find good foraging spots."),
    FleetRetreat("Fleet retreat", new int[] {5,9,13,17}, "Makes fleeing easier. Gains more EXP from encounters on successful retreats."),
    Survival("Survival", new int[] {7,9,11,13,15}, "Increases Max HP, HP recovery gain while using the Recover action."), // Add the HP recovery upon KO for regular mode later (when regular/Hardcore options are there).
    Architecting("Architecting", new int[]{10,20,30}, "Increases building speed when building shelter defenses and other structures"), // Add for Shelter additions later.
    MaterialEfficiency("Material efficiency", new int[]{5,10,20,40}, "Adds a chance to retain some materials that would otherwise have been spent. Saved materials reduce emissions generated as well."),
    Inventing("Inventing", new int[]{10,20,30}, "Improves success chance while inventing new items. Increases maximum bonuses that new inventions may have."), // Not added yet.
    DefensiveTraining("Defensive training", new int[]{5,10,15,20,25}, "Increases overall defense during combat. Increases survivability"),
    UnarmedCombat("Unarmed combat", new int[]{3,6,9,12,15,18}, "Increases attack, damage and amount of attacks while fighting unarmed."),
    WeaponizedCombat("Weaponized combat", new int[]{3,6,9,12,15,18}, "Increases attack and damage of attacks while using weapons."),
    Marksmanship("Marksmanship", new int[]{5,10,15,20,25,30}, "If you have a ranged weapon: Enables ranged attacks before melee combat starts. Increases ranged attack, damage and amount of attacks."), // Not added yet.
    Parrying("Parrying", new int[]{2,7,12,17,22,29}, "Enables parrying of melee attacks. Increases probability at higher levels."), // Not added yet.
    Thief("Thief", new int[]{5,10,15,20,25}, "Reduces risks and increases profits when stealing from other players"), // Not added yet.
    GroupCombatTraining("Group combat training", new int[]{5,10,15,20,25}, "Increases attack and defense while fighting with an ally."), // Not added yet.
    Studious("Studious", new int[]{20,30,40}, "Grants additional EXP each turn. Increases EXP gained when choosing the Study action"),
/*
    <item>Survival</item>
    <item>Architecting</item>
    <item>Material efficiency</item>
    <item>Inventing</item>
    <item>Defensive training</item>
    <item>Unarmed combat</item>
    <item>Weaponized combat</item>
    <item>Marksmanship</item>
    <item>Parrying</item>
    <item>Thievery</item>
*/
    ;
    Skill (String txt, int[] expRequired, String briefDescription)
    {
        this.text = txt;
        this.expRequired = expRequired;
        this.briefDescription = briefDescription;
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
        System.out.println("Exp to level "+level+" of skill "+text+": "+total);
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
