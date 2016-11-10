package erenik.seriousgames.evergreen;

import java.util.ArrayList;
import java.util.List;

import erenik.seriousgames.evergreen.player.NotAliveException;
import erenik.seriousgames.evergreen.player.Player;

/**
 * Class for step-wise simulation of the game world, including players, transactions, strongholds, fights, etc. Between each step, actions, skills and choices should be submitted to the simulator for proper output?
 * Created by Emil on 2016-11-09.
 */
public class Simulator
{
    /// Singleton.
    private static Simulator s = new Simulator();
    public static Simulator getSingleton()
    {
        return s;
    }
    List<Player> players = new ArrayList<>();
    private Simulator()
    {
        // Some default NPC-players?
        players.add(Player.NewAI("Mad Max"));
        players.add(Player.NewAI("Mad Marvin"));
    }

    /// Adjusts stats, generates events based on chosen actions to be played, logged
    public void NextDay()
    {
        if (!players.contains(Player.getSingleton()))
            players.add(Player.getSingleton());
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
}
