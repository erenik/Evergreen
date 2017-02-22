package erenik.evergreen.common.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// import erenik.evergreen.android.act.EncounterActivity;
import erenik.evergreen.common.encounter.Encounter;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.util.Dice;
import erenik.evergreen.util.Tuple;

/**
 * Created by Emil on 2016-10-30.
 */
public class Combatable extends Object
{
    public float hp;
    public float maxHP;
    public int attack;
    public int defense;
    static private Random r;
    public int consecutiveFleeAttempts = 0;
    protected float emissionsWhenCreated = 0;
    public boolean isAttacker = false; // Flag booleans that should be set upon entering a specific encounter. If not attacker, is defender.
    public boolean ranAway = false;
    public boolean runsAway = false; // IF true, will try to run away.
    public float runAwayAtHPPercentage = 0.25f; // IF a flee-er, default to run at 25%?
    public int fleeSkill = 0;
    public float fleeBonusFromTransport = 0;

    public void PrepareForCombat(boolean defendingShelter) {
    }

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
        float php = hp;
        hp -= damage;
        System.out.println("Damage taken: "+damage+" hp reduced from "+php+" to "+hp);
        if (hp <= 0) {
            hp = 0;
        }
        return damage;
    }
    public boolean SetName(String newName)
    {
        if (newName.contains(";"))
            return false;
        if (newName.contains("Any") || newName.contains("Contains"))
            return false;
        name = newName;
        return true;
    }
    public String Name() {
        return name;

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
        a -= consecutiveFleeAttempts; // Decreases when trying to flee.
        return a;
    }
    /// Apply poisons, regen, remove debuffs, etc.
    void NewTurn(Encounter enc)
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
            Enemy e = new Enemy(enemyType, emissionsWhenCreated);
            e.isAttacker = isAttacker;
            enc.AddCombatant(e);
        }
    }
    /// Check HP?
    /// Order this enemy to attack the player. Returns true if it kills the player.
    public boolean Attack(Combatable target, Encounter enc) {
        for (int i = 0; i < attacksPerTurn; ++i) {
            // Attack it? Will have bonus first round (first attempt).
            boolean hit = target.Attack(CurrentAttack());
            ++hitsAttempted;
            if (!hit)
            {
                if (target.isPlayer && isPlayer) { // Both are players?
                    enc.LogEnc(new Log(LogTextID.playerPlayerAttackMiss, LogType.PLAYER_ATTACK_MISS, name, target.name));
//                    enc.Log(name+" attacks "+target.name+" but misses.", LogType.ATTACK_MISS);
                }
                else
                    enc.LogEnc(new Log(isPlayer? LogTextID.playerMonsterAttackMiss : LogTextID.monsterPlayerAttackMiss, isPlayer? LogType.ATTACK_MISS : LogType.ATTACKED_MISS, name, target.name));
//                    enc.Log(isPlayer ? "You attack the " + target.name + " but miss." : "The " + name + " attacks you but misses.", isPlayer ? LogType.ATTACK_MISS : LogType.ATTACKED_MISS);
                continue;
            }
            int damageDealt = target.InflictDamage(attackDamage.Roll(), this);
            if (target.isPlayer && isPlayer) {
                enc.LogEnc(new Log(LogTextID.playerPlayerAttack, LogType.PLAYER_ATTACK, name, target.name, damageDealt+""));
//                enc.Log(name+" attacks "+target.name+" for "+damageDealt + " point" + (damageDealt > 1 ? "s" : "") + " of damage. Remaining HP: "+target.hp, LogType.ATTACK);
            }
            else
                enc.LogEnc(new Log(isPlayer? LogTextID.playerMonsterAttack : LogTextID.monsterPlayerAttack, isPlayer? LogType.ATTACK : LogType.ATTACKED, name, target.name, ""+damageDealt));
//                enc.Log(isPlayer ? "You attack the " + target.name + " for " + damageDealt + " point" + (damageDealt > 1 ? "s" : "") + " of damage." : "The " + name + " attacks you and deals " + damageDealt + " point" + (damageDealt > 1 ? "s" : "") + " of damage.",isPlayer ? LogType.ATTACK : LogType.ATTACKED);
            if (target.hp  < 0){ // Killed player?
                enc.LogEnc(new Log(isPlayer? LogTextID.playerVanquishedMonster : LogTextID.monsterKnockedOutPlayer, isPlayer? LogType.DEFEATED_ENEMY : LogType.DEFEATED, name, target.name));
                return true;
            }
        }
        return false;
    }

    public String name = "NoName";
    public String password = "";
    protected boolean isPlayer = false; // True for player.
    public int exp = 1;
    // Most of these are used mainly by enemies, but may also be used by some player weapons perhaps?
    EnemyType enemyType = null;
    int level = 0; // Base fight stats
    int ensnaring = 0;
    int initialAttackBonus = 0;
    int initialDamageBonus = 0; // Additional effects
    int damageReflecting = 0;
    int encounterModifierPercent = 0;
    int multiplies = 0;
    int timesMultiply = 0;
    int timesMultiplied = 0;
    int vehicleAttackBonus = 0;
    int fleePenalty = 0;
    public int attacksPerTurn = 1;
    int itemBribable = 0;
    int materialBribable = 0;
    float attackChance = 1.f, tailWhipChance = 0, breathAttackChance = 0,
            healChance = 0.f, spellChance = 0.f, hpRegenPerTurn = 0.f,
        receivedDamageMultiplier = 1.f;
    /// Attacks performed this combat?
    int hitsAttempted = 0;

    /// List of status effects for this current fight.
    List<Tuple<Integer,Integer>> ensnared = new ArrayList<Tuple<Integer, Integer>>(); // May be stacked, hence the list.

    float numDefeatedThisFightAttackBonus = 0, numDefeatedThisGameAttackBonus = 0;
    public Dice attackDamage = new Dice(1, 0, 1);
    Dice tailWhipDamage;
    Dice breathAttackDamage;
};
