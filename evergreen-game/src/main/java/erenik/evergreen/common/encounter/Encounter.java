package erenik.evergreen.common.encounter;

import java.util.Random;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.combat.Combatable;
import erenik.evergreen.common.combat.Enemy;
import erenik.evergreen.common.combat.EnemyType;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.player.Finding;
import erenik.evergreen.common.player.Stat;
import erenik.evergreen.common.player.Statistic;
import erenik.evergreen.common.player.TransportStat;
import erenik.util.Dice;
import erenik.util.EList;
import erenik.util.Printer;

import static erenik.evergreen.common.player.Stat.TurnSurvived;

/**
 * Created by Emil on 2016-12-17.
 */
public class Encounter {
    // o-o that spawns this encounter?
    private Finding finding = null;
    public EList<Combatable> attackers = new EList<>(), defenders = new EList<>(), combatants = new EList<>();
//    Player player = null; // App.GetPlayer(); // o-o
//    Player attackingPlayer = null; // For PvP.
//    static EList<Player> playersInvolved = new EList<>();
 //   public EList<Enemy> enemies = new EList<Enemy>(); // EList of 'em.
    EList<Log> log = new EList<Log>();
    Random r = new Random();
    boolean isRandom = false;
    boolean isAssaultOfTheEvergreen = false;
    boolean fleeable = true;
    boolean dead = false;
    int fleeExp = 0;
    private int turnCreated = -1;
    boolean pvp = false;

    /// Since we have 1 enemy type per encounter right now, may as well keep it here.
    EnemyType enemyType;
    private Player primaryAttackedPlayer;

    int TurnCreated(){ return turnCreated; };

    static final int maxEnemyAttacksPerRound = 3;

//    public EList<EncounterListener> listeners = new EList<>();

    /// Creates an encounter with target player as main involved player.
    public Encounter(int turn) {
        turnCreated = turn;
    }
    public Encounter(Player attacker, Player defender) {
        NewEncounter((int) attacker.Get(TurnSurvived));
        attacker.isAttacker = true;
        defender.isAttacker = false;
        pvp = true;
        attackers.add(attacker);
        defenders.add(defender);
        combatants.add(attacker);
        combatants.add(defender);
        PrepareForCombat();
    }
    private void PrepareForCombat() {
        EList<Player> players = GetInvolvedPlayers();
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
            p.PrepareForCombat(!p.isAttacker); // Does what?
            p.runsAway = true;
            p.runAwayAtHPPercentage = 0.25f; // Should check the stats-preferences.
            p.wantedToFleeButCouldNotDesperation = 0;
        }
    }
    // Total emissions of all participating players. Sum.
    int totalEmissions = 0;
    void CalculateTurnAndTotalEmissions(){
        // Calculate turn as the maximum of all involved players?
        totalEmissions = 0;
        turnCreated = -1;
      //  Printer.out("Num involved players: "+GetInvolvedPlayers().size());
        for (int i = 0; i < GetInvolvedPlayers().size(); ++i) {
            Player player = GetInvolvedPlayers().get(i);
            totalEmissions += player.TotalEmissions();
            if (player.Get(TurnSurvived) > turnCreated)
                turnCreated = (int) player.Get(TurnSurvived);
        }
        if (!pvp && (totalEmissions < 0 || turnCreated < 0)) {
            PrintAllCombatants();
        //    Printer.out("Emissions: "+totalEmissions+" turnCreated: "+turnCreated);
            new Exception().printStackTrace();
            System.exit(12);
        }
    }

    private void PrintAllCombatants() {
        Printer.out(""+finding+" "+finding.name());
        Printer.out("tot Emissions: "+totalEmissions+" turnCreated: "+turnCreated);
        Printer.out("Combatants: "+combatants.size());
        for (int i = 0; i < combatants.size(); ++i){
            Combatable c =  combatants.get(i);
            Printer.out(c.name+" attacker: "+c.isAttacker);
        }
    }

    /// Adds the player as defending player in this encounter, and randomizes an encounter.
    public Encounter(Finding fromFinding, Player andMainPlayer) {
        NewEncounter((int) andMainPlayer.Get(TurnSurvived));
        AddDefenders(andMainPlayer);
        CalculateTurnAndTotalEmissions();
        finding = fromFinding;
        switch(fromFinding) {
            case AttacksOfTheEvergreen:
                AssaultsOfTheEvergreen(andMainPlayer);
                break;
            case RandomEncounter:
                RandomMonsterEncounter(0, 2, 0, andMainPlayer); // 2d3, no bonus
                break;
            default:
                Printer.out("Bad finding!!!!");
                System.exit(0);
        }
        PrepareForCombat();
    }

    /// Clears old lists.
    public void NewEncounter(int day) {
        attackers.clear();
        defenders.clear();
        combatants.clear();
        log.clear();
        dead = false;
        fleeExp = 0;
        turnCreated = day;
        totalEmissions = 0;
    }

    // Checks total exp, compares with progress in the encounter?
    private int CalcEncounterEXP() {
        int encounterExp = 0;
        for (int i = 0; i < combatants.size(); ++i) {
            if (combatants.get(i).hp <= 0)
                encounterExp += combatants.get(i).exp;
        }
        return encounterExp;
    }
    /// Step-wise iterative Simulation
    public void Simulate() {
        PrepareForCombat(); // Prepare before simulation. Should make all prepared...
        if (PlayersDead()){
            if (pvp)
                Printer.out("Ending combat prematurely? TRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");
            return;
        }// Skip simulation if players already dead previously?
//        Printer.out("Simulating encounter");
        if (pvp)
           ;// Printer.out("PVP START!");

        // If attacks of the evergreen - allow ranged weapons...?


        /// Will vary based on EncounterActivity-type and equipped weapons etc.
        int defenderFreeAttacks = 1;
        // Start it?
        while(AttackersAndDefendersStillAliveAndDidntRunAway()){
            for (int i = 0; i < combatants.size(); ++i)
                combatants.get(i).OnTurnStart(this);
        //    Printer.out("Start of attack round, participants still alive.");
            if (defenderFreeAttacks > 0)
                --defenderFreeAttacks;
            else
                DoCombatRound(attackers, defenders);
            DoCombatRound(defenders, attackers);
        }
//        Printer.out("Combat over");

        int expToGain = 0;
        if (enemyType != null) {
        //    Printer.out("CreepsAlive: "+CreepsAlive()+" killed: "+CreepsKilled()+" total: "+CreepsTotal()+" enemyTypeEXP: "+enemyType.exp+" fleeExp: "+fleeExp);
            expToGain = (int) (CreepsKilled() * enemyType.exp + fleeExp);
        }
        // So you won, did you defeat the other player?
        for (int j = 0; j < LosingPlayers().size(); ++j) {
            expToGain += 5 + LosingPlayers().get(j).Get(TurnSurvived) / 2;
        }
        // Copy over relevant stats to the more persistent array of stats.
        EList<Player> players = GetInvolvedPlayers();
        for (int i = 0; i < players.size(); ++i) {
            Player player = players.get(i);
            player.Set(Stat.HP, player.hp);
            player.Adjust(Statistic.TotalEncountersParticipatedIn, 1);
            /// For the players that are alive...
            if (player.IsAliveInCombat()){
                LogEnc(new Log(LogTextID.encounterSurvived, LogType.SUCCESS));
//                player
                if (expToGain > 0) {
                    LogEnc(new Log(LogTextID.expGained, LogType.EXP, ""+expToGain));
                }
                player.GainEXP(expToGain);
                player.Adjust(Statistic.EXPGainedInCombat, expToGain);
                if (isAssaultOfTheEvergreen)
                    player.Adjust(Statistic.EvergreenEncountersSurvived, 1);
            }
            else { // Else - if dead
                player.ReviveIfKnockedOut();                 /// Revive etc. now? instead of elsewhere...?
            }
        }
    }

    /// Only call when u know the combat is already over.
    private EList<Player> LosingPlayers() {
        if (pvp == false) // No stuff here.
            return new EList<>();
        if (StillAliveAndDidntAllRunAway(attackers))
            return new EList<Player>(defenders);
        else
            return new EList<Player>(attackers);
    }

    private boolean AttackersStillAliveAndDidntRunAway() {
        EList<Combatable> attackers = GetAttackers();
        return StillAliveAndDidntAllRunAway(attackers);
    }

    private EList<Combatable> GetAttackers() {
        return attackers;
    }

    private boolean StillAliveAndDidntAllRunAway(EList<Combatable> specificCombatants) {
        int numStillThere = 0;
        for (int i = 0; i < specificCombatants.size(); ++i) {
            Combatable c = specificCombatants.get(i);
            if (c.ranAway)
                continue;
            if (c.hp <= 0)
                continue;
            ++numStillThere;
        }
        return numStillThere > 0;
    }

    private int CreepsAlive() {
        return CreepsTotal() - CreepsKilled();
    }

    private int CreepsTotal(){
        int tot = 0;
        for (int i = 0; i < combatants.size(); ++i){
            Combatable c = combatants.get(i);
            if (c instanceof Player)
                continue;
            ++tot;
        }
        return tot;
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
        return tot;
    }

    private boolean PlayersDead() {
        EList<Player> players = GetInvolvedPlayers();
        for (int i = 0; i < players.size(); ++i){
            Player p = players.get(i);
        //    Printer.out("Player hp: "+p.hp);
            if (p.hp > 0)
                return false;
        }
        return true;
    }

    private void AdjustPlayers(EList<Player> players, Stat stat, float adjustment) {
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
//        Printer.out("Attackers alive: "+attackersAlive+" Defenders alive: "+defendersAlive);
        if (attackersAlive > 100) {
            Printer.out(">100 attackers, wtf.");
            PrintAllCombatants();
            new Exception().printStackTrace();
            System.exit(2);
        }
        return attackersAlive > 0 && defendersAlive > 0;
    }

    private EList<Player> GetInvolvedPlayers() {
        EList<Player> players = new EList<>();
        for (int i = 0; i < combatants.size(); ++i){
            Combatable comb = combatants.get(i);
            if (comb instanceof Player){
                players.add((Player) comb);
            }
        }
        return players;
    }

    private Combatable GetTarget(EList<Combatable> possibleTargets, Combatable attacker) {
        for (int i = 0; i < possibleTargets.size(); ++i){
            Combatable c = possibleTargets.get(i);
            if (c.hp > 0)
                return c;
        }
        return null;
    }
    // Evaluates a combat-round for one side of foes attacking another.
    void DoCombatRound(EList<Combatable> activeCombatants, EList<Combatable> opposingCombatants){
        int numEnemiesAttacked = 0;
        for (int i = 0; i < activeCombatants.size(); ++i) {
            Combatable c = activeCombatants.get(i);
            if (c.stunnedRounds > 0) // No movement while stunned. Inform if player?
                continue;
            // LogEnc(new Log("Skipping remaining enemies", LogType.INFO));
            if (c.hp <= 0 || c.ranAway) { // Skip those not relevant anymore - i.e. dead or ran away.
           //     LogEnc(new Log("Skipping due to hp: "+c.hp+" for combatant "+c.name, LogType.INFO));
                continue;
            }
            if (c instanceof Enemy) { /// Skip those dead first before incrementing num attacked.. the dead don't usually attack...
                ++numEnemiesAttacked;
                if (numEnemiesAttacked > maxEnemyAttacksPerRound) {
                    //     LogEnc(new Log("Skipping remaining enemies", LogType.INFO));
                   // Printer.out("Skipping remaining "+(activeCombatants.size() - i)+" attackers this round");
                    break;
                }
            }
            // Flee? Under 25%? Only if the encounter permits.
            if (c.runsAway && (c.hp / c.maxHP) <  c.runAwayAtHPPercentage) {
                if (fleeable){
                    //   LogEnc(new Log(c.name+" tries to run away!", LogType.INFO));
//                Printer.out("Trying to run away!");
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
                } else { // Not flee-able.
                    LogEnc(new Log(LogTextID.wantsToFleeButCannot, LogType.ENC_INFO, c.name));
                    ++c.wantedToFleeButCouldNotDesperation;
                }
            }
            DoAttackRound(c, opposingCombatants);
        }
    }

    private void DoAttackRound(Combatable c, EList<Combatable> opposingCombatants) {
        for (int i = 0; i < c.attacksPerTurn; ++i) {
            Combatable target = GetTarget(opposingCombatants, c);
            if (target == null) {
                continue;
            }
            c.Attack(target, this);
        }
    }

    /// Generates a 'random' encounter, based on player statistics?
    public void RandomMonsterEncounter(int d3, int d6, int bonus, Player attackedPlayer) {
        primaryAttackedPlayer = attackedPlayer;
        isRandom = true;
        enemyType = RandomEnemyTypeMaxLevel(turnCreated / 16); // max lvl 0 creates up to turn 16, then max lvl 1, 32 - L2, 48 - L3, 64 - L4
        GenerateEnemies(d3, d6, bonus);
        LogEnc(new Log(LogTextID.encounterNumMonsters, LogType.INFO, ""+CreepsTotal(), enemyType.name)); // Log for when meeting random encounters.
        CalcEncounterEXP();
    }
    public void AssaultsOfTheEvergreen(Player playerBeingAssaulted) {
        primaryAttackedPlayer = playerBeingAssaulted;
        isAssaultOfTheEvergreen = true;
        fleeable = false;
        LogEnc(new Log(LogTextID.assaultsOfTheEvergreen, LogType.INFO));
        Printer.out("Assaults of the Evergreen, - turn: "+turnCreated);
        // Attacks of the evergreen?
        int everGreenTurn = turnCreated % 16;
        int level = turnCreated / 16; // 0-15= 0, 16-31= 1, 32-47= 2, 48-63= 3, 64= 4
        int d3 = 0, d6 = 0, bonus = 0;
        switch(everGreenTurn) {
            default: break; // The pattern repeats every 16 turns.
            case 0:  // Turns 16, 32, 48 and 64.
                bonus += level + 1;
            case 15:
                bonus += 1;
                d6 += level;
            case 13:
                bonus += 1;
                d3 += level;
            case 10:
                bonus += 1;
            case 6:
                d3 += level + 1;
                bonus += level + 1;
                break;
        }
        enemyType = RandomEnemyType(level);
        switch (level){
            case 0: // Turns 0-15
            case 1: // Turns 16-31
            case 2: // Turns 32-47
            case 3: // Turns 48-63
                break; // Use the above.
            case 4: // Turns 64 and upward.
                enemyType = RandomEnemyType(5); // After turn 64, let any monster attack except the gaia protector? Or at Level 5 for end-game monsters?
                break;
        }
        if (turnCreated == 64) {
            d3 = d6 = 0;
            bonus = 1;
            enemyType = EnemyType.GaiaProtector; // Final boss, yo.
        }
        int numGenerated = GenerateEnemies(d3, d6, bonus);
        AddDefenders(playerBeingAssaulted);
        LogEnc(new Log(LogTextID.shelterAttacked, LogType.INFO, numGenerated+"", enemyType.name));
        CalcEncounterEXP();
    }
    // Populates a number of enemies for the encounter based on given number of dices. Total emissions and enemy-type encounter amount ratios are taken into account within.
    private int GenerateEnemies(int d3, int d6, int bonus) {
        int totalEnemies = Dice.RollD3(d3) + Dice.RollD6(d6) + bonus;
     //   Printer.out("totalEnemies from initial dice roll: "+totalEnemies);
        totalEnemies *= enemyType.encounterAmount;
        if (isAssaultOfTheEvergreen)
            totalEnemies +=  totalEmissions / 25; // + 1 enemy for each 12.5 emissions? so 8 at 100? shouldn't be too bad.. o.O
        else {
            totalEnemies *= primaryAttackedPlayer.Get(TransportStat.AmountEnemiesEncounteredRatio);
            totalEnemies += totalEmissions / 50;
        }
    //    Printer.out("totalEnemies: "+totalEnemies);
        if (totalEnemies < 1)
            totalEnemies = 1;
        int iAmount = totalEnemies;
        for (int i = 0; i < iAmount; ++i) {
            Enemy e = new Enemy(enemyType, totalEmissions, turnCreated, this);
            e.isAttacker = true;
            AddCombatant(e);
        }
        return iAmount;
    }

    private EnemyType RandomEnemyTypeMaxLevel(int maxLevel) {
        EList<EnemyType> typesPossible = new EList<EnemyType>();
        for (int i = 0; i < EnemyType.values().length; ++i) {
            if (EnemyType.values()[i].level <= maxLevel)
                typesPossible.add(EnemyType.values()[i]);
        }
        /// Randomly choose type.
        int ri = r.nextInt(typesPossible.size());
        EnemyType et = typesPossible.get(ri);
        return et;
    }
    private EnemyType RandomEnemyType(int givenLevel) {
        EList<EnemyType> typesPossible = new EList<EnemyType>();
        for (int i = 0; i < EnemyType.values().length; ++i) {
            if (EnemyType.values()[i].level == givenLevel)
                typesPossible.add(EnemyType.values()[i]);
        }
        if (typesPossible.size() == 0)
            return null;
        /// Randomly choose type.
        int ri = r.nextInt(typesPossible.size());
        EnemyType et = typesPossible.get(ri);
        return et;
    }

    /*    public void Log(String s, LogType type) {
        // Add to log to present/update to ui?
        Printer.out("Encounter log: "+s);
        Log l = new Log(s, type);
        log.add(l);
    }
  */
    public void LogEnc(Log logMsg) {
        // Add to log to present/update to ui?
//        Printer.out("Encounter log: "+logMsg);
        log.add(logMsg); // Add to the encounter log.
        for (int i = 0; i < combatants.size(); ++i){ // But also add to the players' specific own logs straight away!
            Combatable c = combatants.get(i);
            if (c instanceof Player){
                Player p = (Player) c;
                p.log.add(logMsg);
            }
        }
    }
    /*
    public void AbandonedShelter() {
        /*
        Printer.out("Abandoned shelters decrease from "+player.Get(Stat.ABANDONED_SHELTER));
        player.Adjust(Stat.ABANDONED_SHELTER, -1);;
        Log("Found some food", LogType.INFO);
        player.Adjust(Stat.FOOD, 1);
        // TODO: Add more descriptions and the random results.
    }
    public void RandomPlayerShelter() {
        player.Adjust(Stat.RANDOM_PLAYERS_SHELTERS, -1); // Decrease the events to spawn...
        player.Adjust(Stat.RandomPlayerFound, 1); // Increase this. Will be sent to server to see who you found.
        Log("Found some food", LogType.INFO);
        player.Adjust(Stat.FOOD, 1);
        // TODO: Perhaps add more actions later.
    }
*/
    public void AddCombatant(Combatable combatant) {
        if (combatant.isAttacker) { // Add it?
            if (!attackers.contains(combatant)) {
                attackers.add(combatant);
            }
        }
        else if (!defenders.contains(combatant))
            defenders.add(combatant);
        if (!combatants.contains(combatant)) {
            combatants.add(combatant);
        }
    }
    /// Adds a player as a defender in this attack.
    public void AddDefenders(Player player) {
        if (!defenders.contains(player))
            defenders.add(player);
        if (!combatants.contains(player))
            combatants.add(player);
    }

    public float GetTotal(Stat stat) {
        float total = 0;
        for (int i = 0; i < GetInvolvedPlayers().size(); ++i){
            total += GetInvolvedPlayers().get(i).Get(stat);
        }
        return total;
    }
}
