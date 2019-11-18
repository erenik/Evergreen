package evergreen.common.player;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import evergreen.util.EList;
import evergreen.util.Printer;

/**
 * Created by Emil on 2016-10-30.
 */
public class Skill implements Serializable {
    private static final long serialVersionUID = 1L;

    SkillType st;
    /*
    Foraging("Foraging", AddLinear(5,5,4), "Increases food acquired while foraging. Increases chance to find good foraging spots."),
    FleetRetreat("Fleet retreat", AddLinearAccum(5, 4, 4), "Makes fleeing easier. Gains more EXP from encounters on successful retreats."),
    Survival("Survival", AddLinearAccum(5,5,5), "Increases Max HP, HP recovery gain while using the Recover action."), // Add the HP recovery upon KO for regular mode later (when regular/Hardcore options are there).
    Architecting("Architecting", AddLinear(10,10,6), "Increases building speed when building shelter defenses and other structures"), // Add for Shelter additions later.
    MaterialEfficiency("Material efficiency",  Quadratic(5, 2, 5), "Adds a chance to retain some materials that would otherwise have been spent. Saved materials reduce emissions generated as well."),
    Recycling("Recycling", AddLinear(5, 5, 5), "Reduces emission generation when crafting and inventing. Also increases efficiency of the Reduce Emissions daily actions"),
    Inventing("Inventing", AddLinear(10,10,8), "Improves success chance while inventing new items. Increases maximum bonuses that new inventions may have."), // Not added yet.
    Crafting("Crafting", AddLinear(5, 5, 5), "Improves success chance when crafting new items. Increases maximum bonuses that newly crafted items may receive."),
    DefensiveTraining("Defensive training", AddLinearAccum(5,5,7), "Increases overall defense during combat. Increases survivability"),
    UnarmedCombat("Unarmed combat", AddLinearAccum(3,2,9), "Increases attack, damage and amount of attacks while fighting unarmed."),
    WeaponizedCombat("Weaponized combat", AddLinearAccum(3,1,6), "Increases attack and damage of attacks while using weapons."),
    Marksmanship("Marksmanship", AddLinear(5,5,6), "If you have a ranged weapon: Enables ranged attacks before melee combat starts. Increases ranged attack, damage and amount of attacks."), // Not added yet.
    Parrying("Parrying", AddLinearAccum(2, 5, 6), "Enables parrying of melee attacks. Increases probability at higher levels."), // Not added yet.
    SilentScouting("Silent scouting", AddLinearAccum(10, 5, 5), "Reduces risks of scouting by lowering the monster encounter rate while scouting."),
    Thief("Thief", AddLinear(5, 5, 5), "Reduces risks and increases profits when stealing from other players"), // Not added yet.
    GroupCombatTraining("Group combat training", AddLinear(5,5,5), "Increases attack and defense while fighting with an ally."), // Not added yet.
    Studious("Studious", AddLinearAccum(20,10,5), "Grants additional EXP each turn. Increases EXP gained when choosing the Study action"),
*/
    ;

    Skill() {};
    Skill (SkillType st) {
        this.st = st;
    }
 
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
     //   Printer.out("Skill writeObject exp: "+totalExp);
        out.writeObject(st);
        out.writeFloat(totalExp);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        st = (SkillType) in.readObject();
        totalExp = in.readFloat();
       // Printer.out("Skill readObject exp: "+totalExp);
    }
    private void readObjectNoData() throws ObjectStreamException
    {
    }
    

    // Exp aquired in this skill towards the next level?
    public int EXPToNext() {
        int level = Level();
        int totalNeeded = EXPToLevelFrom0(Level() + 1);
//        Printer.out("totalNeeded: "+totalNeeded+" and totalExp: "+totalExp);
        return (int) (totalNeeded - this.totalExp);
    }

    public String Text() {
        return st.text;
    }

    public SkillType GetSkillType() {
        return st;
    }

    public String name() {
        return st.text;
    }

    public class LeveledUpException extends  Exception {
        // Exp wasted used if reaching max level.
        LeveledUpException(int levelReached, int expWasted) {
            super("Leveled up");
            this.levelReached = levelReached;
            this.expWasted = expWasted;
        };
        int expWasted = 0;
        int levelReached = 0;
    }

    //    // Throws exception on level up of the skill. Returns amount of exp that was exceeding the maximum (potentially wasted).
    // Returns -1 usually. Returns 0 or positive number if that level is reached.
    public int GainExp(float amount) {
        int level = Level();
        totalExp += amount;
        int newLevel = Level();
        if (newLevel != level) {
        //    Printer.out("Skill.GainExp: new level reached");
            return newLevel;
        }
        return -1;
    }

    public int TotalExp() {
        return (int) totalExp;
    }
    private float totalExp = 0;
    public void setTotalEXP(int toSet) {
        totalExp = toSet;
    }
    /// Max XP total from L0 to max level.
    public int EXPMax()
    {
        return EXPToLevelFrom0(99);
    }
    public int EXPToLevelFrom0(int level) {
        int total = 0;
        for (int i = 0; i < st.expRequired.length && i < level; ++i) {
            total += st.expRequired[i];
        }
    //    Printer.out("Exp to level "+level+" of skill "+text+": "+total);
        return total;
    }
    // Current level in this skill.
    public int Level() {
        float exp = totalExp;
        int level = 0;
        while (exp >= 0 && level < st.expRequired.length) {
            exp -= st.expRequired[level];
            if (exp >= 0)
                ++level;
        }
        return level;
    };

}
