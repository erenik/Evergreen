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
    craftingComplete, craftingProgressed, craftingFailedNullItem,
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
    MessageReceivedFromPlayer, // Player name, message contents.
    foundPlayerSelfLol,
    searchPlayer_alreadyFound, searchPlayers_success, searchPlayers_failed, playerParries,
    // Give and take.
    gaveItemToPlayer, receivedItemFromPlayer,
    gaveBlueprintToPlayer, receivedBlueprintFromPlayer,
    playerNotAliveAnymore,
    ensnared, ensnaredWoreOff, multiplies, sendPlayerResourcesFailed, playerDied, skillLeveledDown, playerDefeatedPlayer, obtainedItem, obtainedBlueprint, scoutFoundItems,
    targetStunned, entHatred, tailWhip, tailWhipMiss, breathAttack, breathAttackMiss, gaiaHeal, slashOfHeavens, slashOfHeavensPartial, recycled,
    wantsToFleeButCannot, couldNotFindPlayer, assaultsOfTheEvergreen, damageReflectedWhileAttacking, stumbledUpon, stumbledUponFood, stumbledUponMaterials, stumbledUponItem, wheelAccident, wheelOmen1, wheelOmen2,
    wheelOmen3, flyingOmen1, flyingOmen2, didNothing, SharedPlayerKnowledgeWithPlayer, PlayerSharedPlayerKnowledgeWithYou, AlreadyHasPlayerKnowledge,

}
