package evergreen.common.player;

import evergreen.util.EList;

/**
 * Created by Emil on 2017-04-09.
 */

public enum SkillType {
    Foraging("Foraging", AddLinear(5,5,9), "Increases food acquired while foraging. Increases chance to find good foraging spots."),
    Scavenging("Scavenging", AddLinear(5,5,9), "Increases materials acquired while looking for materials"),
    FleetRetreat("Fleet retreat", AddLinearAccum(5, 4, 4), "Makes fleeing easier. Gains more EXP from encounters on successful retreats."),
    Survival("Survival", AddLinearAccum(5,5,9), "Increases Max HP, HP recovery gain while using the Recover action."), // Add the HP recovery upon KO for regular mode later (when regular/Hardcore options are there).
    Architecting("Architecting", AddLinear(10,10,6), "Increases building speed when building shelter defenses and other structures"), // Add for Shelter additions later.
    MaterialEfficiency("Material efficiency",  Quadratic(5, 2, 5), "Adds a chance to retain some materials that would otherwise have been spent. Saved materials reduce emissions generated as well."),
    Recycling("Recycling", AddLinear(5, 5, 5), "Reduces emission generation when crafting and inventing. Also increases efficiency of the Reduce Emissions daily actions"),
    Inventing("Inventing", AddLinear(10,10,8), "Improves success chance while inventing new items. Increases maximum bonuses that new inventions may have."), // Not added yet.
    Crafting("Crafting", AddLinear(5, 5, 5), "Improves success chance when crafting new items. Increases maximum bonuses that newly crafted items may receive."),
    DefensiveTraining("Defensive training", AddLinearAccum(5,5,7), "Increases overall defense during combat. Increases survivability"),
    UnarmedCombat("Unarmed combat", AddLinearAccum(3,2,9), "Increases attack, damage and amount of attacks while fighting unarmed."),
    WeaponizedCombat("Weaponized combat", AddLinearAccum(3,1,6), "Increases attack and damage of attacks while using weapons."),
    Parrying("Parrying", AddLinearAccum(2, 5, 6), "Enables parrying of melee attacks. Increases probability at higher levels."), // Not added yet.
    SilentScouting("Silent scouting", AddLinearAccum(10, 5, 5), "Reduces risks of scouting by lowering the monster encounter rate while scouting."),
    Thief("Thief", AddLinear(5, 5, 5), "Reduces risks and increases profits when stealing from other players"), // Not added yet.
    Studious("Studious", AddLinearAccum(20,10,5), "Grants additional EXP each turn. Increases EXP gained when choosing the Study action")
    // To add later again perhaps.
    //    Marksmanship("Marksmanship", AddLinear(5,5,6), "If you have a ranged weapon: Enables ranged attacks before melee combat starts. Increases ranged attack, damage and amount of attacks."), // Not added yet.
    //   GroupCombatTraining("Group combat training", AddLinear(5,5,5), "Increases attack and defense while fighting with an ally."), // Not added yet.
    ;

    public String text = "text";
    public String briefDescription = "Desc";
    int[] expRequired;

    SkillType (String txt, int[] expRequired, String briefDescription)
    {
        this.text = txt;
        this.expRequired = expRequired;
        this.briefDescription = briefDescription;
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

    public static EList<String> Names() {
        EList<String> l = new EList<String>();
        for (int i = 0; i < values().length; ++i) {
            l.add(values()[i].text);
        }
        return l;
    }

    public static SkillType GetFromString(String s) {
        for (int i = 0; i < SkillType.values().length; ++i) {
            if (SkillType.values()[i].text.equals(s))
                return SkillType.values()[i];
        }
        return null;
    }


    public int MaxLevel() {
        return expRequired.length;
    }
}
