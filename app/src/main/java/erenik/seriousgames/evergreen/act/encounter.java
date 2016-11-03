package erenik.seriousgames.evergreen.act;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import erenik.seriousgames.evergreen.App;
import erenik.seriousgames.evergreen.combat.*;
import erenik.seriousgames.evergreen.logging.Log;
import erenik.seriousgames.evergreen.player.Player;
import erenik.seriousgames.evergreen.R;
import erenik.seriousgames.evergreen.player.*;
import erenik.seriousgames.evergreen.util.Dice;
import erenik.seriousgames.evergreen.logging.LogType;

public class encounter extends AppCompatActivity
{
    public static List<Enemy> enemies = new ArrayList<Enemy>(); // List of 'em.
    static List<Log> log = new ArrayList<Log>();
    static Random r = new Random();

    static boolean isRandom = false;
    static boolean isAssaultOfTheEvergreen = false;

    static Player player = Player.getSingleton();
    static boolean dead = false;
    static int expGained = 0;

    /// Clears old lists.
    static void NewEncounter(boolean inShelter)
    {
        enemies.clear();
        log.clear();
        Player player = Player.getSingleton();
        player.PrepareForCombat(inShelter);
        dead = false;
        expGained = 0;
        player.consecutiveFleeAttempts = 0;
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
        Log("You encounter " + iAmount + " " + et.name + "s.", LogType.INFO);
    }
    /// Quick simulation
    static void Simulate()
    {
        /// Will vary based on encounter-type and equipped weapons etc.
        int playerFreeAttacks = 1;
        // Start it?
        while(enemies.size() > 0 && player.IsAlive())
        {
            if (playerFreeAttacks > 0)
                --playerFreeAttacks;
            else
                DoEnemyAttackRound();
            if (dead)
                break;
            DoPlayerAttackRound();
        }
        // Copy over relevant stats to the more persistant array of stats.
        player.Set(Stat.HP, player.hp);
        if (dead)
        {
            // Game over?
            App.GameOver();
        }
        else
        {
            Log("You survive the encounter.", LogType.INFO);
            Log("You gain "+expGained+" EXP.", LogType.EXP);
            player.GainEXP(expGained);
        }
        player.log.addAll(log); // Add current log there with all attack stuff.
        if (isRandom)
            player.Adjust(Stat.ENCOUNTERS, -1);
        else if (isAssaultOfTheEvergreen)
            player.Adjust(Stat.ATTACKS_OF_THE_EVERGREEN, -1);
        else {
            System.out.println("Dunno what to adjust after finishing encounter. Fix this?");
            System.exit(14);
        }
        // Spam all log stuff to console?
        // Save?
        player.SaveLocally();
    }
    static void DoEnemyAttackRound()
    {
        for (int i = 0; i < 5 && i < enemies.size(); ++i)
        {
            Enemy e = enemies.get(i);
            if (e.Attack(player))
            {
                Log("You die.", LogType.INFO);
                dead = true;
                break;
            }
        }
    }
    static void DoPlayerAttackRound()
    {
        // Flee? Under 25%?
        if (player.hp < player.maxHP * 0.3f)
        {
            int fleetRetreat = player.Get(Skill.FleetRetreat).Level();
            int fleeCR = (enemies.size()-1) / 2 - fleetRetreat - player.consecutiveFleeAttempts;
            int roll = Dice.RollD6(4); // 4-24, 14 mid, +/- 10
            if (roll > 14 + fleeCR) {
                // Success?
                Log("You run away.", LogType.INFO);
                if (fleetRetreat > 0)
                    expGained += Math.pow(2, fleetRetreat - 1);
                enemies.clear();
                return;
            }
            else
            {
                Log("You try to run away, but fail", LogType.INFO);
                ++player.consecutiveFleeAttempts;
            }
        }
        // Attack?
        Enemy e = enemies.get(0);
        player.Attack(e);
        if (e.hp < 0) {
            Log("The "+e.name+" dies.", LogType.INFO);
            enemies.remove(e);
            expGained += e.exp;
        }
    }

    static void AssaultsOfTheEvergreen()
    {
        isAssaultOfTheEvergreen = true;
        Player player = Player.getSingleton();
        int turn = player.turn;
        System.out.println("turn: "+turn);
        int level = turn / 16;
        List<EnemyType> typesPossible = new ArrayList<EnemyType>();
        for (int i = 0; i < EnemyType.values().length; ++i)
        {
            if (EnemyType.values()[i].level <= level)
                typesPossible.add(EnemyType.values()[i]);
        }

        // Attacks of the evergreen?
        int everGreenTurn = turn % 16;
        int everGreenStage = turn / 16;
        int d3 = 0, d6 = 0, bonus = 0;
        switch(everGreenTurn)
        {
            default: break; // The pattern repeats every 16 turns.
            case 0:  // Turns 16, 32, 48 and 64.
                bonus += everGreenStage + 1;
                d6 += everGreenStage;
            case 15:
                bonus += everGreenStage + 1;
                d6 += everGreenStage;
            case 13:
                d3 += 1;
                d6 += everGreenStage;
            case 10:
                d3 += 1;
                d6 += everGreenStage;
            case 6:
                d3 += 1;
                d6 += everGreenStage;
                bonus += everGreenStage;
                break;
        }
        if (turn == 64)
        {
            d6 *= 2; // Double the dice?
            d3 *= 2;
            bonus *= 2;
        }
        /// Randomly choose type.
        int ri = r.nextInt(typesPossible.size());
        EnemyType et = typesPossible.get(ri);
        int totalEnemies = Dice.RollD3(d3) + Dice.RollD6(d6) + bonus;
        totalEnemies *= et.encounterAmount;
        if (totalEnemies < 1)
            totalEnemies = 1;
        int iAmount = totalEnemies;
        for (int i = 0; i < iAmount; ++i)
        {
            Enemy e = new Enemy(et);
            enemies.add(e);
        }
        Log("Your shelter is attacked by " + iAmount + " " + et.name + "s!", LogType.INFO);
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
