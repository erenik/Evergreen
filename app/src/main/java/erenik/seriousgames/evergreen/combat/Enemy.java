package erenik.seriousgames.evergreen.combat;

import erenik.seriousgames.evergreen.combat.Combatable;
import erenik.seriousgames.evergreen.combat.EnemyType;

;

/**
 * Created by Emil on 2016-10-30.
 */
public class Enemy extends Combatable
{
    static int turn = 0; // Should be set (copied to) before creating new enemies.
    static int emissions = 0; // Should be set (copied to) before creating new enemies.
    public Enemy(EnemyType type)
    {
        name = type.name;
        this.enemyType = type;
        level = type.level;
        hp = maxHP = type.hp;
        attack = type.attack;
        defense = type.defense;
        exp = type.exp;
        int turnBonus = (turn / 8) - level;

        switch(type) // Set up the additional attributes that are specific.
        {
            case Shrub: ensnaring += 1 + turnBonus; break;
            case Scavenger: initialAttackBonus = 1 + turnBonus; if (turn > 32) initialDamageBonus = 1; break;
            case Rock: damageReflecting += 1 + turnBonus; receivedDamageMultiplier = 0.5f; break;
            case Swarm: multiplies = 1; break;
        }
        /// Scale up by emissions.
        attack += emissions / 10;
        defense += emissions / 20;
        attackDamage.bonus += (emissions / 50); // Scale up bonus for damage.
    }
}