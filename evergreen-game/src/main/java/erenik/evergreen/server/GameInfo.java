package erenik.evergreen.server;

import erenik.evergreen.Game;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.player.Stat;
import erenik.util.Printer;

/**
 * Created by Emil on 2017-04-19.
 */

public class GameInfo {
    public static void main(String[] args){

        EGTCPServer serv = new EGTCPServer();
        int numAIs = 5, iarg = 0;
        boolean emailsTurnSurvived = false,
                knownPlayers = false;
        for (int i = 0; i < args.length; ++i){
//            Printer.out("args "+i+": "+args[i]);
            // No args here.
            if (args[i].equals("-emailsTurnSurvived")) {
                emailsTurnSurvived = true;
            }
            if (args[i].equals("-knownPlayers")) {
                knownPlayers = true;
            }
            // 1 args below
            if (i >= args.length - 1)
                continue;
            String arg = args[i+1];
            try {
                iarg = Integer.parseInt(arg);
            } catch (NumberFormatException nfe){} // Silently ignore it.
            if (args[i].equals("-maxActivePlayers")) {
//                serv.maxActivePlayers = iarg;
  //              Log("Max active players set: "+serv.maxActivePlayers, INFO);
            }
        }
        // Load game?
        Game game = Game.Load("game_1.sav");
        if (game == null) {
            Printer.out("Unable to load game");
            return;
        }
        if (emailsTurnSurvived){
            for (int i = 0; i < game.players.size(); ++i){
                Player p = game.players.get(i);
                Printer.out(String.format("%10s", p.name) + " "
                        + String.format("%30s", p.email)+" "
                        +String.format("%5s", ""+(int)p.Get(Stat.TurnPlayed)));
            }
        }
        if (knownPlayers){
            for (int i = 0; i < game.players.size(); ++i){
                Player p = game.players.get(i);
                Printer.out("Player "+p.name+" known players: "+p.cd.knownPlayerNames.size());
                for (int j = 0; j < p.cd.knownPlayerNames.size(); ++j){
                    Printer.out("- "+p.cd.knownPlayerNames.get(j));
                }
            }
        }
    }
}
