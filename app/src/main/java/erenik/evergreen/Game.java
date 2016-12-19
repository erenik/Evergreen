package erenik.evergreen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.player.Stat;
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
    GameID gameID = new GameID(-1, ""); // Contains name, id #, type string.
    private int updateIntervalSeconds = 0; // If non-0, updates to new turn every x minutes.

    /** Game that this player belongs to.
     *  0 - Local game. Backed up on server for practical purposes.
     *  1 - Global game. All players can interact with you.
     *  2 - Local Multiplayer game. Enter IP or other details to create ad-hoc connection.
     *  3-99 - Reserved.
     *  100-2000. Public game IDs. These games may have a password to join them.
     */
    public int GameID() { return gameID.id;   };


    /** List of players in the game */
    List<Player> players = new ArrayList<>();

    String fileName(){ return "game_"+gameID.id+".sav"; }
    /// Save data to file.
    boolean Save() {
        // Save ID.
        FileOutputStream file;
        ObjectOutputStream objectOut;
        try {
            file = new FileOutputStream(fileName());
            objectOut = new ObjectOutputStream(file);
            // Save num players.
            int numPlayers = players.size();
            objectOut.writeInt(numPlayers);
            for (int i = 0; i < players.size(); ++i)
            {
                Player p = players.get(i);
                objectOut.writeObject(p);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    /// Load data from file.
    boolean Load(String fromFile) {
        // Save ID.
        FileInputStream file;
        ObjectInputStream objectIn;
        try {
            file = new FileInputStream(fromFile);
            objectIn = new ObjectInputStream(file);
            // Load num players.
            players.clear();
            int numPlayers = objectIn.readInt();
            for (int i = 0; i < numPlayers; ++i)
            {
                Player player = new Player();
                try {
                    player = (Player) objectIn.readObject();
                    players.add(player);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
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
        if (players.size() == 0) {
            System.out.println("Game.NextDay: No players, doing nothing");
            return;
        }
        int activePlayers = ActivePlayers();
        System.out.println("Game.NextDay, "+gameID.name+" players "+activePlayers+"/"+players.size());
        for (int i = 0; i < players.size(); ++i)
        {
            Player p = players.get(i);
            if (p.isAI)
                continue;
            if (p.IsAlive() == false)
                continue;
            if (activePlayers < 3)
                System.out.println(p.Name()+" next day..");
            p.Adjust(Stat.HP, -0.2f); // Everybody is dying.
            p.ProcessMandatoryEvents(); // Process all mandatory events, such as battles, assuming the player didn't do so already earlier. (i.e. with Mini-games, choose equipment before, et al)
            p.NextDay();

        }
    }

    private int ActivePlayers() {
        int tot = 0;
        for (int i = 0; i < players.size(); ++i)
        {
            if (players.get(i).IsAlive() == false)
                continue;
            ++tot;
        }
        return tot;
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
//            System.out.println("Player "+i+" name equals? "+p.Name());
            if (p.Name().equals(name))
                return p;
        }
        return null;
    }

    public static List<Game> CreateDefaultGames()
    {
        List<Game> games = new ArrayList<>();
        games.add(Game.UpdatesEverySeconds(10, GameID.GlobalGame_10Seconds, "10 seconds"));
        games.add(Game.UpdatesEverySeconds(60, GameID.GlobalGame_60Seconds, "60 seconds"));
//        games.add(Game.UpdatesEveryMinutes(10, GameID.GlobalGame_10Minutes, "10 minutes"));
  //      games.add(Game.UpdatesEveryMinutes(60, GameID.GlobalGame_60Minutes, "60 minutes"));
        return games;
    }

    private static Game UpdatesEverySeconds(int seconds, int gameID, String gameName) {
        Game g = new Game();
        g.updateIntervalSeconds = seconds;
        g.gameID.id = gameID;
        g.gameID.name = gameName;
        return g;
    }
    private static Game UpdatesEveryMinutes(float minutes, int gameID, String gameName) {
        Game g = new Game();
        g.updateIntervalSeconds = (int) (minutes * 60);
        g.gameID.id = gameID;
        g.gameID.name = gameName;
        return g;
    }

    /** Brief JSON version for reviewing all games.
    * */
    public String toJsonBrief()
    {
        String s = "{" +
                    "\"gameID\":\""+gameID.id+"\"," +
                    "\"type\":\""+gameID.typeString+"\"," +
                    "\"name\":\""+gameID.name+"\"," +
                    "\"updateIntervalSeconds\":\""+updateIntervalSeconds+"\","+
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
                String strVal = (String) value;
                if (key.equals("gameID"))
                    gameID.id = (int) Integer.parseInt(strVal);
                else if (key.equals("name"))
                    gameID.name = (String) value;
                else if (key.equals("type"))
                    gameID.typeString = (String) value;
                else if (key.equals("updateIntervalSeconds"))
                    updateIntervalSeconds = (int) Integer.parseInt(strVal);
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
    // Adds player, saves all player data to file.
    public void AddPlayer(Player player) {
        players.add(player);
        // Save all players?
        Save();
    }

    int gameTimeMs = 0;
    public void Update(long milliseconds)
    {
        gameTimeMs += milliseconds;
        int thresholdMs = updateIntervalSeconds * 1000;
        if (gameTimeMs > thresholdMs) {        // check if next day should come.
            NextDay();
            // Save to file.
            Save();
            gameTimeMs -= thresholdMs;
        }
    }
}
