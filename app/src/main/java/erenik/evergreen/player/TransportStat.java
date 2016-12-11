package erenik.evergreen.player;

/**
 * Created by Emil on 2016-11-11.
 */
public enum TransportStat {
    RandomProbability, // Probability that this transport is chosen when randomizing transport each turn/day.
    EmissionsPerDay,
    ForagingBonus,
    MaterialGatheringBonus,
    SpeedBonus,
    FleeBonus,
    AmountEnemiesEncounteredRatio,
    SocialSupport, // Attack & Defense bonus from other passangers.
    ;

    public float DefaultValue()
    {
        switch (this)
        {
            case RandomProbability:
                return 1;
            default:
                return 0;
        }
    }
}
