/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.server;

import erenik.evergreen.Game;
import erenik.evergreen.GameID;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.auth.Auth;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogListener;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketType;
import erenik.evergreen.common.packet.EGRequest;
import erenik.evergreen.common.packet.EGResponse;
import erenik.evergreen.common.packet.EGResponseType;
import erenik.evergreen.common.player.Config;
import erenik.evergreen.common.player.PlayerListener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.text.SimpleDateFormat;

import erenik.evergreen.common.player.Stat;
import erenik.util.EList;
import java.util.Date;
import erenik.util.EList;
import erenik.util.Printer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Emil
 */
public class EGTCPServer extends Thread {
    private static final String INFO = "INFO";
    /**
     * @param args the command line arguments
     */
    int verbosityLevel = 0; // 1 for more, 2 for morer,
    int portN = 4000;
    int maxActivePlayers = 1000;
    public int packetsReceived = 0;
    public int GetPort(){return portN;};
    int millisecondsTimeout = 50;
    EList<Socket> sockets = new EList<Socket>();
    EList<Game> games = new EList<>();
    ServerSocket servSock;


    // Create a log file based on the start time of this iteration?
    static Date startTime = new Date();
    static String folderString = "logs/firstFocusGroup"; // +new SimpleDateFormat("yyyyMMdd_HHmmss").format(startTime);
    static void LogPlayerStats(String s){
        AppendToFile("player_updates", s);
    }
    static void Log(String s){
        AppendToServerLogFile(s);
    }
    static void Log(String s, String logType){
        AppendToServerLogFile(s);
        Printer.out(logType+": "+s);
    }
    static void AppendToServerLogFile(String s) {
        // Create folder if needed?
        String serverLogFile = "server";
        AppendToFile(serverLogFile, s);
    }
    static void AppendToMergedFile(String s) {
        String serverLogFile = "players_merged";
        AppendToFile(serverLogFile, s);
//        String serverLogFile = "server_"+ (new Date()).for "yyyy.MM.dd G 'at' HH:mm:ss z" (System.currentTimeMillis()/1000);
    }
    static void AppendToFile(String fileName, String s) {
        new File(folderString).mkdirs();
        try {
            FileWriter fw = new FileWriter(folderString+"/"+fileName+".txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            String dateStrNow = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
            out.println(dateStrNow+" "+s);
            out.flush();
            fw.close(); // Close it.
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    LogListener logListener = new LogListener() {
        @Override
        public void OnLog(Log l, Player player) {
            AppendToFile("game"+player.gameID+".txt", player.name+": "+l); // Append to game-specific file.
            AppendToMergedFile(player.name+": "+l);            // Append to file for all games and players?
        }
    };

    public static void main(String[] args) throws Exception {
        // Load settings from file.
        Printer.printToFile = true;

        EGTCPServer serv = new EGTCPServer();
        int numAIs = 5, iarg = 0;
        for (int i = 0; i < args.length; ++i){
//            Printer.out("args "+i+": "+args[i]);
            // No args here.
            if (args[i].equals("-noPauses")) {
                Game.noPauses = true;
                Printer.out("-noPauses found");
            }
            // 1 args below
            if (i >= args.length - 1)
                continue;
            String arg = args[i+1];
            try {
                iarg = Integer.parseInt(arg);
            } catch (NumberFormatException nfe){} // Silently ignore it.
            if (args[i].equals("-ais"))
                numAIs = iarg;
            if (args[i].equals("-maxActivePlayers")) {
                serv.maxActivePlayers = iarg;
                Log("Max active players set: "+serv.maxActivePlayers, INFO);
            }
            if (args[i].equals("-secondsPerDay")){
                Game.secondsPerDay = iarg;
                Printer.out("-secondsPerDay "+iarg);
            }
            if (args[i].equals("-printStatusInterval")){
                Game.intervalToPrintGameStatusSeconds = iarg;
                Printer.out("-printStatusInterval "+iarg);
            }
        }
        if (args.length > 2) {
            if (args[0].equals("test")) {
                // Launch some clients.
                // EGTCPClient.LaunchClients(5); // TODO: Remove later, move to have as args for adding AI.
            }
        }
        Log("Launching server", "INFO");
        serv.start(); // Start it.


        Log("Launching simulated/AI clients "+numAIs, "INFO");
        EGTCPClient.LaunchClients(numAIs);
    }

    boolean stopHosting = false;
    public void run() {
        try {
            StartServer();
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        Log("Stopping server, failed to start it", "ERROR");
    }
    
    void StartServer() throws IOException {
        // Create main game list.
        if (!LoadGames()) {
            games = Game.CreateDefaultGames();
            for (int i = 0; i < games.size(); ++i){
                AddListenersAndSetSysMsg(games.get(i).players);
            }
        }
        Host();
        /// Host server.
        long lastUpdateMs = System.currentTimeMillis();
        while (stopHosting == false) {
            long nowMs = System.currentTimeMillis();
            long diffMs = nowMs - lastUpdateMs;
            AcceptClients(diffMs);
            if (sockets.size() > 0)
                ReadIncomingData();
            for (int i = 0; i < games.size(); ++i) {
                Game g = games.get(i);
                if (g.Update(diffMs))
                    g.PrintGlobalPlayerStatistics();
            }
            lastUpdateMs = nowMs;
        }
        servSock.close();
        Log("Closed server socket", "INFO");
    }

    private boolean LoadGames() {
        games.clear();
        for (int i = 0; i < GameID.MAX_TYPES; ++i){
            Game game = Game.Load(Game.DefaultPath(i));
            if (game == null)
                continue;
            games.add(game);
            for (int j = 0; j < game.players.size(); ++j){
                AddListenersAndSetSysMsg(game.players.get(j));
            }
        }
        return games.size() > 0;
    }

    void Host() throws IOException
    {
        servSock = new ServerSocket(portN);
        servSock.setSoTimeout(millisecondsTimeout);
        Log("Launching tcp server on port: "+portN,"INFO");
    }
    static int count = 0;
    int msLastClientConnected = 0;
    void AcceptClients(long diffMs) throws IOException {
        if (sockets.size() > 0) {
            int thresholdMs = 100000 / sockets.size(); // Kill socket index 0 every 10 seconds. Decrease time as # of sockets increases. I.e. once per second for 100 simultaneous connections.
            msLastClientConnected += diffMs;
            if (msLastClientConnected > thresholdMs) {
                msLastClientConnected -= thresholdMs;
                Socket sock = sockets.get(0);
                sock.close();
                sockets.remove(0);
            }
        }
        /// check old ones, if any disconnected
        for (int i = 0; i < sockets.size(); ++i) {
            Socket sock = sockets.get(i);
            if (sock.isClosed() || !sock.isConnected()) {
                sockets.remove(sock);
                Log("EGTCPServer.AcceptClients: Client disconnected or socket closed");
                Log("EGTCPServer.AcceptClients: Num active sockets/clients: "+sockets.size(), "INFO");
            }
        }
        while (true) {
            try {
                Socket socket = servSock.accept();
                sockets.add(socket);
            }
            catch (java.io.InterruptedIOException ioe) {
                break; // No new client, just break the loop.
            }
            if (verbosityLevel > 1)
                Log("Incoming client, Num clients: "+sockets.size(), "INFO");
        }
    }
    /// Read incoming data from network sockets.
    static byte[] readBuffer = new byte[EGPacket.BUF_LEN];
    void ReadIncomingData() throws IOException {
    //    System.out.print("Incoming data");
        boolean incData = false;
        for (int i = 0; i < sockets.size(); ++i) {
            Socket sock = sockets.get(i);
            InputStream is = sock.getInputStream();
            int availableBytes = is.available();
            if (availableBytes <= 0)
                continue;
            int bytesRead = -1;
            try {
                is.read(readBuffer);
            } catch (IOException ioe) {
                Log("Exception occurred, ditching socket. "+ioe.getMessage(), "INFO");
                sockets.remove(sock);
                --i;
                continue;
            }
//            Printer.out("BytesRead: "+bytesRead);
  //          Printer.out("Packet received?");
            EGPacket pack = EGPacket.packetFromBytes(readBuffer);
    //        Printer.out("Packet received: "+pack);
            if (pack == null) {
                Log("Packet null: ");
                Reply(sock, EGPacket.error(EGResponseType.BadRequest).build()); // Reply with error String.
                sock.close(); // Close the socket.
                sockets.remove(sock); // Remove it from the list.
                continue;
            }
            ++packetsReceived;
   //         Printer.out("Packet type: "+pack.Type().text);
            // Check requests and evaluate them.
            if (pack.Type() == EGPacketType.Request) {
                try {
                    EvaluateRequest(sock, pack);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                Log("Received non-Request type packet. Replying BadRequest. Pack received: "+pack);
                Reply(sock, EGPacket.error(EGResponseType.BadRequest).build());
            }
        }
       // if (incData == false)
         //   Printer.out("?");
    }
    static void Reply(Socket sock, byte[] packetContents) {
        try {
            OutputStream os = sock.getOutputStream();
            os.write(packetContents, 0, packetContents.length);
            os.flush();
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    private void EvaluateRequest(Socket sock, EGPacket pack) throws Exception {
        EGRequest req = null;
        if (pack instanceof EGRequest)
            req = (EGRequest) pack;
        switch(pack.ReqType()) {
            case TurnSurvived: {
                Player p = pack.GetPlayer();
                if (CheckCredentials(p, sock)){
                    Reply(sock, EGResponse.TurnsSurvived(GetPlayerInSystem(p).Get(Stat.TurnSurvived)).build());
                }
                break;
            }
            case Save: { // Player as POJO in body
                EvaluateSaveRequest(sock, pack);
                break;
            }
            case CreatePlayer: { // Initial creation of player. Check for availability.
                EvaluateCreateRequest(sock, pack);
                break;
            }
            case FetchWholeLog: {
                Player p = pack.GetPlayer();
                if (CheckCredentials(p, sock)){
                    SendAllLogMessages(p, sock); // Reply packets
                }
                break;
            }
            case LogLength:
                Player p = pack.GetPlayer();
                if (CheckCredentials(p, sock)){
                    Reply(sock, EGResponse.numLogMessages(GetPlayerInSystem(p).log.size()).build());
                }
                break;
            case FetchLog:{
                EGRequest.ExtraArgs fla = req.parseExtraArgs();
                if (CheckCredentials(fla.player, sock)){
                    EList<Log> logMessagesToReply = GetPlayerInSystem(fla.player).LogSublist(fla.startIndex, fla.startIndex + fla.numMsgsFromStartIndex - 1, fla.oldestLogIDToInclude);
                    Reply(sock, EGResponse.logMessages(logMessagesToReply).build());
                    // TODO: For future work, uncomment here as needed to debug server sending log-messages out.
              //      Printer.out("start index : "+fla.startIndex+" num: "+fla.numMsgsFromStartIndex+" avail: "+GetPlayerInSystem(fla.player).log.size()+" oldestToInclude: "+fla.oldestLogIDToInclude
                //            +" replied#: "+logMessagesToReply.size());
                }
                break;
            }
            case RestartSameCharacter: {
                EvaluateRestartRequest(sock, pack);
                break;
            }
            case Load: { // Player as POJO in body, at least the name and password.
                EvaluateLoadRequest(sock, pack);
                break;
            }
            case LoadCharacters:{
                EvaluateLoadCharactersRequest(sock, pack);
            }
            case GetGamesList: {
                try {
                    Log("EGTCPServer.EvaluateRequest");
                    Reply(sock, EGPacket.gamesList(games).build());
                    // Body irrelevant, just send back the games list.
                } catch (Exception e)
                {
                    return;
                }
                break;
            }
            default:
                Log("Send bad request reply o-o");
                Reply(sock, EGPacket.error(EGResponseType.BadRequest).build());
        }
    }

    private void SendAllLogMessages(Player player, Socket sock) {
        Player playerInSystem = GetPlayerInSystem(player);
        if (playerInSystem == null)
            return;
        int divider = 10;
        for (int i = 0; i < playerInSystem.log.size(); i += divider){
            int startIndex = i;
            int endIndex = i + divider - 1;
            if (endIndex >= playerInSystem.log.size())
                endIndex = playerInSystem.log.size() - 1;
            EList<Log> subList = new EList<>();
            subList.addAll(playerInSystem.log.subList(startIndex, endIndex));
//            for (int j = startIndex; j < endIndex; ++j)
  //              subList.add(playerInSystem.log.get(j));
            EGPacket pack = EGPacket.logMessages(subList);
            Reply(sock, pack.build());
            Printer.out("Replied packet with logmessages "+startIndex+" to "+endIndex);
        }
    }

    // Checks the credentials provided in the player object. If bad, it will send a reply to the given socket.
    private boolean CheckCredentials(Player player, Socket sock) {
        Player playerInSystem = GetPlayerInSystem(player); // Does it equate an existing player?
        if (playerInSystem == null){
            Printer.out("Credentials not ok, no such player");
            Reply(sock, EGPacket.error(EGResponseType.NoSuchPlayer).build()); // No?
            return false;
        }
        if (player.CredentialsMatch(playerInSystem)) { // Passwords etc. match?
            return true;
        }
        Printer.out("Credentials not ok, bad password");
        Reply(sock, EGPacket.error(EGResponseType.BadPassword).build());
        return false;
    }

    private void EvaluateSaveRequest(Socket sock, EGPacket pack) throws Exception {
        Player player = pack.GetPlayer();
        Player playerInSystem = GetPlayerInSystem(player);
        if (!CheckCredentials(player, sock)) {
            Printer.out("EvaluateSaveRequest Bad credentials");
            return;
        }
        playerInSystem.SaveFromClient(player); // Save configured content from the client.
        LogPlayerStats("PlayerSave: "+playerInSystem.name+", Transports: "+playerInSystem.TopTransportsAsString(5)+" ClassifierSetting: "+playerInSystem.TopTransportClassifierSettingUsed());
        Game game = GetGameById(playerInSystem.gameID);
        playerInSystem.ProcessQueuedActiveActions(game); // Process queued active actions, if any.
//            Log("Save success, playerName: "+playerInSystem.name+", replying up-to-date data to client");
        // Update it.
        playerInSystem.sysmsg = GetSysMsg();
        playerInSystem.cd.totalLogMessagesOnServer = playerInSystem.log.size();
        Reply(sock, EGResponse.clientPlayerData(playerInSystem).build());            // Reply the player in system.
    }

    private void EvaluateLoadRequest(Socket sock, EGPacket pack) throws Exception {
    //   Printer.out("Evaluate request: LOAD");
        if (!CheckCredentials(pack.GetPlayer(), sock)) {
            Printer.out("Evaluate request: LOAD - bad credentials.");
            return;
        }
   //     Printer.out("Credentials OK, replying");
        Player playerInSystem = GetPlayerInSystem(pack.GetPlayer());
        Reply(sock, EGResponse.clientPlayerData(playerInSystem).build());            // Reply the player in system.
    }

    private String GetSysMsg() {
        String path = "sysmsg.txt";
        File file = new File(path);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ""; // Empty then.
        }
        BufferedInputStream bis = new BufferedInputStream(fis);
        int maxLength = 4000;
        byte[] buffer = new byte[maxLength];
        try {
            bis.read(buffer, 0, maxLength);
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String str = new String(buffer);
        str = str.trim(); // Trim whitespaces trailing and starting.
//        Printer.out("Sysmsg: "+str);
        return str;
    }

    private void EvaluateLoadCharactersRequest(Socket sock, EGPacket pack) {
        // Extract the details.
        //String name = ;
        byte[] body = pack.GetBody();
        String bodyAsString = new String(body, EGPacket.defaultCharset);

        String[] strarr = bodyAsString.split("\n");
        for (int i = 0; i < strarr.length; ++i)
            Printer.out("arr: "+strarr[i]);
        if (strarr.length < 2) {
            Reply(sock, EGPacket.error(EGResponseType.ParseError).build());
            return;
        }
        String email = strarr[0],
            password = strarr[1];
        EList<Player> players = GetPlayers(email, password);
        if (players.size() == 0) {
            players = GetPlayersByEmail(email);
            if (players.size() > 0){
                Reply(sock, EGPacket.error(EGResponseType.BadPassword).build());
                return;
            }
            Reply(sock, EGPacket.error(EGResponseType.NoSuchPlayer).build());
            return;
        }
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            p.sendAll = Player.CREDENTIALS_ONLY;
        }
        Log("Load success, found "+players.size()+" players, replying data to client");
        Reply(sock, EGPacket.players(players).build());            // Reply the player in system.
    }

    private EList<Player> GetPlayersByEmail(String email) {
        EList<Player> p = new EList<>();
        for (int i = 0; i < games.size(); ++i){
            Game g = games.get(i);
            p.addAll(g.GetCharacters(email));
        }
        return p;
    }

    private EList<Player> GetPlayers(String email, String password) {
        EList<Player> p = new EList<>();
        for (int i = 0; i < games.size(); ++i){
            Game g = games.get(i);
            p.addAll(g.GetCharacters(email, password));
        }
        return p;
    }

    private void EvaluateRestartRequest(Socket sock, EGPacket pack) throws Exception {
        Player player = pack.GetPlayer();
        Log("EvaluateRestartRequest: "+player.name);
        if (player == null) {
            Reply(sock, EGPacket.parseError().build());
            return;
        }
        Game game = GetGameById(player.gameID);
        if (game == null){
            Printer.out("EvaluateRestartRequest: NoSuchGame");
            Reply(sock, EGPacket.error(EGResponseType.NoSuchGame).build());
            return;
        }
        Player exists = game.GetPlayer(player.name);
        if (exists != null){
            // Restart it then?
            exists.PrepareForTotalRestart(); // Clear old items as well, yo.
            exists.ReviveRestart();
            Reply(sock, EGPacket.player(exists).build()); // Notify success with the updated stats n stuff.
            return;
        }
        Reply(sock, EGPacket.error(EGResponseType.NoSuchPlayer).build());
        return;
        /* I guess listeners are still attached properly..?
        player.addLogListener(logListener);
        player.addStateListener(new PlayerListener() {
            @Override
            public void OnPlayerDied(Player player) {
                SavePlayerLog(player);
            }
            @Override
            public void OnPlayerNewDay(Player player) {
                SavePlayerLog(player);
            }
        });
        */
    }

    private void EvaluateCreateRequest(Socket sock, EGPacket pack) throws Exception {
        Player player = pack.GetPlayer();
        player.name = player.name.trim(); // Trim whitespaces by default.
        Log("EvaluateCreateRequest: "+player.name);
        if (ActivePlayers() >= maxActivePlayers) {
            Reply(sock, EGPacket.error(EGResponseType.MaxActivePlayersReached).build());
            Log("Denying Create request due to overpopulation.", "INFO");
            return;
        }
        if (player == null) {
            Reply(sock, EGPacket.parseError().build());
            return;
        }
        // One game, so adjust game ID as needed.
        player.gameID = games.get(0).GameID();
        Game game = GetGameById(player.gameID);
        if (game == null){
            Printer.out("No such game.. D:");
            Reply(sock, EGPacket.error(EGResponseType.NoSuchGame).build());
            return;
        }

        Player exists = game.GetPlayer(player.name);
        if (exists != null) {
            Reply(sock, EGPacket.error(EGResponseType.PlayerWithNameAlreadyExists).build());
            return;
        }
        Log("Creating player: "+player.name+" in game "+player.gameID+" difficulty: "+player.Get(Config.Difficulty)+" startingBonus: "+player.Get(Config.StartingBonus)+" avatar: "+player.Get(Config.Avatar), INFO);
        game.AddPlayer(player);        // Add player to game. Save game and player.
        player.ReviveRestart(); // Revive and restart the player - set default stats according to difficulty and bonuses, etc.
//        player.gameID
        AddListenersAndSetSysMsg(player);
        Reply(sock, EGPacket.player(player).build());        // Notify success by replying the updated player.
    }

    private void AddListenersAndSetSysMsg(EList<Player> players) {
        for (int i = 0; i < players.size(); ++i)
            AddListenersAndSetSysMsg(players.get(i));
    }

    private void AddListenersAndSetSysMsg(Player player) {
        player.addLogListener(logListener);
        player.addStateListener(new PlayerListener() {
            @Override
            public void OnPlayerDied(Player player) {
                SavePlayerLog(player);
                // Force all other players to forget this player? Inform them of the death?
                Game g = GetGameById(player.gameID);
                g.OnPlayerDied(player);
            }
            @Override
            public void OnPlayerNewDay(Player player) {
                SavePlayerLog(player);
            }
        });
        player.sysmsg = GetSysMsg();
    }

    private int ActivePlayers() {
        int totAct = 0;
        for (int i = 0; i < games.size(); ++i){
            totAct += games.get(i).ActivePlayers();
        }
        return totAct;
    }

    /// Saves player log to file, within logs directory.
    void SavePlayerLog(Player player) {
        EList<LogType> filter = new EList<>();
//        filter.add(LogType.ATTACK_MISS);
  //      filter.add(LogType.ATTACKED_MISS);
        player.SaveLog(filter, folderString);
    }

    private Player GetPlayerInSystem(Player player) {
        Game game = GetGameById(player.gameID);
        if (game == null)
            return null;
        return game.GetPlayer(player.name);
    }

    private Game GetGameById(int gameID) {
        for (int i = 0; i < games.size(); ++i) {
            Game g = games.get(i);
            if (g.GameID() == gameID)
                return g;
        }
        return null;
    }

    public void StopHosting() {
        stopHosting = true;
    }
}
