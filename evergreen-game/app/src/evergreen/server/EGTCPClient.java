package evergreen.server;

import evergreen.common.player.AAction;
import evergreen.common.player.Action;
import evergreen.common.player.Skill;
import evergreen.common.player.SkillType;
import evergreen.util.EList;
import evergreen.util.EList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import evergreen.Game;
import evergreen.common.Player;
import evergreen.common.packet.EGPacket;
import evergreen.common.packet.EGPacketCommunicator;
import evergreen.common.packet.EGPacketError;
import evergreen.common.packet.EGPacketReceiverListener;
import evergreen.common.packet.EGRequest;
import evergreen.common.packet.EGRequestType;
import evergreen.common.packet.EGResponseType;
import evergreen.common.player.Config;
import evergreen.common.player.DAction;
import evergreen.common.player.Stat;
import evergreen.util.NameGenerator;
import evergreen.util.Printer;
import evergreen.transport.TransportType;

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
        Printer.out("Launching "+num+" clients.");
        for (int i = 0; i < num; ++i) {
            /// By default, launch 2 clients?
            EGTCPClient client = new EGTCPClient();
            client.start(); // Start thread.
        }
    }
    int msNoPlayers = 0;
    public void run() {
        int sleepTimeMs = 5000;
        try {
            GetGamesList();            // Fetch games list.
            while(true) {
                comm.CheckForUpdates(); // Yeah.
                if (players.size() == 0) {
                    Printer.out("No players to update");
                    msNoPlayers += sleepTimeMs;
                    if (msNoPlayers > 15000) {
                        break;
                    }
                }
                UpdatePlayers();            // Update states of players until done.
                Thread.sleep(sleepTimeMs); // Sleep a second between each update?
            }
        } catch (Exception ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        log.add("Stopping");
        Printer.out("Stopping EGTCPClient.");
    }

    private void UpdatePlayers() {
        // Fetch state. See if it changed?
        for (int i = 0; i < players.size(); ++i) {
            final Player player = players.get(i);
            if (!player.IsAliveOutsideCombat())
                continue;
//            Printer.out("Client updating player "+player.name);
            UpdatePlayer(player);

            EGPacket pack = EGRequest.Save(player);
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    switch (reply.ResType()) {
                        case Player:
                        case OK:
                            Printer.out("Update received");
                            player.fromByteArr(reply.GetBody());
                            break;
                        case PlayerClientData:
                            player.cd = reply.GetClientData();
//                            Printer.out("EGTCPClient: Client received PlayerClientData");
                            break;
                        default:
                            Printer.out("EGTCPClient: Error loading, "+reply.ResType().name());
                            break;
                    }
                }
                @Override
                public void OnError(EGPacketError error) {
                    Printer.out("EGPacketError: "+error.name());
                }
            });
            comm.Send(pack);
        }
    }
    Random skillRandom = new Random(System.currentTimeMillis());
    /// Wat.
    private void UpdatePlayer(final Player player) {
        // Do some change.
        player.cd.dailyActions.clear();
        // Generate some new actions.
        String dActionsStr = "",
            aActionsStr = "";
//        Printer.out("Update EGTCPCLient - generate dailyActions");
        for (int i = 0; i < 4; ++i) { // Generate some random actions.
            Action action = DAction.RandomDailyAction(player);
            if (action == null)
                continue;
            dActionsStr += action+", ";
            player.cd.dailyActions.add(action);
        }
        for (int i = 0; i < 1; ++i){
            Action action = AAction.RandomActiveAction(player);
            if (action == null)
                continue;
            aActionsStr += action+", ";
            player.cd.queuedActiveActions.add(action);
        }
//        Printer.out("Update EGTCPCLient - save to server");
        player.MarkLogMessagesAsReadByClient();         // Mark all log messages as read.
  //      Printer.out("DActions: "+dActionsStr);
    //    Printer.out("AActions: "+aActionsStr);
        player.Equip(player.RandomItem());  // Equip random item to test that sub-system as well.
        player.cd.skillTrainingQueue.clear();
        player.cd.skillTrainingQueue.add(SkillType.values()[skillRandom.nextInt(SkillType.values().length) % SkillType.values().length].text); // Learn random skills?
        player.RandomizeGenerateTransportUsageData();
        // Save/send to server.
        EGPacket save = EGRequest.Save(player);
        save.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                switch (reply.ResType()){
                    default:
                        Printer.out("ResType: "+reply.ResType().name());
                }
            }
            @Override
            public void OnError(EGPacketError error) {
                Printer.out("Save failed: "+error.name());
                Printer.out("Unregistering this player from EGTCPClient thread.");
                players.remove(player);
                return;
            }
        });
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
            player.Set(Config.Difficulty, r.nextInt(6)); // Randomize player difficulty.
            EGPacket pack = EGRequest.CreatePlayer(player);
            pack.addReceiverListener(new EGPacketReceiverListener() {
                @Override
                public void OnReceivedReply(EGPacket reply) {
                    EGResponseType resT = reply.ResType();
                    if (resT == null) {
                        Printer.out("Reply packet: " + reply);
                        return;
                    }
                    switch (resT) {
                        case Player:
                        case OK:
                            players.add(player);
                            Printer.out("Player registered successfully!");
                            break;
                        default:
                            Printer.out("An error occurred. Player not added to list of players: "+resT.name());
                            break;
                    }
                }
                @Override
                public void OnError(EGPacketError error) {
                    Printer.out("EGPacketError: "+error.name());
                    msNoPlayers += 5000; // For each error, go to closing this thread.
                }
            });
            comm.Send(pack);
        }
    }

    private void GetGamesList() {
        Printer.out("Requesting games list");
        EGPacket pack = EGRequest.byType(EGRequestType.GetGamesList);
        pack.addReceiverListener(new EGPacketReceiverListener() {
            @Override
            public void OnReceivedReply(EGPacket reply) {
                Printer.out("Reply received");
                if (games.size() == 0) {
                    games = reply.parseGamesList();
                    CreatePlayersInAllGames(); // Create players.
                }
            }

            @Override
            public void OnError(EGPacketError error) {
                Printer.out("EGPAcketError: "+error.name());
            }
        });
        comm.Send(pack);
    }

    EList<String> log = new EList<>();
    public void PrintLog()
    {
        Printer.out("\nServer log:\n---------------");
        for (int i = 0; i < log.size(); ++i)
        {
            Printer.out(log.get(i));
        }
    }

}
