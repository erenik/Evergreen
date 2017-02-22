package erenik.evergreen.common.encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.combat.Combatable;
import erenik.evergreen.common.combat.Enemy;
import erenik.evergreen.common.combat.EnemyType;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.player.Finding;
import erenik.evergreen.common.player.Skill;
import erenik.evergreen.common.player.Stat;
import erenik.evergreen.common.player.TransportStat;
import erenik.evergreen.util.Dice;

/**
 * Created by Emil on 2016-12-17.
 */
public class Encounter {
    public List<Combatable> attackers = new ArrayList<>(), defenders = new ArrayList<>(), combatants = new ArrayList<>();
//    Player player = null; // App.GetPlayer(); // o-o
//    Player attackingPlayer = null; // For PvP.
//    static List<Player> playersInvolved = new ArrayList<>();
 //   public List<Enemy> enemies = new ArrayList<Enemy>(); // List of 'em.
    List<Log> log = new ArrayList<Log>();
    Random r = new Random();
    boolean isRandom = false;
    boolean isAssaultOfTheEvergreen = false;
    boolean dead = false;
    int creepsKilled = 0, totalCreeps = 0, encounterExp = 0;
    int fleeExp = 0;
    int turn = -1;

    public List<EncounterListener> listeners = new ArrayList<>();

    /// Creates an encounter with target player as main involved player.
    public Encounter() {
    }
    public Encounter(Player attacker, Player defender) {
        NewEncounter(true);
        attackers.add(attacker);
        defenders.add(defender);
        PrepareForCombat();
    }
    private void PrepareForCombat() {
        List<Player> players = GetInvolvedPlayers();
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            p.PrepareForCombat(!p.isAttacker);
            p.runsAway = true;
            p.runAwayAtHPPercentage = 0.25f; // Should check the stats-preferences.
        }
    }
    public Encounter(Finding fromFinding, Player andMainPlayer) {
        switch(fromFinding) {
            case AttacksOfTheEvergreen:
                NewEncounter(true);
                AssaultsOfTheEvergreen(andMainPlayer);
                break;
            case RandomEncounter:
                NewEncounter(false);
                RandomMonsterEncounter(new Dice(3, 2, 0), andMainPlayer);
                break;
            default:
                System.out.println("Bad finding!!!!");
                System.exit(0);
        }
        PrepareForCombat();
    }

    /// Clears old lists.
    public void NewEncounter(boolean inShelter) {
        attackers.clear();
        defenders.clear();
        combatants.clear();
        log.clear();
        dead = false;
        fleeExp = 0;
        creepsKilled = 0;
    }

    // Checks total exp, compares with progress in the encounter?
    private void CalcEncounterEXP() {
        encounterExp = 0;
        for (int i = 0; i < combatants.size(); ++i)
            encounterExp += combatants.get(i).exp;
    }
    /// Generates a 'random' encounter, based on player statistics?
    public void RandomMonsterEncounter(Dice dice, Player attackedPlayer) {
        isRandom = true;
        System.out.println("Attacked player: "+attackedPlayer.name);
        AddDefenders(attackedPlayer);
        int level = attackedPlayer.TurnsSurvived() / 16;
        List<EnemyType> typesPossible = new ArrayList<EnemyType>();
        for (int i = 0; i < EnemyType.values().length; ++i) {
            if (EnemyType.values()[i].level <= level)
                typesPossible.add(EnemyType.values()[i]);
        }
        /// Randomly choose type.
        int ri = r.nextInt(typesPossible.size());
        EnemyType et = typesPossible.get(ri);
        dice.dice *= et.encounterAmount;
        dice.bonus *= et.encounterAmount;
        int iAmount = dice.Roll();
        iAmount *= attackedPlayer.CurrentTransport().Get(TransportStat.AmountEnemiesEncounteredRatio);
        iAmount += attackedPlayer.Get(Stat.EMISSIONS) / 25;
        iAmount = Math.max(iAmount, 1);
        for (int i = 0; i < iAmount; ++i)
        {
            Enemy e = new Enemy(et, attackedPlayer.Get(Stat.EMISSIONS));
            e.isAttacker = true;
            AddCombatant(e);
        }
        Log("You encounter " + iAmount + " " + et.name + "s.", LogType.INFO);
        CalcEncounterEXP();
        totalCreeps = iAmount;
    }
    /// Step-wise iterative Simulation
    public void Simulate() {
        PrepareForCombat(); // Prepare before simulation. Should make all prepared...
        System.out.println("Simulating encounter");
        /// Will vary based on EncounterActivity-type and equipped weapons etc.
        int defenderFreeAttacks = 1;
        // Start it?
        while(AttackersAndDefendersStillAliveAndDidntRunAway()){
            System.out.println("Start of attack round, participants still alive.");
            if (defenderFreeAttacks > 0)
                --defenderFreeAttacks;
            else
                DoCombatRound(attackers, defenders);
            DoCombatRound(defenders, attackers);
        }
        System.out.println("Combat over");
        // Copy over relevant stats to the more persistent array of stats.
        List<Player> players = GetInvolvedPlayers();
        for (int i = 0; i < players.size(); ++i) {
            Player player = players.get(i);
            player.Set(Stat.HP, player.hp);
        }
        if (PlayersDead()) { // Game over?
            System.out.println("Players dead");
            OnEncounterEnded();
            return;
        }
        else {
            Log("You survive the encounter!", LogType.SUCCESS);
            int expToGain = (totalCreeps > 0? CreepsKilled() / totalCreeps  : 1 ) * encounterExp + fleeExp;
            if (creepsKilled > 0) {
                Log("You gain " + expToGain + " EXP.", LogType.EXP);
            }
            for (int i = 0; i < players.size(); ++i) {
                players.get(i).GainEXP(expToGain);
            }
        }
        OnEncounterEnded(); // Save, push logs, etc.
    }

    private int CreepsKilled() {
        int tot = 0;
        for (int i = 0; i < combatants.size(); ++i){
            Combatable c = combatants.get(i);
            if (c instanceof Player)
                continue;
            if (c.hp <= 0)
                ++tot;
        }
        return 0;
    }

    private boolean PlayersDead() {
        List<Player> players = GetInvolvedPlayers();
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            if (p.hp > 0)
                return false;
        }
        return true;
    }

    private void AdjustPlayers(List<Player> players, Stat stat, float adjustment) {
        for (int i = 0; i < players.size(); ++i)
            players.get(i).Adjust(stat, adjustment);
    }

    private boolean AttackersAndDefendersStillAliveAndDidntRunAway() {
        int attackersAlive = 0, defendersAlive = 0;
        for (int i = 0; i < combatants.size(); ++i) {
            Combatable c = combatants.get(i);
            if (c.ranAway)
                continue;
            if (c.isAttacker && c.hp > 0)
                ++attackersAlive;
            else if (c.isAttacker == false && c.hp > 0)
                ++defendersAlive;
        }
        System.out.println("Attackers alive: "+attackersAlive+" Defenders alive: "+defendersAlive);
        return attackersAlive > 0 && defendersAlive > 0;
    }
    // When the encounter ends, paste in the combat log into their own logs.
    private void OnEncounterEnded() {
        List<Player> players = GetInvolvedPlayers();
        for (int i = 0; i < players.size(); ++i) {
            Player player = players.get(i);
            player.log.addAll(log); // Add current log there with all attack stuff.
            for (int j = 0; j < listeners.size(); ++j)
                listeners.get(j).OnEncounterEnded(this);
            if (player.IsAlive() == false) {
                player.InformListeners();
            }
        }
        System.out.println("The encounter ended.");
    }

    private List<Player> GetInvolvedPlayers() {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < combatants.size(); ++i){
            Combatable comb = combatants.get(i);
            if (comb instanceof Player){
                players.add((Player) comb);
            }
        }
        return players;
    }

    private Combatable GetTarget(List<Combatable> possibleTargets, Combatable attacker) {
        for (int i = 0; i < possibleTargets.size(); ++i){
            Combatable c = possibleTargets.get(i);
            if (c.hp > 0)
                return c;
        }
        return null;
    }
    // Evaluates a combat-round for one side of foes attacking another.
    void DoCombatRound(List<Combatable> activeCombatants, List<Combatable> opposingCombatants){
        for (int i = 0; i < activeCombatants.size(); ++i) {
            Combatable c = activeCombatants.get(i);

            // Flee? Under 25%?
            if (c.runsAway && (c.hp / c.maxHP) <  c.runAwayAtHPPercentage) {
                System.out.println("Trying to run away!");
                int fleetRetreat = c.fleeSkill;
                int fleeCR = (opposingCombatants.size()-1) / 2 - fleetRetreat - c.consecutiveFleeAttempts;
                fleeCR -= c.fleeBonusFromTransport; // Decrease challenge rating with transport flee bonus.
                int roll = Dice.RollD6(4); // 4-24, 14 mid, +/- 10
                if (roll > 14 + fleeCR) {
                    // Success?
                    LogEnc(new Log(c instanceof Player? LogTextID.playerFledFromCombat : LogTextID.fledFromCombat, LogType.INFO, c.name));
                    if (fleetRetreat > 0)
                        fleeExp = (int) Math.pow(2, fleetRetreat - 1);
                    c.ranAway = true;
                    return;
                }
                else {
                    LogEnc(new Log(c instanceof Player? LogTextID.playerTriedToFlee : LogTextID.triedToFlee, LogType.ACTION_FAILURE, c.name));
                    ++c.consecutiveFleeAttempts;
                }
            }
            Combatable target = GetTarget(opposingCombatants, c);
            if (target == null) {
                System.out.println("Found no target, skipping");
                continue;
            }
            c.Attack(target, this);
        }
    }

    public void AssaultsOfTheEvergreen(Player playerBeingAssaulted) {
        isAssaultOfTheEvergreen = true;
        System.out.println("turn: "+turn);
        int level = turn / 16;
        List<EnemyType> typesPossible = new ArrayList<EnemyType>();
        for (int i = 0; i < EnemyType.values().length; ++i) {
            if (EnemyType.values()[i].level <= level)
                typesPossible.add(EnemyType.values()[i]);
        }
        // Attacks of the evergreen?
        int everGreenTurn = turn % 16;
        int everGreenStage = turn / 16;
        int d3 = 0, d6 = 0, bonus = 0;
        switch(everGreenTurn) {
            default: break; // The pattern repeats every 16 turns.
            case 0:  // Turns 16, 32, 48 and 64.
                bonus += everGreenStage + 1;
                d6 += everGreenStage;
            case 15:
                bonus += 1;
                d6 += everGreenStage;
            case 13:
                bonus += 1;
                d3 += everGreenStage;
            case 10:
                bonus += 1;
                d3 += everGreenStage;
            case 6:
                d3 += everGreenStage + 1;
                bonus += everGreenStage + 1;
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
        int totalEmissions = 0;
        for (int i = 0; i < GetInvolvedPlayers().size(); ++i)
            totalEmissions += GetInvolvedPlayers().get(i).Get(Stat.EMISSIONS);
        totalEnemies +=  totalEmissions / 12.5;
        totalEnemies *= et.encounterAmount;
        if (totalEnemies < 1)
            totalEnemies = 1;
        int iAmount = totalEnemies;
        for (int i = 0; i < iAmount; ++i) {
            Enemy e = new Enemy(et, totalEmissions);
            e.isAttacker = true;
            AddCombatant(e);
        }
        AddDefenders(playerBeingAssaulted);
        LogEnc(new Log(LogTextID.shelterAttacked, LogType.INFO, iAmount+"", et.name));
        //Log("Your shelter is attacked by " + iAmount + " " + et.name + "s!", LogType.INFO);
        CalcEncounterEXP();
        totalCreeps = iAmount;
    }
    public void Log(String s, LogType type) {
        // Add to log to present/update to ui?
        System.out.println("Encounter log: "+s);
        Log l = new Log(s, type);
        log.add(l);
    }
    public void LogEnc(Log logMsg) {
        // Add to log to present/update to ui?
        System.out.println("Encounter log: "+logMsg);
        log.add(logMsg);
    }
    public void AbandonedShelter() {
        /*
        System.out.println("Abandoned shelters decrease from "+player.Get(Stat.ABANDONED_SHELTER));
        player.Adjust(Stat.ABANDONED_SHELTER, -1);;
        Log("Found some food", LogType.INFO);
        player.Adjust(Stat.FOOD, 1);
        */
        // TODO: Add more descriptions and the random results.
        OnEncounterEnded(); // Save, push logs, etc.
    }

    public void RandomPlayerShelter() {
        /*
        player.Adjust(Stat.RANDOM_PLAYERS_SHELTERS, -1); // Decrease the events to spawn...
        player.Adjust(Stat.RandomPlayerFound, 1); // Increase this. Will be sent to server to see who you found.
        Log("Found some food", LogType.INFO);
        player.Adjust(Stat.FOOD, 1);
        */
        // TODO: Perhaps add more actions later.
        OnEncounterEnded(); // Save, push logs, etc.
    }

    public void AddCombatant(Combatable combatant) {
        if (combatant.isAttacker)
            attackers.add(combatant);
        else
            defenders.add(combatant);
        combatants.add(combatant);
    }
    /// Adds a player as a defender in this attack.
    public void AddDefenders(Player player) {
        defenders.add(player);
        combatants.add(player);
    }
}
