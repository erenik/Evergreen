package erenik.evergreen.common.player;

/**
 * Created by Emil on 2016-10-31.
 */
public enum Stat {
    ID(0), // Should be generated by server on start. 0 for localhost player.
    TurnPlayed(0),
    TurnSurvived(0), // # turns survived since last KO.
    TurnsDead(0), // How many turns the character/player has been dead. If this exceeds some threshold and nothing is done to revive the character, it may be removed permanently?
    /// Current stats.
    Lives(5), // Lives, depends on difficulty. Counts down on each KO.
    HP(10), MAX_HP(10),
    FOOD(5), FoodHotspot(0),
    MATERIALS(3), MaterialDepot(0),
    BASE_ATTACK(10),  // Attack and defense bonuses, based on? Permanent increases? Also later...?
    BASE_DEFENSE(10), // Base defense, i.e. 10? Might vary in the future based on difficulty?
    SHELTER_DEFENSE(1), SHELTER_DEFENSE_PROGRESS(0), // Level and progress towards next level.
    SPEED(1),
    AccumulatedEmissions(0), // Those generated through your actions.
    InheritedEmissions(0), // Those inherited when defeating a player and taking their belongings.
    AbandonedShelter(0), // Abandoned shelters to explore! Own ActiveAction for exploring it? - Harder foes than currently meeting with bigger rewards as well.
//    RANDOM_PLAYERS_SHELTERS(0),  // Random player shelters to be randomly allocated shortly (ask server to whom it belongs?) or postpone until later.
    NestsOfTheEvergreen(0), // Enemy strongholds that you've found.
    UNALLOCATED_EXP(20), // Free exp to be distributed among any skill when the player pleases.
    CurrentTransport(-1),
    EntHatred(0), // They don't like getting killed much.
//    RandomPlayerFound(0), // Random player to be found. Requires connection to server to get which one you find.
;


    Stat(float defaultValue)
    {
        this.defaultValue = defaultValue;
    }
    public float defaultValue;
}
