package erenik.evergreen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Random;

import erenik.evergreen.common.Enumerator;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.auth.Auth;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.player.Stat;
import erenik.util.EList;
import erenik.util.FileUtil;
import erenik.util.Json;
import erenik.util.Tuple;

// import erenik.evergreen.android.App;

/**
 * Handles a specific game. It may either be a local game, global with many players,
 * or a small-group of specific players.
 *
 * Each game file keeps track of the players involved, time passed and some metrics.
 *
 * Created by Emil on 2016-12-10.
 */
public class Game implements Serializable {
    private static final long serialVersionUID = 1L;
    public Game(){
        logEnumerator = new Enumerator();
        Log.logIDEnumerator = logEnumerator; // Set it.
    }
    GameID gameID = new GameID(-1, ""); // Contains name, id #, type string.
    private int updateIntervalSeconds = 0; // If non-0, updates to new turn every x minutes.
    private static String lastError = ""; // Updated as needed, not saved.

    Enumerator logEnumerator = null;

    /** Game that this player belongs to.
     *  0 - Local game. Backed up on server for practical purposes.
     *  1 - Global game. All players can interact with you.
     *  2 - Local Multiplayer game. Enter IP or other details to create ad-hoc connection.
     *  3-99 - Reserved.
     *  100-2000. Public game IDs. These games may have a password to join them.
     */
    public int GameID() { return gameID.id;   };


    /** EList of players in the game */
    public EList<Player> players = new EList<>();

    public void ProcessQueuedActiveActions(){
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            p.ProcessQueuedActiveActions(this);
        }
    }

    /// Save data to file.
    boolean Save() {
        // Save ID.
        FileOutputStream file;
        ObjectOutputStream objectOut;
        try {
            file = new FileOutputStream(fileName());
            objectOut = new ObjectOutputStream(file);
            writeTo(objectOut);
            objectOut.close(); // Close the stream so that it actually saves to the file?
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        // Try load straight away.
        Load(fileName());
        return true;
    }
    /// Load data from file.
    static public Game Load(String fromFile) {
        // Save ID.
        FileInputStream file;
        ObjectInputStream objectIn;
        try {
            file = new FileInputStream(fromFile);
            objectIn = new ObjectInputStream(file);
            Game game = new Game();
            if (!game.readFrom(objectIn))
                return null;
            objectIn.close(); // Close the SCHTREAM!
            return game;
        } catch (FileNotFoundException fnfe){
            lastError = "File not found.";
            return null;
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeTo(java.io.ObjectOutputStream out) throws IOException {
  //      System.out.println("Game writeObject");
        out.writeObject(gameID);
        out.writeInt(updateIntervalSeconds);
        logEnumerator.writeTo(out);
//        System.out.println("logEnumerator saved at "+logEnumerator.value);
        // Save num players.
        int numPlayers = players.size();
        out.writeInt(numPlayers);
        for (int i = 0; i < players.size(); ++i)
        {
            Player p = players.get(i);
            p.sendAll = Player.SEND_ALL; // Save everything by default.
            p.sendLogs = Player.SEND_ALL; // Send all logs to file.
            p.writeTo(out);
//            System.out.println("Player "+p.name+" last log messages");
  //          Log.PrintLastLogMessages(p.log, 5);
        }
    }
    private boolean readFrom(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
//        System.out.println("Game readObject");
        gameID = (GameID) in.readObject();
        updateIntervalSeconds =  in.readInt();
        logEnumerator.readFrom(in);
//        System.out.println("logEnumerator loaded to "+logEnumerator.value);
        // Load num players.
        players = new EList<>();
        int numPlayers = in.readInt();
//        System.out.println("Game readObject - before players");
        for (int i = 0; i < numPlayers; ++i) {
            Player player = new Player();
            try {
//                System.out.println("Game readObject - player "+i);
                if (player.readFrom(in)) {
                    players.add(player);
//                    System.out.println("Loaded player "+player.name);
                }
                if (player.sendAll != Player.SEND_ALL) {
                    System.out.println("Incomplete player saved. WTF?");
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }
        }
//        System.out.println("Loaded "+players.size()+" players");
        return true;
    }

    public void AddDefaultAI() {
        // Some default NPC-players?
        players.add(Player.NewAI("Mad Max"));
        players.add(Player.NewAI("Mad Marvin"));
    }


    /// returns num of player characters simulated.
    public int NextDay() {
        Log.logIDEnumerator = logEnumerator; // Set log enumerator for this session.

        // TODO: Add default player as needed? Elsewhere?
//        if (!players.contains(App.GetPlayer()))
  //          players.add(App.GetPlayer());
        if (players.size() == 0) {
            Log("Game.NextDay: No players, doing nothing");
            return 0;
        }
        int activePlayers = ActivePlayers();
        if (activePlayers == 0){
            Log("Game.NextDay, "+gameID.name+" players "+activePlayers+"/"+players.size()+", skipping since 0 active players.");
            return 0;
        }
        if (IsLocalGame()){ // Local game, check when last new-day was pressed..? Demand at least 1 min?
            System.out.println("Check time?");
        }
        else if (UpdatesSinceLastDay() == 0){
            Log("Game.NextDay, "+activePlayers+" active players, but skipping since no update has happened since last day. GameID: "+GameID());
            return 0;
        }
        LogAndPrint("Game.NextDay, "+gameID.name+" players "+activePlayers+"/"+players.size());
        int numSimulated = 0;
        for (int i = 0; i < players.size(); ++i)
        {
            Player p = players.get(i);
            if (p.isAI)
                continue;
            if (p.IsAlive() == false)
                continue;
            if (activePlayers < 3)
                System.out.println(p.Name()+" next day..");
            ++numSimulated;
            p.Adjust(Stat.HP, -0.2f); // Everybody is dying.
            p.NextDay(this);
            p.lastEditSystemMs = System.currentTimeMillis();
            // Save?
/*            if (p.lastSaveTimeSystemMs < p.lastEditSystemMs) {
                System.out.println("SAving again yow");
                p.SaveLog();
            }
  */      }
        return numSimulated;
    }

    private boolean IsLocalGame() {
        return GameID() == GameID.LocalGame;
    }

    /// Player updates since last day.
    private int UpdatesSinceLastDay() {
        int updates = 0;
        for (int i = 0; i < players.size(); ++i){
            updates += players.get(i).updatesFromClient.size();
        }
        return updates;
    }

    public int ActivePlayers() {
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
    public Player GetPlayer(String name, boolean contains, boolean startsWith) {
        if (name == null)
            return null;
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
    public Player GetPlayer(String name) {
        if (name == null){
            System.out.println("Player.GetPlayer with null name, wtf");
            return null;
        }
        name = name.trim();
        for (int i = 0; i < players.size(); ++i) {
            Player p = players.get(i);
//            System.out.println("Player "+i+" name equals? "+p.Name());
            if (p.Name().equals(name))
                return p;
            if (p.Name().compareToIgnoreCase(name) == 0){
                return p;
            }
        }
        return null;
    }

    public static EList<Game> CreateDefaultGames()
    {
        EList<Game> games = new EList<>();
//        games.add(Game.UpdatesEverySeconds(10, GameID.GlobalGame_10Seconds, "10 seconds"));
        games.add(Game.UpdatesEverySeconds(60, GameID.GlobalGame, "60 seconds")); // Changed ID to be the default one.
//        games.add(Game.UpdatesEveryMinutes(10, GameID.GlobalGame_10Minutes, "10 minutes"));
  //      games.add(Game.UpdatesEveryMinutes(60, GameID.GlobalGame_60Minutes, "60 minutes"));

        for (int i = 0; i < games.size(); ++i)
            games.get(i).CreateDefaultPlayers();
        return games;
    }
    private void CreateDefaultPlayers() {
        // Create default players?
        Player player = new Player();
        player.name = "Erenik";
        player.isAI = false;
        player.email = "emil_hedemalm@hotmail.com";
        player.password = Auth.Encrypt("1234", Auth.DefaultKey);
        player.gameID = GameID();
        System.out.println("Adding default player");
        AddPlayer(player);
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
            EList<Tuple<String,String>> tuples = j.Tuples();
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
        player.gameID = this.GameID();
        // Save all players?
        Save();
    }

    int gameTimeMs = 0;
    public void Update(long milliseconds) {
        gameTimeMs += milliseconds;
        int thresholdMs = updateIntervalSeconds * 100; // Should be * 1000
        if (gameTimeMs > thresholdMs) {        // check if next day should come.
            if (NextDay() != 0)
                Save(); // Save to file.
            gameTimeMs -= thresholdMs;
        }
    }
    private void Log(String msg){
        FileUtil.AppendWithTimeStampToFile("logs", "game"+GameID()+".txt", msg);
        System.out.println("Log: "+msg);
    }
    private void LogAndPrint(String msg){
        Log(msg);
        System.out.println(msg);
    }
    // Returns null if all known player names are already known.
    public Player RandomPlayer(EList<String> knownPlayerNames) {
        /// 10 random chances.
        if (players.size() == 0) {
            System.out.println("Game has 0 players. WAT");
            System.exit(444);
            return null;
        }
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < 10; ++i) {
            int playerIndex = r.nextInt(players.size());
  //          System.out.println("player index: "+playerIndex);
            Player randPlayer = players.get(playerIndex);
            boolean alreadyKnown = false;
            for (int j = 0; j < knownPlayerNames.size(); ++j) {
                String name = randPlayer.name;
                String name2 = knownPlayerNames.get(j);
//                System.out.println(name+" =? "+name2);
                if (name.equals(name2)) {
                    alreadyKnown = true;
                    break;
                }
            }
            if (alreadyKnown)
                continue;
            // Not known? Use it.
            return randPlayer;
        }
        return null;
    }

    public EList<Player> GetCharacters(String email, String password) {
        EList<Player> alp = new EList<>();
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            System.out.println(p.email+" == "+email+"? "+p.email.equals(email)+" "
                    +p.password+" == "+password+"? "+p.password.equals(password)+ " len: "+p.password.length()+" "+password.length());
            if (p.email.equals(email) && p.password.equals(password))
                alp.add(p);
        }
        return alp;
    }

    public EList<Player> GetCharacters(String email) {
        EList<Player> alp = new EList<>();
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            System.out.println(p.email+" == "+email+"? "+p.email.equals(email));
            if (p.email.equals(email))
                alp.add(p);
        }
        return alp;
    }

    public static String DefaultPath(int forID) {
        return "game_"+forID+".sav";
    }

    String fileName(){ return DefaultPath(gameID.id); }

    public static Game LocalGame() {
        Game game = new Game();
        game.gameID.id = GameID.LocalGame;
        return game;
    }
}
