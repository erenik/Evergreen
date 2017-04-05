package erenik.evergreen.server;

import erenik.evergreen.common.player.AAction;
import erenik.evergreen.common.player.Action;
import erenik.evergreen.common.player.Skill;
import erenik.util.EList;
import erenik.util.EList;
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
import erenik.weka.transport.TransportType;

/**
 * A client for the game. Command-line based. For testing AI or being all LINUX-y.
 * Created by Emil on 2016-12-18.
 */
public class EGTCPClient extends Thread
{
    EGPacketCommunicator comm = new EGPacketCommunicator();
    String ip;
    int port;

    EList<Game> games = new EList<>();
    EList<Player> players = new EList<>();

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
            System.out.println("Client updating player "+player.name);
            UpdatePlayer(player);

            EGPacket pack = EGRequest.Save(player);
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    switch (reply.ResType())
                    {
                        case Player:
                        case OK:
                            System.out.println("Update received");
                            player.fromByteArr(reply.GetBody());
                            break;
                        case PlayerClientData:
                            player.cd = reply.GetClientData();
                            System.out.println("EGTCPClient: Client received PlayerClientData");
                            break;
                        default:
                            System.out.println("EGTCPClient: Error loading, "+reply.ResType().name());
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
    Random skillRandom = new Random(System.currentTimeMillis());
    Random transportRand = new Random(System.currentTimeMillis());
    /// Wat.
    private void UpdatePlayer(Player player) {
        // Do some change.
        player.cd.dailyActions.clear();
        // Generate some new actions.
        String dActionsStr = "",
            aActionsStr = "";
        System.out.println("Update EGTCPCLient - generate dailyActions");
        for (int i = 0; i < 4; ++i) { // Generate some random actions.
            Action action = DAction.RandomDailyAction(player);
            if (action == null)
                continue;
            dActionsStr += action+", ";
            player.cd.dailyActions.add(action);
        }
        for (int i = 0; i < 2; ++i){
            Action action = AAction.RandomActiveAction(player);
            if (action == null)
                continue;
            aActionsStr += action+", ";
            player.cd.queuedActiveActions.add(action);
        }
        System.out.println("Update EGTCPCLient - save to server");
        player.MarkLogMessagesAsReadByClient();         // Mark all log messages as read.
        System.out.println("DActions: "+dActionsStr);
        System.out.println("AActions: "+aActionsStr);
        player.Equip(player.RandomItem());  // Equip random item to test that sub-system as well.
        player.cd.skillTrainingQueue.clear();
        player.cd.skillTrainingQueue.add(Skill.values()[skillRandom.nextInt(Skill.values().length) % Skill.values().length].text); // Learn random skills?
        // Emulate various transports.
        for (int i = 0; i < player.transports.size(); ++i) // Generate some other seconds of various degrees.
            player.transports.get(i).secondsUsed = 0;
        player.transports.get(TransportType.Idle.ordinal()).secondsUsed = 3600;
        for (int i = 0; i < 5; ++i) // Generate some other seconds of various degrees.
            player.transports.get(transportRand.nextInt(player.transports.size()) % player.transports.size()).secondsUsed += 3600 / (i + 1); // 3600, 1800, 1200, 900, etc.
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
                            System.out.println("An error occurred. Player not added to list of players: "+resT.name());
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

    EList<String> log = new EList<>();
    public void PrintLog()
    {
        System.out.println("\nServer log:\n---------------");
        for (int i = 0; i < log.size(); ++i)
        {
            System.out.println(log.get(i));
        }
    }

}
