package erenik.evergreen.common.player;

/**
 * Created by Emil on 2016-11-11.
 */
public enum TransportStat {
    // Probability that this transport is chosen when randomizing transport each turn/day. No.
    EmissionsPerDay,
    ForagingBonus,
    MaterialGatheringBonus,
    SpeedBonus,
    FleeBonus,
    AmountEnemiesEncounteredRatio,
    SocialSupport, // Attack & Defense bonus from other passangers.
    ;

    public float DefaultValue() {
        switch (this) {
            default:
                return 0;
        }
    }
}
