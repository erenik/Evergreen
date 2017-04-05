package erenik.evergreen.common.combat;

import erenik.util.Dice;

/**
 * Created by Emil on 2016-10-31.
 */
public enum EnemyType
{
    Shrub("Mutated Shrub",          0, 10, 6, 8, 1.0f, 1),
    Scavenger("Scavenger monster",  0, 7, 7, 7, 1.0f, 1),
    Rock("Rock monster",            1, 5, 12, 12, 1.0f, 3),
    Swarm("Swarm",                  1, 2, 11, 11, 1.5f, 3),
    Troll("Troll",                  2, 23, 17, 15, 0.5f, 9),
    Raptor("Raptor",                2, 12, 16, 11, 0.5f, 7),
    Dragon("Dragon",                3, 25, 20, 14, 0.25f, 20),
    Ent("Ent",                      3, 40, 17, 20, 0.25f, 25),
    GaiaProtector("Gaia protector", 4, 150, 30, 30, 0.01f, 100);
    EnemyType(String name, int level, int hp, int attack, int defense, float encounterAmount, int exp) {
        this.name = name;
        this.level = level; this.hp = hp;
        this.attack = attack; this.defense = defense;
        this.exp = exp;
        this.encounterAmount = encounterAmount;
    }
    void UpdateDetails(Enemy en, int turn){
        int turnBonus = (turn / 8) - level;
        switch(this) // Set up the additional attributes that are specific.
        {
            case Shrub:
                en.ensnaring += 1 + turnBonus;
                break;
            case Scavenger:
                en.initialAttackBonus = 1 + turnBonus;
                if (turn > 8)
                    en.initialDamageBonus += 1 + turn / 32;
                break;
            case Rock:
                en.damageReflecting += 1 + turnBonus;
                en.receivedDamageMultiplier = 0.5f;
                break;
            case Swarm:
                en.multiplies = 1;
                break;
            case Troll:
                en.attackDamage = new Dice(3, 1, 0);
                break;
            case Raptor:
                en.attackDamage = new Dice(3, 1, 0);
                break;
            case Dragon:
                en.attackDamage = new Dice(3, 1, 1);
                break;
            case Ent:
                en.attackDamage = new Dice(3, 1, 1);
                break;
            case GaiaProtector:
                en.attackDamage = new Dice(3, 2, 2);
                break;
        }
    }
    public String name;
    public int level;
    int hp;
    int attack;
    int defense;
    int exp;
    public float encounterAmount; // 1.0 for default (100%) of attack amount.
}
