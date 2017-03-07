/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.server;

import erenik.evergreen.Game;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogListener;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketType;
import erenik.evergreen.common.packet.EGResponseType;
import erenik.evergreen.common.player.PlayerListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Emil
 */
public class EGTCPServer extends Thread {
    /**
     * @param args the command line arguments
     */
    int verbosityLevel = 0; // 1 for more, 2 for morer,
    int portN = 4000;
    public int packetsReceived = 0;
    public int GetPort(){return portN;};
    int millisecondsTimeout = 50;
    List<Socket> sockets = new ArrayList<Socket>();
    List<Game> games = new ArrayList<>();
    ServerSocket servSock;


    // Create a log file based on the start time of this iteration?
    static Date startTime = new Date();
    static String folderString = "logs/"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(startTime);
    static void Log(String s){
        AppendToServerLogFile(s);
    }
    static void Log(String s, String logType){
        AppendToServerLogFile(s);
        System.out.println(logType+": "+s);
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
            // Log onto a server-file for all player's actions?
            // Append to file?
            AppendToMergedFile(player.name+": "+l.text);
        }
    };

    public static void main(String[] args) throws Exception {
        Log("Launching server", "INFO");
        EGTCPClient.LaunchClients(5); // TODO: Remove later, move to have as args for adding AI.

        if (args.length > 2) {
            if (args[0].equals("test")) {
                // Launch some clients.
                // EGTCPClient.LaunchClients(5); // TODO: Remove later, move to have as args for adding AI.
            }
        }
        EGTCPServer serv = new EGTCPServer();
        serv.start(); // Start it.
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
        games = Game.CreateDefaultGames();
        Host();
        /// Host server.
        long lastUpdateMs = System.currentTimeMillis();
        while (stopHosting == false)
        {
            long nowMs = System.currentTimeMillis();
            long diffMs = nowMs - lastUpdateMs;
            AcceptClients(diffMs);
            if (sockets.size() > 0)
                ReadIncomingData();
            for (int i = 0; i < games.size(); ++i) {
                games.get(i).Update(diffMs);
            }
            lastUpdateMs = nowMs;
        }
        servSock.close();
        Log("Closed server socket", "INFO");
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
    static final int BUF_LEN = 40000;
    static byte[] readBuffer = new byte[BUF_LEN];
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
//            System.out.println("BytesRead: "+bytesRead);
  //          System.out.println("Packet received?");
            EGPacket pack = EGPacket.packetFromBytes(readBuffer);
    //        System.out.println("Packet received: "+pack);
            if (pack == null) {
                Log("Packet null: ");
                Reply(sock, EGPacket.error(EGResponseType.BadRequest).build()); // Reply with error String.
                sock.close(); // Close the socket.
                sockets.remove(sock); // Remove it from the list.
                continue;
            }
            ++packetsReceived;
   //         System.out.println("Packet type: "+pack.Type().text);
            // Check requests and evaluate them.
            if (pack.Type() == EGPacketType.Request) {
                EvaluateRequest(sock, pack);
            }
            else {
                Log("Received non-Request type packet. Replying BadRequest. Pack received: "+pack);
                Reply(sock, EGPacket.error(EGResponseType.BadRequest).build());
            }
        }
       // if (incData == false)
         //   System.out.println("?");
    }
    static void Reply(Socket sock, byte[] packetContents) {
        try {
            OutputStream os = sock.getOutputStream();
            os.write(packetContents, 0, packetContents.length);
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    private void EvaluateRequest(Socket sock, EGPacket pack) {
        switch(pack.ReqType()) {
            case Save: { // Player as POJO in body
                EvaluateSaveRequest(sock, pack);
                break;
            }
            case CreatePlayer: { // Initial creation of player. Check for availability.
                EvaluateCreateRequest(sock, pack);
                break;
            }
            case Load: { // Player as POJO in body, at least the name and password.
                EvaluateLoadRequest(sock, pack);
                break;
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

    private void EvaluateSaveRequest(Socket sock, EGPacket pack) {
//        Log("Save pack received: "+pack);
        Player player = new Player();
        boolean ok = player.fromByteArr(pack.GetBody()); // Load from packet.
        if (!ok) { // Check errors
            Log("ParseError");
            Reply(sock, EGPacket.parseError().build());
            return;
        }
        Player playerInSystem = GetPlayerInSystem(player); // Does it equate an existing player?
        if (playerInSystem == null){
            Reply(sock, EGPacket.error(EGResponseType.NoSuchPlayer).build()); // No?
            return;
        }
        if (player.CredentialsMatch(playerInSystem)) { // Passwords etc. match?
            if (verbosityLevel > 1)
                Log("Load success, playerName: "+playerInSystem.name+", replying data to client");
            // Copy over stats from the system one from the one sent by the client.
            playerInSystem.SaveFromClient(player); // Then save it.
            Reply(sock, EGPacket.player(playerInSystem).build());            // Reply the player in system.
            return;
        }
        Reply(sock, EGPacket.error(EGResponseType.BadPassword).build());        // Check cause of failure. Bad authentication? Name already exists?

        boolean saved = Players.Save(player);
        if (saved) {
            Log("Saved, replying OK");
            Reply(sock, EGPacket.ok().build());
            return;
        }
        Log("BadPassword, replying error");
        // Check cause of failure. Bad authentication? Name already exists?
        Reply(sock, EGPacket.error(EGResponseType.BadPassword).build());
    }

    private void EvaluateLoadRequest(Socket sock, EGPacket pack) {
//       System.out.println("Evaluate request: LOAD");
        Player player = new Player();
        try {
            /// Use bytes AFTER the header FFS....
            boolean ok = player.fromByteArr(pack.GetBody());
        } catch (Exception e) {
            Log("reply parse error");
            Reply(sock, EGPacket.error(EGResponseType.ParseError).build());
            return;
        }
        // Get player in system, compare credentials if needed.
        Player playerInSystem = GetPlayerInSystem(player);
        if (playerInSystem == null) {
            Reply(sock, EGPacket.error(EGResponseType.NoSuchPlayer).build());
            return;
        }
        if (player.CredentialsMatch(playerInSystem)) {
            if (verbosityLevel > 1)
                Log("Load success, playerName: "+playerInSystem.name+", replying data to client");
            Reply(sock, EGPacket.player(playerInSystem).build());            // Reply the player in system.
            return;
        }
        Reply(sock, EGPacket.error(EGResponseType.BadPassword).build());        // Check cause of failure. Bad authentication? Name already exists?
    }

    private void EvaluateCreateRequest(Socket sock, EGPacket pack) {
        Player player = Player.fromByteArray(pack.GetBody());
        Log("EvaluateCreateRequest: "+player.name);
        if (player == null)
        {
            Reply(sock, EGPacket.parseError().build());
            return;
        }
        Game game = GetGameById(player.gameID);
        if (game == null){
            Reply(sock, EGPacket.error(EGResponseType.NoSuchGame).build());
            return;
        }
        Player exists = game.GetPlayer(player.name);
        if (exists != null)
        {
            Reply(sock, EGPacket.error(EGResponseType.PlayerWithNameAlreadyExists).build());
            return;
        }
        game.AddPlayer(player);        // Add player to game. Save game and player.
//        player.gameID
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
        Reply(sock, EGPacket.ok().build());        // Notify success.
    }
    /// Saves player log to file, within logs directory.
    void SavePlayerLog(Player player) {
        List<LogType> filter = new ArrayList<>();
        filter.add(LogType.ATTACK_MISS);
        filter.add(LogType.ATTACKED_MISS);
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
