package erenik.evergreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import erenik.evergreen.common.Player;
import erenik.evergreen.util.Json;
import erenik.evergreen.util.Tuple;

// import erenik.evergreen.android.App;

/**
 * Handles a specific game. It may either be a local game, global with many players,
 * or a small-group of specific players.
 *
 * Each game file keeps track of the players involved, time passed and some metrics.
 *
 * Created by Emil on 2016-12-10.
 */
public class Game
{
    int gameID;
    String name;
    private int updateIntervalMinutes = 0; // If non-0, updates to new turn every x minutes.

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

    public void NextDay()
    {
        // TODO: Add default player as needed? Elsewhere?
//        if (!players.contains(App.GetPlayer()))
  //          players.add(App.GetPlayer());
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

    public static List<Game> CreateDefaultGames()
    {
        List<Game> games = new ArrayList<>();
        games.add(Game.UpdatesEveryMinutes(1));
        games.add(Game.UpdatesEveryMinutes(60));
        games.add(Game.UpdatesEveryMinutes(1440));
        return games;
    }
    private static Game UpdatesEveryMinutes(int minutes) {
        Game g = new Game();
        g.updateIntervalMinutes = minutes;
        return g;
    }

    /** Brief JSON version for reviewing all games.
    * */
    public String toJsonBrief()
    {
        String s = "{" +
                    "\"gameID\":\""+gameID+"\"," +
                    "\"name\":\""+name+"\"," +
                    "\"updateIntervalMinutes\":\""+updateIntervalMinutes+"\","+
                    "\"numPlayers\":\""+players.size()+"\"" +
                        "}";
        return s;
    }

    public boolean parseFromJson(String line)
    {
       // try {
            Json j = new Json();
            j.Parse(line);
            List<Tuple<String,String>> tuples = j.Tuples();
            for (int i = 0; i < tuples.size(); ++i){
                String key = tuples.get(i).x;
                Object value = tuples.get(i).y;
                if (key.equals("gameID"))
                    gameID = (int) value;
                else if (key.equals("name"))
                    name = (String) value;
                else if (key.equals("updateIntervalMinutes"))
                    updateIntervalMinutes = (int) value;
                else if (key.equals("numPlayers"))
                    System.out.println("numPlayers "+value);
                else
                    System.out.println("Bad key-value pair: "+key+" value: "+value);
            }
       /* }
        catch (JSONException e) {
            e.printStackTrace();
            return false;
        }*/
        return true;
    }
}
