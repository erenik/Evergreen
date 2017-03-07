package erenik.evergreen.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import erenik.evergreen.Game;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketCommunicator;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequest;
import erenik.evergreen.common.packet.EGRequestType;
import erenik.evergreen.common.packet.EGResponseType;
import erenik.evergreen.common.player.DAction;
import erenik.util.NameGenerator;

/**
 * A client for the game. Command-line based. For testing AI or being all LINUX-y.
 * Created by Emil on 2016-12-18.
 */
public class EGTCPClient extends Thread
{
    EGPacketCommunicator comm = new EGPacketCommunicator();
    String ip;
    int port;

    List<Game> games = new ArrayList<>();
    EGTCPClient()
    {}
    public static void main(String[] args)
    {
        LaunchClients(3);
    }
    public static void LaunchClients(int num) {
        System.out.println("Launching "+num+" clients.");
        for (int i = 0; i < num; ++i) {
            /// By default, launch 2 clients?
            EGTCPClient client = new EGTCPClient();
            client.start(); // Start thread.
        }
    }
    public void run()
    {
        try {
            GetGamesList();            // Fetch games list.
            CreatePlayersInAllGames(); // Create players.
            while(true) {
                UpdatePlayers();            // Update states of players until done.
                Thread.sleep(5000); // Sleep a second between each update?
            }
        } catch (Exception ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        log.add("Stopping");
        System.out.println("Stopping server.");
    }

    private void UpdatePlayers()
    {
        // Fetch state. See if it changed?
        for (int i = 0; i < players.size(); ++i)
        {
            final Player player = players.get(i);
            if (player.IsAlive() == false)
                continue;
            EGPacket pack = EGRequest.Load(player);
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    switch (reply.ResType())
                    {
                        case Player:
                        case OK:
                            System.out.println("Update received");
                            player.fromByteArr(reply.GetBody());
                            if (player.IsAlive())
                                UpdatePlayer(player);
                            break;
                        default:
                            System.out.println("Error loading");
                            break;
                    }
                }
            });
            comm.Send(pack);
        }
    }
    private void UpdatePlayer(Player player)
    {
        // Do some change.
        player.dailyActions.clear();
        // Generate some new actions.
        String actionsStr = "Actions: ";
        for (int i = 0; i < 4; ++i) {
            DAction action = DAction.RandomAction(player);
            actionsStr += action;
            player.dailyActions.add(action.toString());
        }
        System.out.println(actionsStr);
        player.DailyActionsAsString();
        // Save/send to server.
        comm.Send(EGRequest.Save(player));
    }

    List<Player> players = new ArrayList<>();

    private void CreatePlayersInAllGames()
    {
        for (int i = 0; i < games.size(); ++i)
        {
            final Player player = new Player();
            player.name = NameGenerator.New();
            Game game = games.get(i);
            player.gameID = game.GameID();
            EGPacket pack = EGRequest.CreatePlayer(player);
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    EGResponseType resT = reply.ResType();
                    if (resT == null) {
                        System.out.println("Reply packet: " + reply);
                        return;
                    }
                    switch (resT)
                    {
                        case Player:
                        case OK:
                            players.add(player);
                            System.out.println("Player registered successfully!");
                            break;
                        default:
                            System.out.println("An error occurred. Player not added to list of players.");
                            break;
                    }
                }
            });
            comm.Send(pack);
        }
    }

    private void GetGamesList()
    {
        System.out.println("Requesting games list");
        EGPacket pack = EGRequest.byType(EGRequestType.GetGamesList);
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                System.out.println("Reply received");
                games = reply.parseGamesList();
            }
        });
        comm.Send(pack);
        while(games.size() == 0)
        {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    List<String> log = new ArrayList<>();
    public void PrintLog()
    {
        System.out.println("\nServer log:\n---------------");
        for (int i = 0; i < log.size(); ++i)
        {
            System.out.println(log.get(i));
        }
    }

}
