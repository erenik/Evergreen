package erenik.evergreen.common.combat;

/**
 * Created by Emil on 2016-10-30.
 */
public class Enemy extends Combatable
{
    static int turn = 0; // Should be set (copied to) before creating new enemies.
    public Enemy(EnemyType type, float emissions)
    {
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
}
