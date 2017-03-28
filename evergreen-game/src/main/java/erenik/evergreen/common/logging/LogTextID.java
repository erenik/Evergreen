package erenik.evergreen.common.logging;

/**
 * Created by Emil on 2017-01-28.
 */

public enum LogTextID {
    undefined, // To represent 0, those messages which have not been converted into LogTextID form yet.
    debug,
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
    // Starting bonuses
    startingBonusFood,
    startingBonusMaterials,
    startingBonusWeapon,
    startingBonusArmor,
    startingBonusTool,
    startingBonusInventions, startingBonusItem,

    newDayPlayerTurnPlayed, transportOfTheDay,
    starvingLostHP, diedOfStarvation,
    tooManyDailyActionsLossOfTime,
    foragingFood, gatherMaterials, recoverRecovered,
    unableToFindPlayerByName,
    studiesEXP,
    attackPlayer, attackedByPlayer,
    stealFailedDetected, playerTriedToStealFromYouFailedDetected, stealFailed, somethingAmiss, stealSuccess_whatName, stolen,
    searchPlayerFailed, foundPlayer,
    searchPlayer_foundAnother,
    craftingComplete, craftingProgressed,
    inventFailed, inventingOldThoughts, inventSuccess,
    scoutFoodStashes, scoutMatStashes,
    buildDefensesProgress, defensesReachedLevel,
    materialShortageAffectingProgress,
    skillLeveledUp,
    encounterNumMonsters, encounterSurvived, expGained,
    startingBonusNone,
    ReceivedPlayerResources, // Player, quantity, type,
    SentPlayerResources, // Player, quantity, type,
    MessageSentToPlayer, // Player name only,
    MessageReceivedFromPlayer,
    foundPlayerSelfLol,
    searchPlayer_alreadyFound,  // Player name, message contents.

}
