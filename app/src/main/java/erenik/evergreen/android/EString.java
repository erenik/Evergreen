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
            case playerPlayerAttack: id = R.string.playerPlayerAttack; break;
            case playerMonsterAttack: id = R.string.playerMonsterAttack; break;
            case monsterPlayerAttack: id = R.string.monsterPlayerAttack; break;
            case playerPlayerAttackMiss: id = R.string.playerPlayerAttackMiss; break;
            case monsterPlayerAttackMiss: id = R.string.monsterPlayerAttackMiss; break;
            case playerMonsterAttackMiss: id = R.string.playerMonsterAttackMiss; break;


            default:
                s = logTextID.name(); break;
        }
        if (id != -1)
            s = App.currentActivity.getString(id);
        // Replace args as needed.
        int replaced = 0;
        while(s.contains("(arg)"))
            s = s.replaceFirst("\\(arg\\)", args.get(replaced++));
        for (int i = replaced; i < args.size(); ++i) {
            s += ", Unused arg: "+args.get(i);
        }
        return s;
    }
}
