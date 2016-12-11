package erenik.evergreen;

import java.util.ArrayList;
import java.util.List;

import erenik.evergreen.android.App;
import erenik.evergreen.Player;

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

    List<Game> games = new ArrayList<>();
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

    /// Adjusts stats, generates events based on chosen actions to be played, logged
    public void NextDay()
    {

        /// Check if the games are supposed to proceed to the next day.
        for (int i = 0; i < games.size(); ++i)
        {
            Game game = games.get(i);
            game.NextDay();
        }
    }

}
