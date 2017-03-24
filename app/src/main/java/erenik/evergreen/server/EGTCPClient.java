package erenik.evergreen.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import erenik.evergreen.Game;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketCommunicator;
import erenik.evergreen.common.packet.EGPacketError;
import erenik.evergreen.common.packet.EGPacketReceiverListener;
import erenik.evergreen.common.packet.EGRequest;
import erenik.evergreen.common.packet.EGRequestType;
import erenik.evergreen.common.packet.EGResponseType;
import erenik.evergreen.common.player.Config;
import erenik.evergreen.common.player.DAction;
import erenik.evergreen.common.player.Stat;
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
    List<Player> players = new ArrayList<>();

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
    public void run() {
        try {
            GetGamesList();            // Fetch games list.
            while(true) {
                comm.CheckForUpdates(); // Yeah.
                UpdatePlayers();            // Update states of players until done.
                Thread.sleep(5000); // Sleep a second between each update?
            }
        } catch (Exception ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        log.add("Stopping");
        System.out.println("Stopping server.");
    }

    private void UpdatePlayers() {
        // Fetch state. See if it changed?
        for (int i = 0; i < players.size(); ++i) {
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

                @Override
                public void OnError(EGPacketError error) {
                    System.out.println("EGPAcketError: "+error.name());
                }
            });
            comm.Send(pack);
        }
    }
    /// Wat.
    private void UpdatePlayer(Player player) {
        // Do some change.
        player.cd.dailyActions.clear();
        // Generate some new actions.
        String actionsStr = "Actions: ";
        for (int i = 0; i < 4; ++i) {
            DAction action = DAction.RandomAction(player);
            actionsStr += action+", ";
            player.cd.dailyActions.add(action.toString());
        }
        player.MarkLogMessagesAsReadByClient();         // Mark all log messages as read.
        System.out.println(actionsStr);
        player.DailyActionsAsString();
        player.Equip(player.RandomItem());
        // Save/send to server.
        comm.Send(EGRequest.Save(player));
    }

    private void CreatePlayersInAllGames() {
        Random r = new Random();
        for (int i = 0; i < games.size(); ++i) {
            final Player player = new Player();
            player.name = NameGenerator.New();
            Game game = games.get(i);
            player.gameID = game.GameID();
            player.Set(Config.StartingBonus, Player.StartingBonus.values()[r.nextInt(Player.StartingBonus.values().length)].ordinal()); // Set starting bonus.
            player.Set(Config.Difficulty, r.nextInt(6));
            EGPacket pack = EGRequest.CreatePlayer(player);
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    EGResponseType resT = reply.ResType();
                    if (resT == null) {
                        System.out.println("Reply packet: " + reply);
                        return;
                    }
                    switch (resT) {
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
                @Override
                public void OnError(EGPacketError error) {
                    System.out.println("EGPAcketError: "+error.name());
                }
            });
            comm.Send(pack);
        }
    }

    private void GetGamesList() {
        System.out.println("Requesting games list");
        EGPacket pack = EGRequest.byType(EGRequestType.GetGamesList);
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                System.out.println("Reply received");
                if (games.size() == 0) {
                    games = reply.parseGamesList();
                    CreatePlayersInAllGames(); // Create players.
                }
            }

            @Override
            public void OnError(EGPacketError error) {
                System.out.println("EGPAcketError: "+error.name());
            }
        });
        comm.Send(pack);
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
