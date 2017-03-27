package erenik.evergreen;

import erenik.evergreen.common.Player;
import erenik.util.EList;

// import erenik.evergreen.android.App;
// import erenik.evergreen.common.Player;

/**
 * Class for step-wise simulation of the game world, including players, transactions, strongholds, fights, etc. Between each step, actions, skills and choices should be submitted to the simulator for proper output?
 * Created by Emil on 2016-11-09.
 */
public class Simulator
{
    /// Singleton.
    private static Simulator s = new Simulator();
    public static Simulator getSingleton()
    {
        return s;
    }

    EList<Game> games = new EList<>();
    // Local game for singleplayer. Default has some NPCs, or no?
    Game localGame = new Game();

    private Simulator()
    {
//        game.AddDefaultAI();
    }

    /// Review all games, see if they should proceed to the next day/turn.
    public void ProcessGames()
    {
        /// If needed, reload the list of games first perhaps?
        LoadGames();
    }
    /// Loads all games from their separate files. Each game file keeps track of the players involved, time passed and some metrics.
    void LoadGames()
    {

    }

    /// Adjusts stats, generates events based on chosen actions to be played, logged. Returns amount of player characters simulated.
    public int RequestNextDay(Player requestingPlayer, boolean isLocalGame) {
        System.out.println("Simulator.RequestNextDay: Requesting player: "+requestingPlayer);
        /// Local game?
        if (isLocalGame){
            // Simulate it?
            if (localGame.players.indexOf(requestingPlayer) == -1)
                localGame.players.add(requestingPlayer);
            localGame.NextDay(); // Yeah.
            return 1;
        }
        /// Multiplayer, send data to server, request an update.


        /// No games in? Add default game and default player.
        if (games.size() == 0) {
            games.add(new Game());
            // TODO: Add default player elsewhere.
   //         games.get(0).players.add(App.GetPlayer());
        }
        /// Check if the games are supposed to proceed to the next day.
        int total = 0;
        for (int i = 0; i < games.size(); ++i) {
            Game game = games.get(i);
            total += game.NextDay();
        }
        return total;
    }

}
