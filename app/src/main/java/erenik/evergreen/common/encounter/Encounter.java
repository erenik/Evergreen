package erenik.evergreen.common.encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.combat.Enemy;
import erenik.evergreen.common.combat.EnemyType;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.player.Finding;
import erenik.evergreen.common.player.Skill;
import erenik.evergreen.common.player.Stat;
import erenik.evergreen.common.player.TransportStat;
import erenik.evergreen.util.Dice;

/**
 * Created by Emil on 2016-12-17.
 */
public class Encounter
{
    Player player = null; // App.GetPlayer(); // o-o
    Player attackingPlayer = null; // For PvP.
    static List<Player> playersInvolved = new ArrayList<>();
    public List<Enemy> enemies = new ArrayList<Enemy>(); // List of 'em.
    List<Log> log = new ArrayList<Log>();
    Random r = new Random();
    boolean isRandom = false;
    boolean isAssaultOfTheEvergreen = false;
    boolean dead = false;
    int creepsKilled = 0, totalCreeps = 0, encounterExp = 0;
    int fleeExp = 0;
    int turn = -1;
    private boolean fled = false;


    public List<EncounterListener> listeners = new ArrayList<>();

    /// Creates an encounter with target player as main involved player.
    public Encounter(Player mainPlayer)
    {
        player = mainPlayer;
    }
    public Encounter(Player attacker, Player defender)
    {
        player = defender;
        attackingPlayer = attacker;
        NewEncounter(true);
        // Prepare for combat for the attackingPlayer, the rest should be as usual.
        attackingPlayer.PrepareForCombat(false); // Not in shelter, since attacking another's shelter.
    }
    public Encounter(Finding fromFinding, Player andMainPlayer)
    {
        player = andMainPlayer;
        switch(fromFinding)
        {
            case AttacksOfTheEvergreen:
                NewEncounter(true);
                AssaultsOfTheEvergreen();
                break;
            case Encounter:
                NewEncounter(false);
                Random(new Dice(3, 2, 0));
                break;
        }
    }

    /// Clears old lists.
    public void NewEncounter(boolean inShelter)
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

    public void Random(Dice dice)
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
        Log("You encounter " + iAmount + " " + et.name + "s.", LogType.INFO);
        CalcEncounterEXP();
        totalCreeps = enemies.size();
    }
    /// Quick simulation
    public void Simulate() {
        /// Will vary based on EncounterActivity-type and equipped weapons etc.
        int playerFreeAttacks = 1;
        // Start it?
        while(enemies.size() > 0  && player.IsAlive()) // PvE
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
        /// PvP
        while(attackingPlayer != null && player.IsAlive()) {
            if (playerFreeAttacks > 0)
                --playerFreeAttacks;
            else
                DoEnemyAttackRound();
            if (PlayersDead())
                break;
            DoPlayerAttackRound();
            if (fled)
                break;
            if (attackingPlayer.IsAlive() == false) // Attacking player died?
                Log("The attacking player was defeated!", LogType.SUCCESS);
            break;
        }

        // Copy over relevant stats to the more persistent array of stats.
        player.Set(Stat.HP, player.hp);
        if (PlayersDead())
        {
            // Game over?
            OnEncounterEnded();
            return;
        }
        else
        {
            Log("You survive the encounter.", LogType.INFO);
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
            System.out.println("Assume it was a PvP encounter.");
//            System.out.println("Dunno what to adjust after finishing encounter. Fix this?");
//            System.exit(14);
        }
        OnEncounterEnded(); // Save, push logs, etc.
    }

    private void OnEncounterEnded() {
        player.log.addAll(log); // Add current log there with all attack stuff.
        for (int i = 0; i < listeners.size(); ++i)
            listeners.get(i).OnEncounterEnded(this);
        if (player.IsAlive() == false)
            player.InformListeners();
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

    void DoEnemyAttackRound() {
        if (attackingPlayer != null) // Player attack, fairly simple.
            attackingPlayer.Attack(player, this);
        for (int i = 0; i < 5 && i < enemies.size(); ++i) {
            Enemy e = enemies.get(i);
            if (e.Attack(player, this))
            {
                player.Log("You die.", LogType.INFO);
                dead = true;
                break;
            }
        }
    }
    void DoPlayerAttackRound() {
        // Flee? Under 25%?
        if (player.hp < player.maxHP * 0.3f && !isAssaultOfTheEvergreen) {
            int fleetRetreat = player.Get(Skill.FleetRetreat).Level();
            int fleeCR = (enemies.size()-1) / 2 - fleetRetreat - player.consecutiveFleeAttempts;
            fleeCR -= player.CurrentTransport().Get(TransportStat.FleeBonus); // Decrease challenge rating with transport flee bonus.
            int roll = Dice.RollD6(4); // 4-24, 14 mid, +/- 10
            if (roll > 14 + fleeCR) {
                // Success?
                player.Log("You run away.", LogType.INFO);
                if (fleetRetreat > 0)
                    fleeExp = (int) Math.pow(2, fleetRetreat - 1);
                fled = true;
                return;
            }
            else {
                player.Log("You try to run away, but fail", LogType.INFO);
                ++player.consecutiveFleeAttempts;
            }
        }
        // Attack?
        if (enemies.size() > 0 ) {
            Enemy e = enemies.get(0);
            player.Attack(e, this);
            if (e.hp <= 0) {
                Log("The " + e.Name() + " dies.", LogType.INFO);
                enemies.remove(e);
                ++creepsKilled;
            }
        }
        else if (attackingPlayer != null)
            player.Attack(attackingPlayer, this); // Should maybe do some check afterwards too, yah?
    }

    public void AssaultsOfTheEvergreen()
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
    public void Log(String s, LogType type)
    {
        // Add to log to present/update to ui?
        System.out.println("Encounter log: "+s);
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
