package erenik.seriousgames.evergreen.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import erenik.seriousgames.evergreen.logging.*;
import erenik.seriousgames.evergreen.util.Tuple;
import erenik.seriousgames.evergreen.act.encounter;
import erenik.seriousgames.evergreen.util.Dice;

/**
 * Created by Emil on 2016-10-30.
 */
public class Combatable
{
    public float hp;
    public float maxHP;
    public int attack;
    public int defense;
    static private Random r;

    /// Attempts to attack this unit, returns true if it hits.
    boolean Attack(int attack)
    {
        int cr = defense - attack;
        int min = 4, max = 24;
        float average = (min + max) / 2.f;
        // 2D6: 2-12 (12-2+1 -> 11 -> +/- 5 diff)
        // 3D6: 3-18 (18-3+1 -> 16 -> +/- 7.5 diff)
        // 4D6: 4-24 (24-4+1 -> 21 -> +/- 10 diff of Att/Def spectrum)
        int rollNeeded = (int)average + cr;
        int roll = Dice.RollD6(4) + 1; // +1 bonus to incline it to hit more often than not. Many misses will maybe be boring.. ish?
        boolean hit = roll >= rollNeeded;
        return hit;
    }
    /* Inflicts damage. This may be adjusted by implementers, but should mostly apply it straight to HP.
       Returns the damage inflicted (>0)
    */
    int InflictDamage(int damage, Combatable attacker)
    {
        if (damageReflecting > 0)
        {
            if (Dice.RollD6(2) >= 11)
                attacker.InflictDamage(1, this);
        }
        damage *= receivedDamageMultiplier;
        hp -= damage;
        if (hp <= 0) {
            hp = 0;
        }
        return damage;
    }
    // Current Defense value.
    int CurrentDefense()
    {
        return defense - ensnared.size();
    }
    /// Current Attack value.
    int CurrentAttack()
    {
        int a = attack - ensnared.size();
        a += (hitsAttempted == 0? initialAttackBonus : 0); // Add attack bonus if it's the first attack, and there exists any such bonus..?
        return a;
    }
    /// Apply poisons, regen, remove debuffs, etc.
    void NewTurn()
    {
        // Count down the snares/binds.
        for (int i = 0; i < ensnared.size(); ++i) {
            Tuple<Integer, Integer> t = ensnared.get(i);
            ++t.x;
            if (t.x >= t.y) {
                ensnared.remove(t);
                --i;
            }
        }
        hp += hpRegenPerTurn;
        // Multiply?
        if (multiplies > 0 && timesMultiplied < timesMultiply)
        {
            Enemy e = new Enemy(enemyType);
            encounter.enemies.add(e);
        }
    }
    /// Check HP?
    public boolean IsAlive()
    {
        return hp > 0;
    }
    /// Order this enemy to attack the player. Returns true if it kills the player.
    public boolean Attack(Combatable target)
    {
        for (int i = 0; i < attacksPerTurn; ++i)
        {
            // Attack it? Will have bonus first round (first attempt).
            boolean hit = target.Attack(CurrentAttack());
            ++hitsAttempted;
            if (!hit)
            {
                encounter.Log(isPlayer? "You attack the "+target.name+" but miss." : "The "+name+" attacks you but misses.",
                        isPlayer? LogType.ATTACK_MISS : LogType.ATTACKED_MISS);
                continue;
            }
            int damageDealt = target.InflictDamage(attackDamage.Roll(), this);
            encounter.Log(isPlayer? "You attack the "+target.name+" for "+damageDealt+" points of damage." : "The "+name+" attacks you and deals "+damageDealt+" points of damage.",
                    isPlayer? LogType.ATTACK : LogType.ATTACKED);
            if (!target.IsAlive()) // Killed player?
            {
                return true;
            }
        }
        return false;
    }

    public String name = "NoName";
    protected boolean isPlayer = false; // True for player.
    public int exp = 1;
    // Most of these are used mainly by enemies, but may also be used by some player weapons perhaps?
    EnemyType enemyType = null;
    int level = 0, // Base fight stats
        ensnaring = 0, initialAttackBonus = 0, initialDamageBonus = 0, // Additional effects
        damageReflecting = 0, encounterModifierPercent = 0,
            multiplies = 0, timesMultiply = 0, timesMultiplied = 0,
        vehicleAttackBonus = 0, fleePenalty = 0, attacksPerTurn = 1,
        itemBribable = 0, materialBribable = 0;
    float attackChance = 1.f, tailWhipChance = 0, breathAttackChance = 0,
            healChance = 0.f, spellChance = 0.f, hpRegenPerTurn = 0.f,
        receivedDamageMultiplier = 1.f;
    /// Attacks performed this combat?
    int hitsAttempted = 0;

    /// List of status effects for this current fight.
    List<Tuple<Integer,Integer> > ensnared = new ArrayList<Tuple<Integer, Integer>>(); // May be stacked, hence the list.

    float numDefeatedThisFightAttackBonus = 0, numDefeatedThisGameAttackBonus = 0;
    public Dice attackDamage = new Dice(1, 0, 1);
    Dice tailWhipDamage;
    Dice breathAttackDamage;
};
