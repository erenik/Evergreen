package erenik.evergreen.common.player;

/**
 * Created by Emil on 2016-10-31.
 */
public enum Stat
{
    TurnSurvived(0),
    HP(10), MAX_HP(10),
    FOOD(5), FoodHotspot(0),
    MATERIALS(3), MaterialDepot(0),
    BASE_ATTACK(10), ATTACK_BONUS(0), // Attack and defense bonuses, based on? Permanent increases? Also later...?
    BASE_DEFENSE(10), DEFENSE_BONUS(0), // Base defense, i.e. 10? Might vary in the future based on difficulty?
    SHELTER_DEFENSE(1), SHELTER_DEFENSE_PROGRESS(0), // Level and progress towards next level.
    SPEED(1),
    EMISSIONS(0),
    ABANDONED_SHELTER(0), // Abandoned shelters to explore! Own event for it.
    ENCOUNTERS(0),  // Random enemy encounters to defeat. Encountered while doing other tasks (scouting, gathering).
    ATTACKS_OF_THE_EVERGREEN(0),
    RANDOM_PLAYERS_SHELTERS(0),  // Random player shelters to be randomly allocated shortly (ask server to whom it belongs?) or postpone until later.
    ENEMY_STRONGHOLDS(0), // Enemy strongholds that you've found.
    UNALLOCATED_EXP(20), // Free exp to be distributed among any skill when the player pleases.
    CurrentTransport(-1),
    RandomPlayerFound(0), // Random player to be found. Requires connection to server to get which one you find.
    ;

    Stat(float defaultValue)
    {
        this.defaultValue = defaultValue;
    }
    public float defaultValue;
}
