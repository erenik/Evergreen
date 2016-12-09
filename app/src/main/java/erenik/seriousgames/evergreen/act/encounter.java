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
import erenik.seriousgames.evergreen.transport.TransportStat;
import erenik.seriousgames.evergreen.util.Dice;
import erenik.seriousgames.evergreen.logging.LogType;

public class Encounter extends EvergreenActivity
{
    public List<Enemy> enemies = new ArrayList<Enemy>(); // List of 'em.
    List<Log> log = new ArrayList<Log>();
    Random r = new Random();

    boolean isRandom = false;
    boolean isAssaultOfTheEvergreen = false;

    Player player = App.GetPlayer();
    boolean dead = false;
    int creepsKilled = 0, totalCreeps = 0, encounterExp = 0;
    int fleeExp = 0;
    int turn = -1;
    private boolean fled = false;

    static List<Player> playersInvolved = new ArrayList<>();

    /// Clears old lists.
    void NewEncounter(boolean inShelter)
    {
        enemies.clear();
        log.clear();
        playersInvolved.clear();
        playersInvolved.add(player);
        player.PrepareForCombat(inShelter);
        dead = false;
        fleeExp = 0;
        fled = false;
        creepsKilled = 0;
        player.consecutiveFleeAttempts = 0;
        turn = player.Turn();
    }

    private void CalcEncounterEXP()
    {
        encounterExp = (int) player.Turn();
    }

    void Random(Dice dice)
    {
        isRandom = true;
        int level = player.Turn() / 16;
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
        iAmount *= player.CurrentTransport().Get(TransportStat.AmountEnemiesEncounteredRatio);
        iAmount += player.Get(Stat.EMISSIONS) / 25;
        for (int i = 0; i < iAmount; ++i)
        {
            Enemy e = new Enemy(et, player.Get(Stat.EMISSIONS));
            enemies.add(e);
        }
        Log("You Encounter " + iAmount + " " + et.name + "s.", LogType.INFO);
        CalcEncounterEXP();
        totalCreeps = enemies.size();
    }
    /// Quick simulation
    void Simulate()
    {
        /// Will vary based on Encounter-type and equipped weapons etc.
        int playerFreeAttacks = 1;
        // Start it?
        while(enemies.size() > 0 && player.IsAlive())
        {
            if (playerFreeAttacks > 0)
                --playerFreeAttacks;
            else
                DoEnemyAttackRound();
            if (PlayersDead())
                break;
            DoPlayerAttackRound();
            if (fled)
                break;
        }
        // Copy over relevant stats to the more persistant array of stats.
        player.Set(Stat.HP, player.hp);
        if (PlayersDead())
        {
            // Game over?
            player.log.addAll(log); // Add current log there with all attack stuff.
            App.GameOver();
            App.SaveLocally(getBaseContext()); // Save stuffs?
            return;
        }
        else
        {
            Log("You survive the Encounter.", LogType.INFO);
            int expToGain = (enemies.size() > 0? creepsKilled / totalCreeps  : 1 ) * encounterExp + fleeExp;
            if (creepsKilled > 0) {
                Log("You gain " + expToGain + " EXP.", LogType.EXP);
            }
            player.GainEXP(expToGain);
        }
        if (isRandom)
            player.Adjust(Stat.ENCOUNTERS, -1);
        else if (isAssaultOfTheEvergreen)
            player.Adjust(Stat.ATTACKS_OF_THE_EVERGREEN, -1);
        else {
            System.out.println("Dunno what to adjust after finishing Encounter. Fix this?");
            System.exit(14);
        }
        OnEncounterEnded(); // Save, push logs, etc.
    }

    void OnEncounterEnded()
    {
        player.log.addAll(log); // Add current log there with all attack stuff.
        SaveLocally();
    }

    private boolean PlayersDead()
    {
        for (int i = 0; i < playersInvolved.size(); ++i)
        {
            Player p = playersInvolved.get(i);
            if (p.hp > 0)
                return false;
        }
        return true;
    }

    void DoEnemyAttackRound()
    {
        for (int i = 0; i < 5 && i < enemies.size(); ++i)
        {
            Enemy e = enemies.get(i);
            if (e.Attack(player, this))
            {
                Log("You die.", LogType.INFO);
                dead = true;
                break;
            }
        }
    }
    void DoPlayerAttackRound()
    {
        // Flee? Under 25%?
        if (player.hp < player.maxHP * 0.3f && !isAssaultOfTheEvergreen)
        {
            int fleetRetreat = player.Get(Skill.FleetRetreat).Level();
            int fleeCR = (enemies.size()-1) / 2 - fleetRetreat - player.consecutiveFleeAttempts;
            fleeCR -= player.CurrentTransport().Get(TransportStat.FleeBonus); // Decrease challenge rating with transport flee bonus.
            int roll = Dice.RollD6(4); // 4-24, 14 mid, +/- 10
            if (roll > 14 + fleeCR) {
                // Success?
                Log("You run away.", LogType.INFO);
                if (fleetRetreat > 0)
                    fleeExp = (int) Math.pow(2, fleetRetreat - 1);
                fled = true;
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
        player.Attack(e, this);
        if (e.hp <= 0) {
            Log("The " + e.Name() + " dies.", LogType.INFO);
            enemies.remove(e);
            ++creepsKilled;
        }
    }

    void AssaultsOfTheEvergreen()
    {
        isAssaultOfTheEvergreen = true;
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
        totalEnemies += player.Get(Stat.EMISSIONS) / 12.5;
        totalEnemies *= et.encounterAmount;
        if (totalEnemies < 1)
            totalEnemies = 1;
        int iAmount = totalEnemies;
        for (int i = 0; i < iAmount; ++i)
        {
            Enemy e = new Enemy(et, player.Get(Stat.EMISSIONS));
            enemies.add(e);
        }
        Log("Your shelter is attacked by " + iAmount + " " + et.name + "s!", LogType.INFO);
        CalcEncounterEXP();
        totalCreeps = enemies.size();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter);
        // Hook up some GUI stuffs.

        // Update gui.
        // ?

    }
    void UpdateGui()
    {
        // HP, attack, defense, debuffs, enemies.
    }

    public void Log(String s, LogType type)
    {
        // Add to log to present/update to ui?
        Log l = new Log(s, type);
        log.add(l);
    }

    public void AbandonedShelter()
    {
        System.out.println("Abandoned shelters decrease from "+player.Get(Stat.ABANDONED_SHELTER));
        player.Adjust(Stat.ABANDONED_SHELTER, -1);;
        Log("Found some food", LogType.INFO);
        player.Adjust(Stat.FOOD, 1);
        // TODO: Add more descriptions and the random results.
        OnEncounterEnded(); // Save, push logs, etc.
    }

    public void RandomPlayerShelter()
    {
        player.Adjust(Stat.RANDOM_PLAYERS_SHELTERS, -1); // Decrease the events to spawn...
        player.Adjust(Stat.RandomPlayerFound, 1); // Increase this. Will be sent to server to see who you found.
        Log("Found some food", LogType.INFO);
        player.Adjust(Stat.FOOD, 1);
        // TODO: Perhaps add more actions later.
        OnEncounterEnded(); // Save, push logs, etc.
    }
}
