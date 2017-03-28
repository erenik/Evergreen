package erenik.evergreen.common.player;

/**
 * Created by Emil on 2017-03-24.
 */

public enum Config {
    /// Config stats.
    /*  0 - Easiest, 10% progress lost upon death (all stats n skills).
        1 - Easy, 20%,
        2 - Medium, 35% loss,
        3 - Hard, 50% loss,
        4 - Unforgiving, 75% loss.
        5 - Wipeout, 100%, hardcore
     */
    Difficulty(0),
    Avatar(0), // The profile picture they choose on character creation.
    StartingBonus(0), // What bonus to start with.
    CreationTime(0), // Time of creation saved as a float.
    RunAway(0.25f), // Default HP-percentage when the player wants to run for his/her life to survive combats (if applicable).
    LatestLogMessageIDSeen(0),
    ;
    Config(float defaultValue){ this.defaultValue = defaultValue; };
    public float defaultValue;
}
