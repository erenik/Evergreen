package erenik.evergreen.common.logging;

/**
 * Created by Emil on 2017-01-28.
 */

public enum LogTextID {
    undefined, // To represent 0, those messages which have not been converted into LogTextID form yet.
    reduceEmissionsSuccessful,
    reduceEmissionsMostlySuccessful,
    reduceEmissionsNotSoSuccessful,
    reduceEmissionsFailed,
    scoutingSuccess,
    scoutingFailure,
    fledFromCombat,  // 1 arg, name of the fleer.
    playerFledFromCombat,
    playerTriedToFlee, // 1 arg, name of fleer
    triedToFlee,
    // 2 args, quantity and name of attacking monster type.
    shelterAttacked,
    // Max 1 encounter per scouting sessions? Number determines strength?
    scoutRandomEncounter,
    playerPlayerAttack, playerMonsterAttack, monsterPlayerAttack, playerPlayerAttackMiss, monsterPlayerAttackMiss, playerMonsterAttackMiss, playerVanquishedMonster, monsterKnockedOutPlayer,
    secondLife,

}
