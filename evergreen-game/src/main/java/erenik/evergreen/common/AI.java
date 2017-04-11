package erenik.evergreen.common;

import java.util.Date;

import erenik.evergreen.common.player.Action;
import erenik.evergreen.common.player.DAction;
import erenik.evergreen.common.player.Stat;
import erenik.util.Printer;

/**
 * Created by Emil on 2017-04-11.
 */

public class AI {

    public AI(int type){
        this.type = type;
    };

    public static final int Erenik = 1; // Just recover..?
    int type;

    public void Update(Player player){
        switch (type){
            case Erenik:
                // Only recover.
                player.cd.dailyActions.clear();
                player.cd.dailyActions.add(new Action(DAction.Recover, null));
                player.cd.dailyActions.add(new Action(DAction.ReduceEmissions, null));
                Printer.out("Attack: "+player.BaseAttack()+" HP: "+player.HP()+"/"+player.MaxHP()+" emissionsAccum: "+player.Get(Stat.AccumulatedEmissions)+" inherited: "+player.Get(Stat.InheritedEmissions));
                player.updatesFromClient.add(new Date()); // Simulate updates from client to progress the game.
                break;
            default:

        }
    }

}
