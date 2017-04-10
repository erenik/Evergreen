package erenik.evergreen.common.combat;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.encounter.Encounter;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.player.Stat;
import erenik.util.Dice;
import erenik.util.Tuple;

/**
 * Created by Emil on 2016-10-30.
 */
public class Enemy extends Combatable {
//    static int turn = 0; // Should be set (copied to) before creating new enemies.
    private int turnCreated = 0;

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
        attackDamage.bonus += (emissions / 50); // 2 times per 100.
        UpdateStats(enc);
    }

    // Called when? Each turn of combat? Yaaaas.
    public void OnTurnStart(Encounter enc){
        super.OnTurnStart(enc);
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
            case Troll:
                attackDamage = new Dice(4, 1, 1);
                break;
            case Raptor:
                attackDamage = new Dice(3, 1, 0);
                attacksPerTurn = 2;
                break;
            case Dragon:
                attackDamage = new Dice(3, 1, 1);
                hpRegenPerTurn = 0.5f;
                break;
            case Ent:
                attackDamage = new Dice(3, 2, 0); // 2D3
                float bonus = enc.GetTotal(Stat.EntHatred); // Increase attack with ent-hatred.
                if (bonus > 0) {
                    System.out.println("Ent hatred");
                }
                attack += bonus; // Oh yeah.
                attackDamage.bonus += bonus / 5; // For each 5 ents killed, increase damage of attacks as well.
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
        }
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
                fromAttacker.InflictDamage(1, this, enc);
            }
        }
    }

    // Return true if it kills the target.
    @Override
    public boolean Attack(Combatable target, Encounter enc) {
        switch (enemyType){
            case Dragon: {
                int roll = Dice.RollD6(1);
                switch (roll) {
                    default:
                    case 0: DefaultAttack(target, enc); // 50% chance reuglar attack.
                        break;
                    case 1:  // 33% Tail-whip!
                    case 2: {
                        boolean hit = AttackHit(target, CurrentAttack() + 3, true, enc);
                        if (hit) {
                            int damage = Dice.RollD3(2);
                            enc.LogEnc(new Log(LogTextID.tailWhip, LogType.ATTACKED, name, target.name, "" + damage));
                            target.InflictDamage(damage, this, enc);
                        } else {
                            enc.LogEnc(new Log(LogTextID.tailWhipMiss, LogType.ATTACKED_MISS, name, target.name));
                        }
                        break;
                    }
                    case 3: // 16% Breath attack
                        boolean hit = AttackHit(target, CurrentAttack() + 5, false, enc);
                        if (hit) {
                            int damage = Dice.RollD3(3);
                            enc.LogEnc(new Log(LogTextID.breathAttack, LogType.ATTACKED, name, target.name, "" + damage));
                            target.InflictDamage(damage, this, enc);
                        } else {
                            enc.LogEnc(new Log(LogTextID.breathAttackMiss, LogType.ATTACKED_MISS, name, target.name));
                        }
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
