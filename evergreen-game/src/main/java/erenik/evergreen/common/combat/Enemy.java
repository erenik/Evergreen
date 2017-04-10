package erenik.evergreen.common.combat;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.encounter.Encounter;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.util.Dice;
import erenik.util.Tuple;

/**
 * Created by Emil on 2016-10-30.
 */
public class Enemy extends Combatable {
//    static int turn = 0; // Should be set (copied to) before creating new enemies.
    private int turnCreated = 0;

    public Enemy(EnemyType type, float emissions, int turn) {
        turnCreated = turn;
        emissionsWhenCreated = emissions;
        name = type.name;
        this.enemyType = type;
        level = type.level;
        hp = maxHP = type.hp;
        attack = type.attack;
        defense = type.defense;
        exp = type.exp;
        type.UpdateDetails(this, turn);
        /// Scale up by emissions.
        attack += emissions / 12.5; // 8 times per 100
        defense += emissions / 20; // 5 times per 100
        attackDamage.bonus += (emissions / 50); // 2 times per 100.
    }

    public void OnTurnStart(Encounter enc){
        super.OnTurnStart(enc);
        hp += hpRegenPerTurn;
        // Multiply?
        if (multiplies > 0 && timesMultiplied < multiplies && enc.attackers.size() < 20) { // Don't multiply if there attacker number is still saturated.
            if (Dice.RollD6(2) >= 10) {
                ++timesMultiplied;
                Enemy e = new Enemy(enemyType, emissionsWhenCreated, turnCreated);
                e.isAttacker = isAttacker;
                e.multiplies = 0; // Don't let the duplicate multiply again or it will be infinite.
                enc.AddCombatant(e);
                enc.LogEnc(new Log(LogTextID.multiplies, LogType.ENC_INFO, name));
            }
        }
    }

    @Override
    protected void OnKilled(Combatable targetYouKilled) {

    }

    @Override
    protected void OnDied(Combatable attackerWhoKilledYou) {

    }

    @Override
    protected void OnDealtDamage(Combatable target, int damage) {
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
    protected void OnReceivedDamage(Combatable fromAttacker, int damage) {
        if (damageReflecting > 0){
            if (Dice.RollD6(2) + damageReflecting >= 12) {
                fromAttacker.InflictDamage(1, this);
            }
        }
    }
}
