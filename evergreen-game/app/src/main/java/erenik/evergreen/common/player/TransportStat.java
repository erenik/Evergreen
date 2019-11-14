package erenik.evergreen.common.player;

/**
 * Created by Emil on 2016-11-11.
 */
public enum TransportStat {
    EmissionsPerDay, // Probability that this transport is chosen when randomizing transport each turn/day. No.
    ForagingBonus, // + berries gathered per hour
    MaterialGatheringBonus, // + materials gathered per hour
    SpeedBonus, // + area covered when scouting, looking for players, etc.
    FleeBonus, // + to flee in combats.
    AmountEnemiesEncounteredRatio, // +% enemies encountered when out scouting, looking for players, etc.
    SocialSupport, // +Attack & Defense bonus from other passengers.
    Weight // The weight used when weighing all transports together. E.g. Idle has lower to not reduce all transports' effects totally.
    ;

    public float DefaultValue() {
        switch (this) {
            default:
                return 0;
        }
    }
}
