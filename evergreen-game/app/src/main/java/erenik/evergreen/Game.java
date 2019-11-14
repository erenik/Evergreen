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
import java.util.Locale;
import java.util.Random;

import erenik.evergreen.common.AI;
import erenik.evergreen.common.Enumerator;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.auth.Auth;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.player.Config;
import erenik.evergreen.common.player.Difficulty;
import erenik.evergreen.common.player.Skill;
import erenik.evergreen.common.player.SkillType;
import erenik.evergreen.common.player.Stat;
import erenik.evergreen.common.player.Statistic;
import erenik.util.Dice;
import erenik.util.EList;
import erenik.util.FileUtil;
import erenik.util.Json;
import erenik.util.Printer;
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
    /// Sent along as first argument with EGPacket. If bad, should tell clients to update their games.
    public static int GAME_VERSION = 2;

    private static final long serialVersionUID = 1L;
    public static int secondsPerDay = 60; // The hard-coded. Default 1 minute per round, can be set via command-line arg in Server code now, -simulationTime
    public static boolean noPauses = false;
    private int day = 0;
    public static int intervalToPrintGameStatusSeconds = 300;

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
  //      Printer.out("Game writeObject");
        out.writeObject(gameID);
        out.writeInt(day);
        out.writeLong(dayStartTimeMs); // Start time of the latest day-determines when next day should be evaluated.
        out.writeInt(updateIntervalSeconds);
        logEnumerator.writeTo(out);
//        Printer.out("logEnumerator saved at "+logEnumerator.value);
        // Save num players.
        int numPlayers = players.size();
        out.writeInt(numPlayers);
        for (int i = 0; i < players.size(); ++i)
        {
            Player p = players.get(i);
            p.sendAll = Player.SEND_ALL; // Save everything by default.
            p.sendLogs = Player.SEND_ALL; // Send all logs to file.
            p.writeTo(out);
//            Printer.out("Player "+p.name+" last log messages");
  //          Log.PrintLastLogMessages(p.log, 5);
        }
    }
    private boolean readFrom(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
//        Printer.out("Game readObject");
        gameID = (GameID) in.readObject();
        day = in.readInt();
        dayStartTimeMs = in.readLong();
        updateIntervalSeconds =  in.readInt();
        logEnumerator.readFrom(in);
//        Printer.out("logEnumerator loaded to "+logEnumerator.value);
        // Load num players.
        players = new EList<>();
        int numPlayers = in.readInt();
//        Printer.out("Game readObject - before players");
        for (int i = 0; i < numPlayers; ++i) {
            Player player = new Player();
            try {
//                Printer.out("Game readObject - player "+i);
                if (player.readFrom(in)) {
                    players.add(player);
//                    Printer.out("Loaded player "+player.name);
                }
                if (player.sendAll != Player.SEND_ALL) {
                    Printer.out("Incomplete player saved. WTF?");
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }
        }
//        Printer.out("Loaded "+players.size()+" players");
        return true;
    }

    public void AddDefaultAI() {
        // Some default NPC-players?
        players.add(Player.NewAI("Mad Max"));
        players.add(Player.NewAI("Mad Marvin"));
    }

    static int numNextDaysSkipped = 0;

    boolean ShouldPrintDaysSkipped(){
//        if (numNextDaysSkipped < 100 && numNextDaysSkipped % 10 == 0)
  //          return true;
        if (numNextDaysSkipped < 1000 && numNextDaysSkipped % 100 == 0)
            return true;
        if (numNextDaysSkipped < 1000 && numNextDaysSkipped % 1000 == 0)
            return true;
        return false;
    }

    /// returns num of player characters simulated.
    public int NextDay() {
        Log.logIDEnumerator = logEnumerator; // Set log enumerator for this session.
        // TODO: Add default player as needed? Elsewhere?
//        if (!players.contains(App.GetPlayer()))
  //          players.add(App.GetPlayer());
        if (players.size() == 0) {
            if (ShouldPrintDaysSkipped())
                Log("Game.NextDay: No players, doing nothing");
            ++numNextDaysSkipped;
            return 0;
        }
        int activePlayers = ActivePlayers();
        if (activePlayers == 0 && !noPauses){
            if (ShouldPrintDaysSkipped())
                Log("Game.NextDay, "+gameID.name+" players "+activePlayers+"/"+players.size()+", skipping since 0 active players.");
            ++numNextDaysSkipped;
            return 0;
        }
        if (IsLocalGame()){ // Local game, check when last new-day was pressed..? Demand at least 1 min?
            if (ShouldPrintDaysSkipped())
                Printer.out("Check time?");
        }
        else if (UpdatesSinceLastDay() == 0 && !noPauses){
            if (ShouldPrintDaysSkipped())
                Log("Game.NextDay, "+activePlayers+" active players, but skipping since no update has happened since last day. GameID: "+GameID());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ++numNextDaysSkipped;
            return 0;
        }
        numNextDaysSkipped = 0;

        dayStartTimeMs = System.currentTimeMillis(); // If nothing happening, but as well sleep a bit?
        LogAndPrint("Game.NextDay, "+gameID.name+" day "+day+" players "+activePlayers+"/"+players.size());
        ++day;
        int numSimulated = 0;
        for (int i = 0; i < players.size(); ++i) {
            Player p = players.get(i);
            if (p.ai != null)
                p.ai.Update(p);
//              if (p.isAI) // /WAT I don't even.
  //            continue;
            if (!p.IsAliveOutsideCombat())
                continue;
            if (activePlayers < 3)
                Printer.out(p.Name()+" next day..");
            ++numSimulated;
            p.Adjust(Stat.HP, -0.2f); // Everybody is dying.
            p.NextDay(this);
            p.lastEditSystemMs = System.currentTimeMillis();
            // Save?
/*            if (p.lastSaveTimeSystemMs < p.lastEditSystemMs) {
                Printer.out("SAving again yow");
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
        for (int i = 0; i < players.size(); ++i) {
            if (!players.get(i).IsAliveOutsideCombat())
                continue;
            ++tot;
        }
        return tot;
    }

    /// Booleans default false, flag the one you wish to search with, both may be used simultaneously.
    public Player GetPlayer(String name, boolean contains, boolean startsWith) {
        if (name == null)
            return null;
    //    Printer.out("Looking for "+name);
        for (int i = 0; i < players.size(); ++i) {
            Player p = players.get(i);
            if (contains && p.Name().contains(name)) {
      //          Printer.out("Player "+i+" contains? "+p.Name());
                return p;
            }
            if (startsWith && p.Name().startsWith(name)) {
            //    Printer.out("Player "+i+" startsWith? "+p.Name());
                return p;
            }
        }
        return null;
    }
    public Player GetPlayer(String name) {
        if (name == null){
            Printer.out("Player.GetPlayer with null name, wtf");
            return null;
        }
        name = name.trim();
        for (int i = 0; i < players.size(); ++i) {
            Player p = players.get(i);
//            Printer.out("Player "+i+" name equals? "+p.Name());
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
        games.add(Game.UpdatesEveryMinutes(1, GameID.GlobalGame, "60 seconds")); // Changed ID to be the default one.
//        games.add(Game.UpdatesEveryMinutes(10, GameID.GlobalGame_10Minutes, "10 minutes"));
  //      games.add(Game.UpdatesEveryMinutes(60, GameID.GlobalGame_60Minutes, "60 minutes"));

        for (int i = 0; i < games.size(); ++i)
            games.get(i).CreateDefaultPlayers();
        return games;
    }
    private void CreateDefaultPlayers() {
        int numTest = 0; // Set 6 to test all difficulties.
        for (int i = 0; i < numTest; ++i) {
            // Create default players?
            Player player = new Player();
            player.name = "Erenik ";
            if (numTest > 1)
                player.name += Difficulty.String(i);
            player.isAI = false;
            player.email = "emil_hedemalm@hotmail.com";
            player.password = Auth.Encrypt("1234", Auth.DefaultKey);
            player.gameID = GameID();

            player.ai = new AI(AI.Erenik);

            player.Set(Config.Difficulty, i); // 0-5 in difficulty
            player.ReviveRestart();

            // 1 life
            player.Set(Stat.Lives, 1);
            player.Set(Stat.FOOD, 1000);
            player.Set(Stat.MATERIALS, 1000);
            player.Set(Stat.SHELTER_DEFENSE, 7);

            int quality = 0;
            player.cd.inventory.add(Invention.RandomWeapon(quality)); // Give a good weapon.
            player.cd.inventory.add(Invention.RandomArmor(quality)); // Armor
            player.cd.inventory.add(Invention.RandomTool(quality)); // And tool.
            // Equip all.
            for (int j = 0; j < player.cd.inventory.size(); ++j) {
                player.Equip(player.cd.inventory.get(j));
            }

//            player.SetLevel(SkillType.WeaponizedCombat, 9);
  //          player.SetLevel(SkillType.DefensiveTraining, 9);
    //        player.SetLevel(SkillType.Survival, 9);
      //      player.SetLevel(SkillType.Parrying, 9);

            Printer.out("Adding default player");
            player.PrintAll();

            AddPlayer(player);
        }
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
                    Printer.out("numPlayers "+value);
                else
                    Printer.out("Bad key-value pair: "+key+" value: "+value);
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

    long dayStartTimeMs = 0;
    long lastSecondNotified = 0;
    /// Return true if a new day occurred.
    public boolean Update(long milliseconds) {
        updateIntervalSeconds = secondsPerDay; // Can set via command-line, -secondsPerDay 60
        int msPerDayInGame = updateIntervalSeconds * 1000; // Should be * 1000
        long nextUpdateMs = dayStartTimeMs + msPerDayInGame;
        long tilNextUpdateMs = nextUpdateMs - System.currentTimeMillis();
        long secondsTilNextUpdate = tilNextUpdateMs / 1000;
        if (secondsTilNextUpdate % intervalToPrintGameStatusSeconds == 0){
            if (secondsTilNextUpdate != lastSecondNotified){
                long minutesTilNextUpdate = secondsTilNextUpdate / 60;
                long hoursTilNextUpdate = minutesTilNextUpdate / 60;
                int minuteTilNextUpdate = (int) (minutesTilNextUpdate - hoursTilNextUpdate * 60);
                Printer.out("Time to next update: "+hoursTilNextUpdate+"h "+minuteTilNextUpdate+"m");
                lastSecondNotified = secondsTilNextUpdate;
            }
        }

        if (System.currentTimeMillis() > dayStartTimeMs + msPerDayInGame) {        // check if next day should come.
            if (NextDay() != 0) {
                Save(); // Save to file.
                return true;
            }
        }
        return false;
    }

    private void Log(String msg){
        FileUtil.AppendWithTimeStampToFile("logs", "game"+GameID()+".txt", msg);
        Printer.out("Log: "+msg);
    }
    private void LogAndPrint(String msg){
        Log(msg);
        Printer.out(msg);
    }
    // Returns null if all known player names are already known.
    public Player RandomLivingPlayer(EList<String> knownPlayerNames) {
        /// 10 random chances.
        if (players.size() == 0) {
            Printer.out("Game has 0 players. WAT");
            System.exit(444);
            return null;
        }
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < 10; ++i) {
            int playerIndex = r.nextInt(players.size());
  //          Printer.out("player index: "+playerIndex);
            Player randPlayer = players.get(playerIndex);
            if (!randPlayer.IsAliveOutsideCombat())
                continue;
            boolean alreadyKnown = false;
            for (int j = 0; j < knownPlayerNames.size(); ++j) {
                String name = randPlayer.name;
                String name2 = knownPlayerNames.get(j);
//                Printer.out(name+" =? "+name2);
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
            Printer.out(p.email+" == "+email+"? "+p.email.equals(email)+" "
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
            Printer.out(p.email+" == "+email+"? "+p.email.equals(email));
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

    public void PrintGlobalPlayerStatistics() {
        long[] statistics = new long[Statistic.values().length];
        int longestTurnSurvived = 0;

        // [0] - Total, [1] - Average of those alive, [2] - Max level of those alive.
        float[][] skillStatistics = new float[3][SkillType.values().length];
        /// 0 min, 1 average, 2 max
        float[][] statStatistics = new float[3][Stat.values().length];
        for (int i = 0; i < Stat.values().length; ++i)
            statStatistics[0][i] = 100000000;

        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            if (p.IsAliveOutsideCombat() && p.Get(Stat.TurnSurvived) > longestTurnSurvived)
                longestTurnSurvived = (int) p.Get(Stat.TurnSurvived);
            for (int j = 0; j < p.cd.skills.size(); ++j) {
                Skill s = p.cd.skills.get(j);
                int skillIndex = s.GetSkillType().ordinal();
                int skillLevel = s.Level();
//                Printer.out("skills Level "+s.Level());
                skillStatistics[0][skillIndex] += skillLevel;
                if (!p.IsAliveOutsideCombat())
                    continue;
                if (skillLevel > skillStatistics[2][skillIndex])
                    skillStatistics[2][skillIndex] = skillLevel;
                skillStatistics[1][skillIndex] += skillLevel;
            }
            for (int j = 0; j < p.cd.statArr.length; ++j){
                float statValue = p.cd.statArr[j];
                switch (Stat.values()[j]){
                    case BASE_ATTACK: statValue = p.BaseAttack(); break;
                    case BASE_DEFENSE: statValue = p.BaseDefense(); break;
                    case MAX_HP: statValue = p.MaxHP(); break; // Max hp taking into account skills, etc.
                }
                if (p.IsAliveOutsideCombat()) {
                    statStatistics[1][j] += statValue;
                    if (statValue > statStatistics[2][j])
                        statStatistics[2][j] = statValue;
                    if (statValue < statStatistics[0][j])
                        statStatistics[0][j] = statValue;
                }
            }
//            Statistic.Print("Player statistics for: "+p.name, p.cd.statistics);
            statistics = Statistic.Add(statistics, p.cd.statistics);
        }
        String skillStatisticsString = "\nSkills breakdown. alive players: "+ActivePlayers()
                +"\n"+String.format("%20s", "Skill name")+" "+String.format("%6s", "Total")+" "+String.format("%6s", "Average")+" "+String.format("%6s", "Highest")
                +"\n=============================================================\n";
        for (int i = 0; i < SkillType.values().length; ++i){
            skillStatisticsString += "\n"+String.format("%20s", SkillType.values()[i])+" "+String.format("%6s", (int)skillStatistics[0][i])
                    +"   "+String.format(Locale.ENGLISH, "%.2f", (skillStatistics[1][i] / (float)ActivePlayers()))
                    +" "+String.format("%6s", (int)skillStatistics[2][i]);
        }

        String statsStatisticsString = "\nStats breakdown, alive players: "+ActivePlayers()
                +"\n"+String.format("%20s", "Stat")
                +" "+String.format("%4s", "AMin")
                +" "+String.format("%5s", "AAvg")
                +" "+String.format("%4s", "AMax")
                +"\n====================================================";
        for (int i = 0; i < Stat.values().length; ++i){
            statStatistics[1][i] /= ActivePlayers();
            switch (Stat.values()[i]){
                case SHELTER_DEFENSE_PROGRESS:
                    continue;
            }
            statsStatisticsString += "\n"+String.format("%20s", Stat.values()[i].name())
                    +" "+String.format("%4s", ""+(int) statStatistics[0][i])
                    +" "+String.format(Locale.ENGLISH, "%5s", ""+ String.format("%.1f", statStatistics[1][i]))
                    +" "+String.format("%4s", ""+(int) statStatistics[2][i]);
        }



        FileUtil.AppendWithTimeStampToFile("logs", "TurnStatistics", Statistic.Print("Global player summary stats for day: "+day+" longestTurnSurvived and still alive: " +longestTurnSurvived
                +"\nPlayers: "+ActivePlayers()+"/"+players.size()
                , statistics)+skillStatisticsString+statsStatisticsString);
    }

    public void OnPlayerDied(Player player) {
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            p.OnPlayerDied(player);
        }
    }
}
