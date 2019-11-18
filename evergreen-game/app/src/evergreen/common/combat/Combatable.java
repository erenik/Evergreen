package evergreen.common.combat;

import java.util.Random;

// import evergreen.android.act.EncounterActivity;
import evergreen.common.Player;
import evergreen.common.encounter.Encounter;
import evergreen.common.logging.Log;
import evergreen.common.logging.LogTextID;
import evergreen.common.logging.LogType;
import evergreen.util.Dice;
import evergreen.util.EList;
import evergreen.util.Tuple;

/**
 * Created by Emil on 2016-10-30.
 */
public abstract class Combatable extends Object
{
    public float hp;
    public float maxHP;
    protected int attack;
    protected int defense;
    static private Random r;
    public int consecutiveFleeAttempts = 0;
    public int wantedToFleeButCouldNotDesperation = 0;
    protected float emissionsWhenCreated = 0;
    public boolean isAttacker = false; // Flag booleans that should be set upon entering a specific encounter. If not attacker, is defender.
    public boolean ranAway = false;
    public boolean runsAway = false; // IF true, will try to run away.
    public float runAwayAtHPPercentage = 0.25f; // IF a flee-er, default to run at 25%?
    public int fleeSkill = 0;
    public float fleeBonusFromTransport = 0;
    public int parry = 0;
    public int stunnedRounds = 0;
    private static float MaxParry = 14; // Out of 4-24?

    public abstract void PrepareForCombat(boolean defendingShelter);

    /// Attempts to attack this unit, returns true if it hits.
    boolean Attack(int attack) {
        int cr = defense - attack;
        int numDice = 2; // More dice will put the equilibrium very close to bleh.. not good.
        int diceType = 20; // 2-40, better than 6-36, more hits and misses in general.
        Dice dice = new Dice(diceType, numDice, 0); // 2d12, better spread.
        int min = numDice, max = numDice * diceType;
        float average = (min + max) / 2.f;
        // 2D6: 2-12 (12-2+1 -> 11 -> +/- 5 diff)
        // 3D6: 3-18 (18-3+1 -> 16 -> +/- 7.5 diff)
        // 4D6: 4-24 (24-4+1 -> 21 -> +/- 10 diff of Att/Def spectrum),
        int rollNeeded = (int)average + cr; // 28/2 = 14 + CR. 6 to 36 - 30 range, so u can hit even if def - att is at 14 but not 15?
        int roll = dice.Roll() + 1; // +1 bonus to incline it to hit more often than not. Many misses will maybe be boring.. ish?
        boolean hit = roll >= rollNeeded;
        return hit;
    }
    /* Inflicts damage. This may be adjusted by implementers, but should mostly apply it straight to HP.
       Returns the damage inflicted (>0)
    */
    int InflictDamage(int damage, Combatable attacker, Encounter enc) {
        damage *= receivedDamageMultiplier;
        float php = hp;
        int iDamage = Math.round(damage);
        if (iDamage < 1)
            iDamage = 1; // Minimum 1.
        hp -= iDamage;
        if (hp < 0)
            hp = 0;
        attacker.OnDealtDamage(this, iDamage, enc);
        OnReceivedDamage(attacker, iDamage, enc);
   //     Printer.out("Damage taken: "+damage+" hp reduced from "+php+" to "+hp);
        return iDamage;
    }

    protected abstract void OnKilled(Combatable targetYouKilled);
    protected abstract void OnDied(Combatable attackerWhoKilledYou);
    protected abstract void OnDealtDamage(Combatable target, int damage, Encounter enc);
    protected abstract void OnReceivedDamage(Combatable fromAttacker, int damage, Encounter enc);
    // By default, just call DefaultAttack(target,enc); Returns true if it kills the target.
    public abstract boolean Attack(Combatable target, Encounter enc);


    /*{
        Printer.out("Combatable.OnKilled, has to be overridden");
        new Exception().printStackTrace();
        System.exit(1);
    }*/

    /*{
        Printer.out("Combatable.OnDied, has to be overridden");
        new Exception().printStackTrace();
        System.exit(1);
    }*/

    public boolean SetName(String newName) {
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
    int CurrentDefense() {
        return defense - ensnared.size();
    }
    /// Current Attack value.
    int CurrentAttack() {
        int a = attack - ensnared.size();
        a += (hitsAttempted == 0? initialAttackBonus : 0); // Add attack bonus if it's the first attack, and there exists any such bonus..?
        a -= consecutiveFleeAttempts; // Decreases when trying to flee.
        a += wantedToFleeButCouldNotDesperation; // Add desperation bonus to attack value.
        return a;
    }

    void DoAttackRound(Combatable target, Encounter enc){
        for (int i = 0; i < attacksPerTurn; ++i)
            Attack(target, enc);
    }

    /// Would you hit? or miss?
    boolean AttackHit(Combatable target, int attackValue, boolean allowParry, Encounter enc){
        boolean hit = target.Attack(attackValue);
        // Evaluate if parrying occurs.
        if (hit && allowParry && target.parry > 0){
            // Roll some D6's..!
            float chanceToParry = target.parry + 6; // Max level is 6, + from gear, so cap it at like... 15 total?
            if (chanceToParry > MaxParry)
                chanceToParry = MaxParry; // Max 80% parry rate?
            boolean parried = Dice.RollD6(4) < chanceToParry; // 4-24, less than the chance to parry?
            if (parried) {
                enc.LogEnc(new Log(LogTextID.playerParries, LogType.INFO, target.name));
                hit = false;
            }
        }
        return hit;
    }

    /// Check HP?
    /// Order this enemy to attack the player. Returns true if it kills the target.
    public boolean DefaultAttack(Combatable target, Encounter enc) {
        boolean hit = AttackHit(target, CurrentAttack(), true, enc);
        ++hitsAttempted;
        if (!hit) {
            if (target.isPlayer && isPlayer) { // Both are players?
                enc.LogEnc(new Log(LogTextID.playerPlayerAttackMiss, LogType.PLAYER_ATTACK_MISS, name, target.name));
            }
            else
                enc.LogEnc(new Log(isPlayer? LogTextID.playerMonsterAttackMiss : LogTextID.monsterPlayerAttackMiss, isPlayer? LogType.ATTACK_MISS : LogType.ATTACKED_MISS, name, target.name));
            return false;
        }
        int damageDealt = target.InflictDamage(attackDamage.Roll(), this, enc);
  //      enc.LogEnc(new Log("Hit! "+damageDealt+" damage! HP now at "+target.hp, LogType.INFO));
        if (target.isPlayer && isPlayer) {
            enc.LogEnc(new Log(LogTextID.playerPlayerAttack, LogType.PLAYER_ATTACK, name, target.name, damageDealt+""));
        }
        else
            enc.LogEnc(new Log(isPlayer? LogTextID.playerMonsterAttack : LogTextID.monsterPlayerAttack, isPlayer? LogType.ATTACK : LogType.ATTACKED, name, target.name, ""+damageDealt));
        AfterDamageDealt(target, enc);
        return false;
    }

    protected void AfterDamageDealt(Combatable target, Encounter enc){
        if (target.hp  <= 0){ // Killed player?
            if (target.isPlayer && isPlayer)
                enc.LogEnc(new Log(LogTextID.playerDefeatedPlayer, LogType.INFO, name, target.name));
            else
                enc.LogEnc(new Log(
                        isPlayer? LogTextID.playerVanquishedMonster : LogTextID.monsterKnockedOutPlayer,
                        isPlayer? LogType.DEFEATED_ENEMY : LogType.DEFEATED,
                        name,
                        target.name));
            target.OnDied(this); // Do post-defeat actions.
            OnKilled(target);
        }
    }

  /*
  {
        if (target.hp  <= 0){ // Killed player?
            if (target.isPlayer && isPlayer)
                enc.LogEnc(new Log(LogTextID.playerDefeatedPlayer, LogType.INFO, name, target.name));
            else
                enc.LogEnc(new Log(
                        isPlayer? LogTextID.playerVanquishedMonster : LogTextID.monsterKnockedOutPlayer,
                        isPlayer? LogType.DEFEATED_ENEMY : LogType.DEFEATED,
                        name,
                        target.name));
            target.OnDied(this); // Do post-defeat actions.
            OnKilled(target);
            return true;
        }
    }
*/
    public String name = "NoName";
    public String password = "";
    protected boolean isPlayer = false; // True for player.
    public int exp = 1;
    // Most of these are used mainly by enemies, but may also be used by some player weapons perhaps?
    EnemyType enemyType = null;
    int level = 0; // Base fight stats
    int ensnaring = 0; // On attack, Roll 2D6, 11+ ensnared, losing 1 Att/Def for 2 turns.
    int initialAttackBonus = 0;
    int initialDamageBonus = 0; // Additional effects
    int damageReflecting = 0;
    int encounterModifierPercent = 0;
    int multiplies = 0; // Swarms
    int timesMultiplied = 0; // Swarms
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

    /// EList of status effects for this current fight.
    public EList<Tuple<Integer,Integer>> ensnared = new EList<Tuple<Integer, Integer>>(); // May be stacked, hence the list. 1st integer is Att/Def loss, 2nd is # of turns it is lost.

    float numDefeatedThisFightAttackBonus = 0, numDefeatedThisGameAttackBonus = 0;
    public Dice attackDamage = new Dice(1, 0, 1);
    Dice tailWhipDamage;
    Dice breathAttackDamage;

    public void OnTurnStart(Encounter enc){
        if (ensnared.size() > 0){
            for (int i = 0; i < ensnared.size(); ++i){
                Tuple<Integer,Integer> t = ensnared.get(i);
                --t.y;
                if (t.y <= 0) {
                    ensnared.remove(t);
                    --i;
                }
            }
            if (ensnared.size() == 0 && this instanceof Player) {
                Player pThis = (Player) this;
                pThis.LogInfo(LogTextID.ensnaredWoreOff, pThis.name);
            }
        }
        if (stunnedRounds >= 0)
            --stunnedRounds;
    }

};
