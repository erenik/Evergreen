package erenik.seriousgames.evergreen.player;

/**
 * Created by Emil on 2016-11-02.
 */
public enum InventionStat
{
    QualityLevel(0),
    MaterialCost(5),
    AttackBonus(0),
    AttackDamageDiceType(6), // Should be 3 or 6. Used for both traps and weapons.
    AttackDamageDice(0), // 1 or more for weapons n stuff.
    AttackDamageBonus(0),
    DefenseBonus(0),
    BonusAttacks(0),
    ParryBonus(0),
    /// Ranged attacks.
    RangedAttackBonus(0), RangedDamageBonus(0),
    /// Tool stats
    HarvestBonus(0),
    ScavengingBonus(0),
    RecoveryBonus(0),
    ConstructionBonus(0),
    InventingBonus(0),
    ScoutingBonus(0),
    /// Shelter addition stats.
    GreenhouseProductivity(0),
    EscapePath(0),
    Crenelation(0),
    Recycler(0),
    // Vehicle upgrades.
    Catalyst(0),
    Compressor(0), // + Speed, + Flee
    ;
    public int defaultValue = 0;
    InventionStat(int defaultValue)
    {
        this.defaultValue = defaultValue;
    }
}
