package erenik.evergreen.android;

import java.util.List;

import erenik.evergreen.R;
import erenik.evergreen.common.logging.LogTextID;

/**
 * Created by Emil on 2017-02-21.
 */

public class EString {
    public static String GetLogText(LogTextID logTextID, List<String> args) {
        int id = -1; // id on android.
        String s = "";
        switch(logTextID) {
            case debug: s = "Debug: (arg)"; break;
            case reduceEmissionsSuccessful: id = R.string.reduceEmissionsSuccessful; break;
            case reduceEmissionsMostlySuccessful: id = R.string.reduceEmissionsMostlySuccessful; break;
            case reduceEmissionsNotSoSuccessful: id = R.string.reduceEmissionsNotSoSuccessful; break;
            case reduceEmissionsFailed: id = R.string.reduceEmissionsFailed; break;
            case scoutingSuccess: id = R.string.scoutingSuccess; break;
            case scoutingFailure: id = R.string.scoutingFailure; break;
            case fledFromCombat: id = R.string.fledFromCombat; break;
            case playerFledFromCombat: id = R.string.playerFledFromCombat; break;
            case playerTriedToFlee: id = R.string.playerTriedToFlee; break;
            case triedToFlee: id = R.string.triedToFlee; break;
            case shelterAttacked: id = R.string.shelterAttacked; break;
            case scoutRandomEncounter: id = R.string.scoutRandomEncounter; break;
            case scoutFoodStashes: id = R.string.scoutFoodStashes; break;
            case scoutMatStashes: id = R.string.scoutMatStashes; break;
            case buildDefensesProgress: id = R.string.buildDefensesProgress; break;
            case defensesReachedLevel: id = R.string.defensesReachedLevel; break;
            case materialShortageAffectingProgress: id = R.string.materialShortage; break;
            case skillLeveledUp: id = R.string.skillLeveledUp; break;

            case playerPlayerAttack: id = R.string.playerPlayerAttack; break;
            case playerMonsterAttack: id = R.string.playerMonsterAttack; break;
            case monsterPlayerAttack: id = R.string.monsterPlayerAttack; break;
            case playerPlayerAttackMiss: id = R.string.playerPlayerAttackMiss; break;
            case monsterPlayerAttackMiss: id = R.string.monsterPlayerAttackMiss; break;
            case playerMonsterAttackMiss: id = R.string.playerMonsterAttackMiss; break;
            case playerVanquishedMonster: id = R.string.playerVanquishedMonster; break;
            case monsterKnockedOutPlayer: id = R.string.monsterKnockedOutPlayer; break;
            case secondLife: id = R.string.secondLife; break;
            case startingBonusFood: id = R.string.startingBonusFood; break;
            case startingBonusMaterials: id = R.string.startingBonusMaterials; break;
            case startingBonusWeapon:  // All 3 have the same text pretty much.
            case startingBonusArmor:
            case startingBonusTool: id = R.string.startingBonusItem; break;
            case startingBonusInventions: id = R.string.startingBonusInventions; break;
            case newDayPlayerTurnPlayed: id = R.string.newDayPlayerTurnPlayed; break;
            case transportOfTheDay: id = R.string.transportOfTheDay; break;
            case starvingLostHP: id = R.string.starvingLostHP; break;
            case diedOfStarvation: id = R.string.diedOfStarvation; break;
            case tooManyDailyActionsLossOfTime: id = R.string.tooManyDailyActions; break;
            case foragingFood: id = R.string.foragingFood; break;
            case gatherMaterials: id = R.string.gatherMaterials; break;
            case recoverRecovered: id = R.string.recoverRecovered; break;
            case unableToFindPlayerByName: id = R.string.unableToFindPlayerByName; break;
            case studiesEXP: id = R.string.studiesEXP; break;
            case attackPlayer: id = R.string.attackPlayer; break;
            case attackedByPlayer: id = R.string.attackedByPlayer; break;
            case stealFailedDetected: id = R.string.stealFailedDetected; break;
            case playerTriedToStealFromYouFailedDetected: id = R.string.playerTriedToStealFromYouFailedDetected; break;
            case stealFailed: id = R.string.stealFailed; break;
            case somethingAmiss: id = R.string.somethingAmiss; break;
            case stealSuccess_whatName: id = R.string.stealSuccess_whatName; break;
            case stolen: id = R.string.stolen; break;
            case searchPlayerFailed: id = R.string.searchPlayerFailed; break;
            case foundPlayer: id = R.string.foundPlayer; break;
            case searchPlayer_foundAnother: id = R.string.searchPlayer_foundAnother; break;
            case craftingComplete: id = R.string.craftingComplete; break;
            case craftingProgressed: id = R.string.craftingProgressed; break;
            case inventFailed: id = R.string.inventFailed; break;
            case inventingOldThoughts: id = R.string.inventingOldThoughts; break;
            case inventSuccess: id = R.string.inventSuccess; break;
            case encounterNumMonsters: id = R.string.encounterNumMonsters; break;
            case encounterSurvived: id = R.string.encounterSurvived; break;
            case expGained: id = R.string.expGained; break;
            case startingBonusNone: id = R.string.startingBonusNone; break;

            default:
                s = logTextID.name(); break;
        }
        if (id != -1)
            s = App.currentActivity.getString(id);
        // Replace args as needed.
        int replaced = 0;
        while(s.contains("(arg)") && replaced < args.size())
            s = s.replaceFirst("\\(arg\\)", args.get(replaced++));
        for (int i = replaced; i < args.size(); ++i) {
            s += ", Unused arg: "+args.get(i);
        }
        return s;
    }
}
