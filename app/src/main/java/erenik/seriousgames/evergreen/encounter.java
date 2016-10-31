package erenik.seriousgames.evergreen;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class encounter extends AppCompatActivity
{
    static List<Enemy> enemies = new ArrayList<Enemy>(); // List of 'em.
    static List<Log> log = new ArrayList<Log>();
    static Random r = new Random();

    static boolean isRandom = false;
    /// Clears old lists.
    static void NewEncounter()
    {
        enemies.clear();
        log.clear();
        Player player = Player.getSingleton();
        player.PrepareForCombat();
    }
    static void Random(Dice dice)
    {
        isRandom = true;
        Player player = Player.getSingleton();
        int level = player.turn / 16;
        List<EnemyType> typesPossible = new ArrayList<EnemyType>();
        for (int i = 0; i < EnemyType.values().length; ++i)
        {
            if (EnemyType.values()[i].level <= level)
                typesPossible.add(EnemyType.values()[i]);
        }
        /// Randomly choose type.
        int ri = r.nextInt(typesPossible.size());
        EnemyType et = typesPossible.get(ri);
        dice.dice *= et.encounterAmount;
        dice.bonus *= et.encounterAmount;
        int iAmount = dice.Roll();
        for (int i = 0; i < iAmount; ++i)
        {
            Enemy e = new Enemy(et);
            enemies.add(e);
        }
        Log("You encounter "+iAmount+" "+et.name+"s.", LogType.INFO);
    }
    /// Quick simulation
    static void Simulate()
    {
        // Start it?
        Player player = Player.getSingleton();
        int expGained = 0;
        boolean dead = false;
        while(enemies.size() > 0 && player.IsAlive())
        {
            for (int i = 0; i < 5 && i < enemies.size(); ++i)
            {
                Enemy e = enemies.get(i);
                if (e.Attack(player))
                {
                    Log("You die.", LogType.ATTACKED);
                    dead = true;
                    break;
                }
            }
            if (dead)
                break;
            Enemy e = enemies.get(0);
            player.Attack(e);
            if (!e.IsAlive()) {
                Log("The "+e.name+" dies.", LogType.INFO);
                enemies.remove(e);
                expGained += e.exp;
            }
        }
        Log("You survive the encounter.", LogType.INFO);
        // Done.
        player.GainEXP(expGained);
        // Reduce encounters of this sort.
        if (isRandom)
            player.Adjust(Stat.ENCOUNTERS, -1);
        // Spam all log stuff to console?
        player.log.addAll(log); // Add current log there with all attack stuff.
        // Save?
        player.SaveLocally();

    }
    static void AssaultsOfTheEvergreen()
    {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter);
        // Hook up some GUI stuffs.

        // Update gui.

    }
    void UpdateGui()
    {
        // HP, attack, defense, debuffs, enemies.
    }

    public static void Log(String s, LogType type)
    {
        // Add to log to present/update to ui?
        Log l = new Log(s, type);
        log.add(l);
    }
}
