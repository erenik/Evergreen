package erenik.evergreen.common.Invention;

/**
 * Created by Emil on 2016-11-02.
 */
public enum InventionStat {
    /* Quality level of this invention. Generally varies between 0 at the start to 4 at maximum?
    * */
    QualityLevel(0),
    Blueprint(0), // If 1, is a blueprint/invention, otherwise it is an actual object.
    MaterialCost(5),
    ProgressRequiredToCraft(5),
    /** Once an invention has been invented, this number keeps track of iterated times the same item would have been invented. Upon reaching 10xQualityLevel, the next QualityLevel of the same item is invented.
     *  This continues until all invention levels of the same item has been invented. :)
     */
    TimesInvented(0), //     public int timesInvented = 0;
    // Weapon stats
    // Ordinal of InventionType. -1 is the default, bad  type.
    Type(-1),
    // For weapon, this means the WeaponType,
    // for RangedWeapons it would mean the RangedWeaponType,
    // for Tools, the ToolType etc.
    // IF -1, is bad, should be randomized before proceeding to details.
    SubType(-1),
    Equipped(-1), // non-negative to signify which player has it equipped. 0 for localhost player, 1+ for multiplayer player games.
    AdditionalEffect(-1),
    IsRanged(0), // If 1, is true.
    AttackBonus(0),
    AttackDamageDiceType(6), // Set it always?
    AttackDamageDice(0), // 1 or more for weapons n stuff.
    AttackDamageBonus(0),
    DefenseBonus(0),
    BonusAttacks(0),
    ParryBonus(0), // Works how?
    /// Ranged attacks.
    RangedAttackBonus(0), RangedDamageBonus(0),
    /// Tool stats
    HarvestBonus(0), // +food per Foraging,
    ScavengingBonus(0), // +Material per Material search,
    RecoveryBonus(0), // + HP per turn?
    ConstructionBonus(0), // + building progress.
    InventingBonus(0), // + inventing bonus.
    ScoutingBonus(0), // + scouting.
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
