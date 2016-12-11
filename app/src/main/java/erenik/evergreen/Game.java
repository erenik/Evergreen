package erenik.evergreen;

import java.util.ArrayList;
import java.util.List;

import erenik.evergreen.android.App;

/**
 * Handles a specific game. It may either be a local game, global with many players,
 * or a small-group of specific players.
 *
 * Each game file keeps track of the players involved, time passed and some metrics.
 *
 * Created by Emil on 2016-12-10.
 */
public class Game {
    int gameID;
    /** Game that this player belongs to.
     *  0 - Local game. Backed up on server for practical purposes.
     *  1 - Global game. All players can interact with you.
     *  2 - Local Multiplayer game. Enter IP or other details to create ad-hoc connection.
     *  3-99 - Reserved.
     *  100-2000. Public game IDs. These games may have a password to join them.
     */
    public int GameID() { return gameID;   };


    /** List of players in the game */
    List<Player> players = new ArrayList<>();

    /// Save data to file.
    boolean Save() {
        return true;
    }
    /// Load data from file.
    boolean Load(){

        return true;
    }

    public void AddDefaultAI() {
        // Some default NPC-players?
        players.add(Player.NewAI("Mad Max"));
        players.add(Player.NewAI("Mad Marvin"));
    }

    public void NextDay() {
        if (!players.contains(App.GetPlayer()))
            players.add(App.GetPlayer());
        System.out.println("Simulator.NextDay");
        for (int i = 0; i < players.size(); ++i)
        {
            Player p = players.get(i);
            if (p.isAI)
                continue;
            System.out.println(p.Name()+" next day..");
            p.NextDay();
        }
    }
    /// Booleans default false, flag the one you wish to search with, both may be used simultaneously.
    public Player GetPlayer(String name, boolean contains, boolean startsWith)
    {
        System.out.println("Looking for "+name);
        for (int i = 0; i < players.size(); ++i)
        {
            Player p = players.get(i);
            if (contains && p.Name().contains(name)) {
                System.out.println("Player "+i+" contains? "+p.Name());
                return p;
            }if (startsWith && p.Name().startsWith(name))
        {
            System.out.println("Player "+i+" startsWith? "+p.Name());
            return p;
        }
        }
        return null;
    }
    public Player GetPlayer(String name)
    {
        for (int i = 0; i < players.size(); ++i)
        {
            Player p = players.get(i);
            System.out.println("Player "+i+" name equals? "+p.Name());
            if (p.Name().equals(name))
                return p;
        }
        return null;
    }

}
