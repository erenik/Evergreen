package evergreen.common.combat;

import evergreen.common.Player;
import evergreen.common.encounter.Encounter;
import evergreen.common.logging.Log;
import evergreen.common.logging.LogTextID;
import evergreen.common.logging.LogType;
import evergreen.common.player.Stat;
import evergreen.util.Dice;
import evergreen.util.Printer;
import evergreen.util.Tuple;

/**
 * Created by Emil on 2016-10-30.
 */
public class Enemy extends Combatable {
//    static int turn = 0; // Should be set (copied to) before creating new enemies.
    private int turnCreated = 0;
    private int breathAttackCooldown = 3;

    public Enemy(EnemyType type, float emissions, int turn, Encounter enc) {
        turnCreated = turn;
        emissionsWhenCreated = emissions;
        name = type.name;
        this.enemyType = type;
        level = type.level;
        hp = maxHP = type.hp;
        defense = type.defense;
        exp = type.exp;
        /// Scale up by emissions.
        defense += emissions / 20; // 5 times per 100
        UpdateStats(enc);
    }

    // Called when? Each turn of combat? Yaaaas.
    public void OnTurnStart(Encounter enc){
        super.OnTurnStart(enc);
        if (hp <= 0) {
//            Printer.out("Ghost ent...");
            return;
        }
        hp += hpRegenPerTurn;
        // Multiply?
        if (multiplies > 0 && timesMultiplied < multiplies && enc.attackers.size() < 20) { // Don't multiply if there attacker number is still saturated.
            if (Dice.RollD6(2) >= 10) {
                ++timesMultiplied;
                Enemy e = new Enemy(enemyType, emissionsWhenCreated, turnCreated, enc);
                e.isAttacker = isAttacker;
                e.multiplies = 0; // Don't let the duplicate multiply again or it will be infinite.
                enc.AddCombatant(e);
                enc.LogEnc(new Log(LogTextID.multiplies, LogType.ENC_INFO, name));
            }
        }
        UpdateStats(enc);
    }

    private void UpdateStats(Encounter enc) {
        UpdateBaseDamage();
        attack = enemyType.attack;
        attack += emissionsWhenCreated / 12.5; // 8 times per 100
        attack += (4 - level) + turnCreated / 10; // Increase attack for each turn that has elapsed as well?
        int turnBonus = (turnCreated / 8) - level;
        switch (enemyType) {
            case Shrub:
                ensnaring = 1 + turnBonus;
                break;
            case Scavenger:
                initialAttackBonus = 1 + turnBonus;
                if (turnCreated > 8)
                    initialDamageBonus += 1 + turnCreated / 32;
                break;
            case Rock:
                damageReflecting = 1 + turnBonus;
                receivedDamageMultiplier = 0.5f;
                break;
            case Swarm:
                multiplies = 1;
                break;
            case Troll: // Extra dice?
                attackDamage.dice += 1; // And number of dice.
                attackDamage.diceType += 2; // Increase dice type.
                attackDamage.bonus += 1; // n add some bonus.
                break;
            case Raptor:
                attackDamage.diceType += 1; // Only slightly bigger dice.
                attacksPerTurn = 2;
                break;
            case Dragon:
                attackDamage.bonus += 1;
                attackDamage.diceType += 3;
                hpRegenPerTurn = 0.5f;
                break;
            case Ent:
                attackDamage.diceType += 4;
                attackDamage.dice += 1; // = new Dice(3, 2, 0); // 2D3
                float bonus = enc.GetTotal(Stat.EntHatred); // Increase attack with ent-hatred.
                attack += bonus / 3; // Oh yeah.
                attackDamage.bonus += bonus / 7; // For each 5 ents killed, increase damage of attacks as well.
                hpRegenPerTurn = 1;
                break;
            case GaiaProtector:
                attackDamage = new Dice(3, 2, 4);
                hpRegenPerTurn = 2;
                if (hp > 75)
                    attacksPerTurn = 1;
                else if (hp > 25) // More attacks at lower HP.
                    attacksPerTurn = 2;
                else
                    attacksPerTurn = 3;
                break;
            case ToxicOoze:
                receivedDamageMultiplier = 0.25f;
                attackDamage.dice += 1;
                attackDamage.bonus += 1;
                hpRegenPerTurn = 1;
                break;
            case RockGolem:
                attackDamage.dice += 2;
                attackDamage.diceType += 2;
                attackDamage.bonus += 2;
                hpRegenPerTurn = 1;
                break;
        }
    }

    private void UpdateBaseDamage() {
        int emissions = (int) emissionsWhenCreated;
        /// Base damage for all mobs is always just 1.
        if (emissions < 25)
            attackDamage = new Dice(1, 1, 0); // 1 base
        else if (emissions < 50)
            attackDamage = new Dice(2, 1, 0); // 1d2
        else if (emissions < 75)
            attackDamage = new Dice(3, 1, 0); // 1d3
        else if (emissions < 100)
            attackDamage = new Dice(4, 1, 0); // 1d4
        else if (emissions < 125)
            attackDamage = new Dice(6, 1, 0); // 1d6
        else if (emissions < 150)
            attackDamage = new Dice(8, 1, 1); // 1d8+1
        else if (emissions < 175)
            attackDamage = new Dice(8, 1, 2); // 1d8+2
        else if (emissions < 200)
            attackDamage = new Dice(8, 1, 2); // 1d8+2
        else if (emissions < 250)
            attackDamage = new Dice(10, 1, 3); // 1d10+3
        else
            attackDamage = new Dice(12, 1, 4); // 1d12+4
        if (emissions > 25)
           ; // Printer.out("Base damage set to: "+attackDamage);
    }

    @Override
    public void PrepareForCombat(boolean defendingShelter) {
        // Uhhh... do nothing?
    }

    @Override
    protected void OnKilled(Combatable targetYouKilled) {

    }

    @Override
    protected void OnDied(Combatable attackerWhoKilledYou) {
        if (attackerWhoKilledYou instanceof Player) {
            Player player = (Player) attackerWhoKilledYou;
            if (enemyType == EnemyType.Ent) {
                player.Adjust(Stat.EntHatred, 1);
                player.Log(LogTextID.entHatred, LogType.ENC_INFO);
            }
        }
    }

    @Override
    protected void OnDealtDamage(Combatable target, int damage, Encounter enc) {
        if (ensnaring > 0){ // Ensnaring shrubs..!
            if (Dice.RollD6(2) >= 11){
                target.ensnared.add(new Tuple<Integer,Integer>(1, 2));
                if (target instanceof Player){
                    Player p = (Player) target;
                    p.LogInfo(LogTextID.ensnared, target.name, name);
                }
            }
        }
    }

    @Override
    protected void OnReceivedDamage(Combatable fromAttacker, int damage, Encounter enc) {
        if (damageReflecting > 0){
            if (Dice.RollD6(2) + damageReflecting >= 12) {
                int diceType = 1 + damageReflecting / 3; // Scale up damage reflectance as its strength increases
                Dice d = new Dice(diceType, 1, 0); // Take 1-X damage, based on reflectance.
                int damageToTake = d.Roll();
                fromAttacker.InflictDamage(damageToTake, this, enc);
                enc.LogEnc(new Log(LogTextID.damageReflectedWhileAttacking, LogType.ENC_INFO, name, fromAttacker.name, ""+damageToTake));
            }
        }
    }

    // Return true if it kills the target.
    @Override
    public boolean Attack(Combatable target, Encounter enc) {
        switch (enemyType){
            case Dragon: {
                if (breathAttackCooldown > 0)
                    --breathAttackCooldown;
                int roll = Dice.RollD6(1);
                switch (roll) {
                    default:
                    case 0: DefaultAttack(target, enc); // 50% chance reuglar attack.
                        break;
                    case 1:  // 33% Tail-whip!
                    case 2: {
                        boolean hit = AttackHit(target, CurrentAttack() + 2, true, enc);
                        if (hit) {
                            Dice newDice = attackDamage.copy();
                            newDice.dice += 1; // Like base attack, but more dice.
                            newDice.bonus += 1;
                            int damage = newDice.Roll();
                            enc.LogEnc(new Log(LogTextID.tailWhip, LogType.ATTACKED, name, target.name, "" + damage));
                            target.InflictDamage(damage, this, enc);
                            AfterDamageDealt(target, enc); // To get messages logged properly.
                        } else {
                            enc.LogEnc(new Log(LogTextID.tailWhipMiss, LogType.ATTACKED_MISS, name, target.name));
                        }
                        break;
                    }
                    case 3: // 16% Breath attack
                        if (breathAttackCooldown > 0){
                            DefaultAttack(target, enc);
                            break;
                        }
                        boolean hit = AttackHit(target, CurrentAttack() + 4, false, enc);
                        if (hit) {
                            Dice newDice = attackDamage.copy();
                            newDice.dice += 2; // Like base attack, but more dice n bonus.
                            newDice.bonus += 2;
                            int damage = newDice.Roll();
                            enc.LogEnc(new Log(LogTextID.breathAttack, LogType.ATTACKED, name, target.name, "" + damage));
                            target.InflictDamage(damage, this, enc);
                            AfterDamageDealt(target, enc); // To get messages logged properly.
                        } else {
                            enc.LogEnc(new Log(LogTextID.breathAttackMiss, LogType.ATTACKED_MISS, name, target.name));
                        }
                        breathAttackCooldown = 3; // Wait like 1 round at least between breaths.
                        break;
                }
                break;
            }
            case GaiaProtector: {
                int roll = Dice.RollD6(1);
                switch (roll){
                    default:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        DefaultAttack(target, enc);
                        break;
                    case 5:
                        int healed = Dice.RollD3(3);
                        enc.LogEnc(new Log(LogTextID.gaiaHeal, LogType.ENC_INFO, name, ""+healed));
                        break;
                    case 6:
                        boolean hit = AttackHit(target, CurrentAttack() + 10, true, enc);
                        int damage = Dice.RollD3(4)+12;
                        if (hit){
                            enc.LogEnc(new Log(LogTextID.slashOfHeavens, LogType.ATTACKED, name, target.name, ""+damage));
                            target.InflictDamage(damage, this, enc);
                        } else {
                            damage /= 3;
                            enc.LogEnc(new Log(LogTextID.slashOfHeavensPartial, LogType.ATTACKED, name, target.name, ""+damage));
                            target.InflictDamage(damage, this, enc);
                        }
                        AfterDamageDealt(target, enc); // To get messages logged properly.
                        break;
                }
                break;
            }
            default:
                return DefaultAttack(target,enc);
        }
        return target.hp <= 0;
    }
}
