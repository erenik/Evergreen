package erenik.evergreen.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import erenik.evergreen.Game;
import erenik.evergreen.Simulator;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Invention.InventionStat;
import erenik.evergreen.common.Invention.InventionType;
import erenik.evergreen.common.Invention.WeaponType;
//import erenik.evergreen.android.App;
import erenik.evergreen.common.combat.Combatable;
import erenik.evergreen.common.encounter.Encounter;
import erenik.evergreen.common.event.Event;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogListener;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.player.*;
import erenik.evergreen.util.Dice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Emil on 2016-10-25.
 */
public class Player extends Combatable implements Serializable {
    List<PlayerListener> listeners = null;
    List<LogListener> logListeners = null;
    List<Encounter> encountersToProcess = null;

    /// Creates stuff not added automatically - after creation, or after loading from file via e.g. readObject.
    void Init() {
        encountersToProcess = new ArrayList<>();
        statArr = new float[Stat.values().length];
        transportProbabilityArr = new float[Transport.values().length]; // Only variable we care about for transports as far as configurability is concerned.
        listeners = new ArrayList<>();
        logListeners = new ArrayList<>();
        transports = new ArrayList<>();
        skills = new ArrayList<Skill>(Arrays.asList(Skill.values()));
        dailyActions = new ArrayList<>();
        skillTrainingQueue = new ArrayList<>();
    }

    // Adds a listener for state changes to the player.
    public void addStateListener(PlayerListener listener){ listeners.add(listener);};
    /// Local simulator for proceeding to the next day. Isn't used for the multiplayer games.
    Simulator simulator = Simulator.getSingleton();
    /// The current game you are playing in.
//    Game game = null; // Re-point it, or create locally as needed.

    public long lastSaveTimeSystemMs = 0;
    public long lastEditSystemMs = 0;

    static Random r = new Random(System.nanoTime());
    // Main stats.
//    float hp, food, materials, base_attack, base_defense, emissions;
    float[] statArr = null;
    float[] transportProbabilityArr = null; // Only variable we care about for transports as far as configurability is concerned.
    /**
     * List of actions this player will take during the day/turn.
     * List of names of actions, optionally with extra arguments after a colon?
     *
    */
    public List<String> dailyActions = null;
    public int activeAction = -1;
    private List<Transport> transports = null;
    public boolean playEvents = false;
    /// Increment every passing day. Stop once dying.
    public int TurnsSurvived() {
        return (int) Math.round(Get(Stat.TurnSurvived));
    };
    /// Temporary/chaning starving modifier, changes each turn. Default 1, lower when starving.
    float t_starvingModifier = 1;
    float t_materialModifier = 1; // Same as starving, lowers when negative (debt) on action requirements of materials.

    /// Currently equipped weapon. Null if not equiped. Pointer to weapon in inventory if equipped.
    List<Integer> equippedIndices = new ArrayList<>(); // Indices of which inventions are currently equipped.

    // Serialization version.
    public static final long serialVersionUID = 1L;
    
    Invention GetEquipped(InventionType queryType)
    {
        List<Invention> equipped = GetEquippedInventions();
        for (int i = 0; i < equipped.size(); ++i) {
            Invention inv = equipped.get(i);
            if (inv.type == queryType)
                return inv;
        }
        return  null;
    }
    public Invention GetEquippedWeapon() {
        return GetEquipped(InventionType.Weapon);
    }
    public Invention GetEquippedArmor() {
        return GetEquipped(InventionType.Armor);
    }

    /// List of events to evaluate/process/play-mini-games. Old events are removed from the list.
    List<Event> events = new ArrayList<Event>();
    /// Log of messages for this specific player.
    public List<Log> log = new ArrayList<Log>();
    /// Array of exp in each Skill.
    List<Skill> skills =  null;
    /// Queued skills to be leveled up.
    public List<String> skillTrainingQueue = new ArrayList<>();
    public List<Invention> inventions = new ArrayList<>(); // Blueprints, 1 of each.
    public List<Invention> inventory = new ArrayList<>(); // Inventory, may have duplicates of items that can be traded etc.
    /// To increase bonuses/chance of invention if failing a lot in series.
    int successiveInventingAttempts = 0;
    int successiveCraftingAttempts = 0;
    /// Total crafting progress into current crafting invention.
    int craftingProgress = 0;
    Invention inventionCurrentlyBeingCrafted = null; // For inventions requiring multiple turns to craft.
    /** Game that this player belongs to.
     *  0 - Local game. Backed up on server for practical purposes.
     *  1 - Global game. All players can interact with you.
     *  2 - Local Multiplayer game. Enter IP or other details to create ad-hoc connection.
     *  3-99 - Reserved.
     *  100-2000. Public game IDs. These games may have a password to join them.
    */
    public int gameID = -1;

    public List<String> knownStrongholds = new ArrayList<>();
    public ArrayList<String> knownPlayerNames = new ArrayList<>();

    // Auto-created. Start-using whenever.
    public List<LogType> logTypesToShow = new ArrayList<LogType>(Arrays.asList(LogType.values()));
    public boolean isAI = false;
    /// Used for clients/single-simulator for self.
    public Player() {
        Init();
        SetName("Parlais Haux Le'deur");
        SetDefaultStats();
    }
    // Delivers a String based on using ObjectOutputStream, then saving the bytes.
    public byte[] toByteArr() {
        try { 
            ObjectOutputStream oos = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            writeObject(oos);
            oos.close();
            return baos.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        } 
        return null;
    }
    public boolean fromByteArr(byte[] bytes) {
        try { 
            ObjectInputStream ois = null;
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            readObject(ois);
            ois.close();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) { 
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;        
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(gameID);
        out.writeObject(name);
        System.out.println("name: "+name);
        out.writeObject(password);
        out.writeObject(statArr);
        out.writeObject(transportProbabilityArr);
        for (int i = 0; i < transportProbabilityArr.length; ++i) {
            System.out.println("transp: "+i+": "+transportProbabilityArr[i]);
        }
        out.writeObject(skills);
        out.writeObject(dailyActions);
        out.writeObject(equippedIndices);
        out.writeObject(log);
        out.writeObject(skillTrainingQueue);
        out.writeObject(inventions);
        out.writeObject(inventory);
        out.writeObject(inventionCurrentlyBeingCrafted);
        out.writeObject(knownStrongholds);
        out.writeObject(knownPlayerNames);
        out.writeObject(logTypesToShow);
        out.writeBoolean(isAI);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
        Init();
        SetDefaultStats();
        // Actually load here
        gameID = in.readInt();
        name = (String) in.readObject();
        System.out.println("name: "+name);
        password = (String) in.readObject();
        statArr = (float[]) in.readObject();
        transportProbabilityArr = (float[]) in.readObject();
        DefaultTransports();
        for (int i = 0; i < transportProbabilityArr.length; ++i) {
            System.out.println("transp: "+i+": "+transportProbabilityArr[i]);
            transports.get(i).Set(TransportStat.RandomProbability, transportProbabilityArr[i]);
        }
//        System.out.println("name: "+name+" pw: "+password);
        skills = (List<Skill>) in.readObject();
        dailyActions = (List<String>) in.readObject();
        equippedIndices = (List<Integer>) in.readObject();
        log = (List<Log>) in.readObject();
        skillTrainingQueue = (List<String>) in.readObject();
        inventions = (List<Invention>) in.readObject();
        inventory = (List<Invention>) in.readObject();
        inventionCurrentlyBeingCrafted = (Invention) in.readObject();
        knownStrongholds = (List<String>) in.readObject();
        knownPlayerNames = (ArrayList<String>) in.readObject();
        logTypesToShow = (List<LogType>) in.readObject();
        isAI = in.readBoolean();
        System.out.println("Init from readOBject!");

    }
    private void readObjectNoData() throws ObjectStreamException
    {

    }

    public int MaxHP() {
        int maxHP = GetInt(Stat.MAX_HP);
        for (int i = 0; i < Get(Skill.Survival).Level(); ++i)
            maxHP += i + 1; // 1/3/6/10/15,  +1,2,3,4,5 increases
        return  maxHP;
    }
    public boolean IsAlive()
    {
        return GetInt(Stat.HP) > 0;
    }
    int Speed()
    {
        return GetInt(Stat.SPEED);
    }
    public int UnarmedCombatBonus() {
        return GetEquippedWeapon() == null? Get(Skill.UnarmedCombat).Level() : 0;
    }
    public List<Invention> GetEquippedInventions() {
        List<Invention> equipped = new ArrayList<>();
        for (int i = 0; i < inventory.size(); ++i) {
            Invention item = inventory.get(i);
            if (item.Get(InventionStat.Equipped) >= 0) {
                equipped.add(item);
            }
        }
        return equipped;
    }
    /// Fetches total form all equipped gear.
    public int GetEquipped(InventionStat stat) {
        List<Invention> equipped = GetEquippedInventions();
        int tot = 0;
        for (int i = 0; i < equipped.size(); ++i)
            tot += equipped.get(i) != null? equipped.get(i).Get(stat) : 0;
        return tot;
    }
    public int BaseAttack() {
        int att = GetInt(Stat.BASE_ATTACK) + GetInt(Stat.ATTACK_BONUS);
        att += UnarmedCombatBonus(); // Max +9 Attack
        att += GetEquipped(InventionStat.AttackBonus);
        att += GetEquippedWeapon() != null ?  (Get(Skill.WeaponizedCombat).Level() + 1) / 2 : 0;
        return att;
    }
    public int OnTransportAttack() {
        return (int) (BaseAttack() + CurrentTransport().Get(TransportStat.SocialSupport));
    }
    /// Attack when in shelter? + shelter bonus, yo? or? D:
    public int ShelterAttack() {
        return BaseAttack() + GetInt(Stat.SHELTER_DEFENSE);
    }
    public int ShelterDefense() {
        return BaseDefense() + GetInt(Stat.SHELTER_DEFENSE);
    }
    public int OnTransportDefense() {
        return (int) (BaseDefense() + CurrentTransport().Get(TransportStat.SocialSupport));
    }
    public int BaseDefense() {
        int def = GetInt(Stat.BASE_DEFENSE) + GetInt(Stat.DEFENSE_BONUS);
        def += (UnarmedCombatBonus()-1) / 3;
        def += GetEquipped(InventionStat.DefenseBonus);
        def += Get(Skill.DefensiveTraining).Level();
        return def;
    }
    public Dice Damage()
    {
        // Base damage, 1D6, no bonuses.
        Dice damage = new Dice(6, 1, 0);
        damage.bonus += UnarmedCombatBonus()/2; // Max +4 damage
        Invention weapon = GetEquippedWeapon();
        if (weapon != null) { // Weapon equipped.
            damage.diceType = weapon.Get(InventionStat.AttackDamageDiceType);
            damage.dice = weapon.Get(InventionStat.AttackDamageDice);
            damage.bonus = weapon.Get(InventionStat.AttackDamageBonus);
            damage.bonus += Get(Skill.WeaponizedCombat).Level() / 2;
            System.out.println("Damage: "+damage.dice+"D"+damage.diceType+"+"+damage.bonus);
        }
        return damage;
    }
    public void SetDefaultStats()
    {
        isPlayer = true;
        // Default stats?
        for (int i = 0; i < Stat.values().length; ++i)
            statArr[i] = Stat.values()[i].defaultValue;
        for (int i = 0; i < Transport.values().length; ++i)
            transportProbabilityArr[i] = Transport.values()[i].defaultProbability;
        for (int i = 0; i < skills.size(); ++i) // Reset EXP in each skill?
            skills.get(i).setTotalEXP(0);
        if (inventions != null)
            inventions.clear();
        DefaultTransports();
        // Clear both queues.
        dailyActions.clear();
        skillTrainingQueue.clear();
    }
    void DefaultTransports() {
        // Add default transports.
        if (transports != null)
            transports.clear();
        transports = new ArrayList<>();
        for (int i = 0; i < Transport.values().length; ++i) {
            Transport t = Transport.values()[i];
            t.SetDefaults();
            transports.add(t);
        }
    }
    
    public void Adjust(Stat s, float amount)
    {
        statArr[s.ordinal()] += amount;
        if (s == Stat.HP && statArr[s.ordinal()] <= 0) {
            System.out.println("Died, informing listeners");
            for (int i = 0; i < listeners.size(); ++i)
                listeners.get(i).OnPlayerDied(this);
        }
    }
    // Getter for main stats.
    float Get(int stat)
    {
        return statArr[stat];
    }
    public float Get(Stat s)
    {
        return statArr[s.ordinal()];
    }
    public int GetInt(Stat s)
    {
        return (int) statArr[s.ordinal()];
    }
    void SetInt(Stat s, int am)
    {
        statArr[s.ordinal()] = am;
    }
    public void Set(Stat stat, float value)
    {
        statArr[stat.ordinal()] = value;
    }
    /// Saves to local "preferences"
    public void Log(Log l) {
        log.add(l);
        for (int i = 0; i < logListeners.size(); ++i) {
            LogListener ll = logListeners.get(i);
            ll.OnLog(l, this);
        }
    }
    public void Log(String text, LogType t) {
        log.add(new Log(text, t));
        for (int i = 0; i < logListeners.size(); ++i) {
            LogListener ll = logListeners.get(i);
            ll.OnLog(new Log(text, t), this);
        }
    }

    public Transport CurrentTransport() {
        for (int i = 0; i < transports.size(); ++i) {
            Transport t = transports.get(i);
            if (Get(Stat.CurrentTransport) == t.ordinal())
                return t;
        }
        return null;
    }
    /// Stuff to process at the start of every day, also NewGame.
    void NewDay() {
        // New day, assign transport?
        encountersToProcess.clear();
        Adjust(Stat.TurnPlayed, 1);
        Adjust(Stat.TurnSurvived, 1);
        System.out.println("Turn: " + Get(Stat.TurnPlayed));
    }
    /// Adjusts stats, generates events based on chosen actions to be played, logged
    public void NextDay(Game game) {
        if (GetInt(Stat.HP) <= 0) {
            // TODO: Add a listener-callback mechanism for when the player dies.
            //            App.GameOver();
            return;
        }
        NewDay();  // New day :3
        Log("-- Day " + (int) Get(Stat.TurnPlayed) + " --", LogType.INFO);
        Transport t = Transport.RandomOf(transports);
       // System.out.println("Randomed transport.. "+t.name());
        Set(Stat.CurrentTransport, t.ordinal());
        Log("Transport of the day is: "+CurrentTransport().name(), LogType.INFO);
        float emissionsToGenerate = CurrentTransport().Get(TransportStat.EmissionsPerDay);
        if (emissionsToGenerate > 0)
            Log("Generated "+Stringify(emissionsToGenerate)+" units of emissions.", LogType.INFO);
        Adjust(Stat.EMISSIONS, emissionsToGenerate);
        // Yeah.
        Adjust(Stat.FOOD, -2);
        if (GetInt(Stat.FOOD) >= 0) {
            Adjust(Stat.HP, 1);
            ClampHP();
        }
        else {
            float loss = Get(Stat.FOOD) / 5;
            Adjust(Stat.HP, loss);
            Log("Starving, lost "+(-loss)+" HP.", LogType.OtherDamage);
            if (!IsAlive())
            {
                Log("Died of starvation", LogType.OtherDamage);
                return;
            }
        }
        /// Gain EXP? Spend unspent EXP?
        int expToGain = (int) (2 + Get(Skill.Studious).Level() + Get(Stat.UNALLOCATED_EXP));
        // Erase unallocated exp. It will be re-added later perhaps.
        Set(Stat.UNALLOCATED_EXP, 0);
 //       System.out.println("Exp gained: "+expToGain);
        GainEXP(expToGain);

        // Analyze some chosen hours of activity. Generate events and stuff for each?
        EvaluateActions(game);
        /// Force the player to play through the generated events before they proceed.
//        HandleGeneratedEvents();

        // Attacks of the evergreen?
        int everGreenTurn = (int) (TurnsSurvived()) % 16;
        switch(everGreenTurn) {
            default: break;
            case 0: case 6:case 10:case 13: case 15: // The pattern repeats every 16 turns.
                encountersToProcess.add(new Encounter(Finding.AttacksOfTheEvergreen, this));
                break;
        }
        /// Process encounters that were stored up.
        for (int i = 0; i < encountersToProcess.size(); ++i){
            Encounter enc = encountersToProcess.get(i);
            enc.Simulate();
        }

        for (int i = 0; i < listeners.size(); ++i) // Inform listeners, new day is over.
            listeners.get(i).OnPlayerNewDay(this);
    }

    public void ClampHP()
    {
        float hp = Get(Stat.HP);
        if (hp > MaxHP())
            hp = MaxHP();
        else if (hp < 0)
            hp = 0;
        Set(Stat.HP, hp);
    }

    String Stringify(float value)
    {
        // Stringifies target value, based on some assumptions.
        int iValue = Math.round(value);
        float remainder = value - iValue;
        if (Math.abs(remainder) < 0.05f)
            return ""+iValue;
        return String.format("%.1f", value).replace(',', '.');
    }
    float hoursPerAction  = 1.f;
    float foodHarvested = 0.0f;
    DAction da;
    void EvaluateActions(Game game)
    {
        t_starvingModifier = GetInt(Stat.HP) >= 0? 1.0f : 1 / (1 + Math.abs(Get(Stat.HP)) * 0.5f);
        // Have this increase with some skill?
        float hoursSimulated = 6.f;
        int div = dailyActions.size();
        final int MAX_ACTIONS = 8;
        switch(div) // Add waste time if many actions are scheduled.
        {
            case MAX_ACTIONS: div += 4.f; break;
            case 7: div += 2.f; break;
            case 6: div += 1.f; break;
            case 5: div += 0.5f; break;
        }
        if (div > MAX_ACTIONS)
            div += 6.f;
        if (div > MAX_ACTIONS)
            Log("Having scheduled too much to do during the day, you manage to lose a lot of time between the actions you had intended to do. You are even forced to cancel entirely some of the actions you wanted to do.", LogType.PROBLEM_NOTIFICATION);
        else if (dailyActions.size() > 4)
            Log("During the day, you lose some time while changing tasks. Choose 4 or fewer actions per day for full efficiency.", LogType.PROBLEM_NOTIFICATION);
        hoursPerAction = hoursSimulated / div;
//        System.out.println("hoursPerAction: "+hoursPerAction);
        foodHarvested = 0.0f;
        // Execute at most 8 actions per day, regardless of queue.
        for (int i = 0; i < dailyActions.size() && i < MAX_ACTIONS; ++i)
        {
            /// Parse actions and execute them.
            da = DAction.ParseFrom(dailyActions.get(i));
            EvaluateAction(da, game);
        }
    }
    void EvaluateAction(DAction da, Game game)
    {
        float units = 1;
        switch (da)
        {
            case GatherFood:
                units = Dice.RollD3(2);  // r.nextInt(5) + 2;
                units += Get(Skill.Foraging).Level();
                units += CurrentTransport().Get(TransportStat.ForagingBonus);
                if (units < 1) units = 1;
                units *= t_starvingModifier;
                units *= hoursPerAction;
                Adjust(Stat.FOOD, units);
                Log(da.text + ": Found " + Stringify(units) + " units of food.", LogType.INFO);
                break;
            case GatherMaterials:
                units = Dice.RollD3(2);
                units += CurrentTransport().Get(TransportStat.MaterialGatheringBonus);
                units *= t_starvingModifier;
                units *= hoursPerAction;
                Log(da.text+": Found " + Stringify(units) + " units of materials.", LogType.INFO);
                Adjust(Stat.MATERIALS, units);
                break;
            case Scout: Scout(); break;
            case Recover:
                units = 0.5f * hoursPerAction * (1 + 0.5f * Get(Skill.Survival).Level()); // Recovery +50/100/150/200/250%
                units *= t_starvingModifier;
                Adjust(Stat.HP, units);
                ClampHP();
                Log(da.text + ": Recovered " + Stringify(units) + " HP.", LogType.INFO);
                break;
            case BuildDefenses: BuildDefenses(); break;
//            case AugmentTransport: AugmentTransport(); break;
            case LookForPlayer: LookForPlayer(da, game); break;
//            case Expedition: Log("Not implemented yet - Expedition", LogType.ATTACKED); break;
            case Invent: Invent(da); break;
            case Craft: Craft(da); break;
            case Steal: Steal(da, game); break;
            case AttackAPlayer: AttackAPlayer(da, game);
                break;
            case Study:
                // Gain exp into current skill perhaps..?
                int toGain = Dice.RollD3(2) + Get(Skill.Studious).Level();
                Log("Studies grant you "+toGain+" experience points.", LogType.PROGRESS);
                GainEXP(toGain);
                break;
            case ReduceEmissions: ReduceEmissions(); break;
            default:
                System.out.println("Uncoded daily action");
                System.exit(15);
        }
    }

    private void ReduceEmissions() {
        int successful = 0;
        for (int i = 0; i < hoursPerAction; ++i) {
            if (r.nextInt(100) > 50)
                ++successful;
        }
        if (successful > hoursPerAction * 0.75)
            Log(new Log(LogTextID.reduceEmissionsSuccessful, LogType.SUCCESS, successful+""));
        else if (successful > hoursPerAction * 0.5)
            Log(new Log(LogTextID.reduceEmissionsMostlySuccessful, LogType.SUCCESS, successful+""));
        else if (successful > hoursPerAction * 0.25)
            Log(new Log(LogTextID.reduceEmissionsNotSoSuccessful, LogType.SUCCESS, successful+""));
        else
            Log(new Log(LogTextID.reduceEmissionsFailed, LogType.SUCCESS, successful+""));
        Adjust(Stat.EMISSIONS, -successful);
        if (Get(Stat.EMISSIONS) < 0) {
            Set(Stat.EMISSIONS, 0);
        }
    }

    private void AttackAPlayer(DAction da, Game game) {
        // Check if a player was provided?
        String targetName = da.requiredArguments.get(0).value;
        Player p = game.GetPlayer(targetName);
        if (p == null) {
            Log("Unable to find player by name "+targetName, LogType.ACTION_FAILURE);
            return;
        }
        Log("You attack "+targetName+".", LogType.ATTACK);
        p.Log("You are attacked by "+name+"!", LogType.ATTACKED);
        // Create a custom encounter for this.
        Encounter enc = new Encounter(this, p);
        enc.Simulate(); // Simulate it.

    }

    private void AugmentTransport() {
        // Arg 1 is Transport, Arg 2 is Transport Augment (an invention).
        String name = da.requiredArguments.get(0).value;
        Log(da.text+": Tinkering on the "+name+".", LogType.INFO);
        Invention invention = null;
        String inventionName = da.requiredArguments.get(1).value;
        for (int i = 0; i < inventions.size(); ++i) {
            Invention inv = inventions.get(i);
            if (inv.name.equals(inventionName))
                invention = inv;
        }
        if (invention == null) {
            Log("Could find no invention matching "+inventionName, LogType.PROBLEM_NOTIFICATION);
            return;
        }
        Log("TODO: Actual transport augmentation.", LogType.INFO);
    }
    private void Steal(DAction da, Game game)
    {
        String targetPlayerName = da.requiredArguments.get(0).value;
        Player p = game.GetPlayer(targetPlayerName);
        if (p == null)
        {
            Log("No player found with that name.", LogType.ATTACK_MISS);
            return;
        }
        /*
        Spy/Steal: attempts to get information or resources from target enemy stronghold, NPC or player. Roll 2D6. GS: Roll another D6. BS: Roll 1 less D6.
            18+, steal 6D3 food or 6D3 Materials, and 4 items or 2 inventions.
            16~17, steal 5D3 food or 5D3 Materials, and 3 items or 1 invention.
            14~15, steal 4D3 food or 4D3 Materials, and 2 items.
            12~13, steal 3D3 food or 3D3 Materials, and 1 item.
            10~11, steal 2D3 Food or 2D3 Materials.
            9, steal 1D3,
            7~8, failed, but not detected
            2~6, failed & detected, player traps may activate, player may use ranged attacks on the spy, or stronghold defenders appear (1D6 severity, random type).
        */
        int roll = Dice.RollD6(2 + Get(Skill.Thief).Level());
        if (roll < 7) { // Failed and detected.
            Log("While trying to steal from "+targetPlayerName+" you were detected! You didn't manage to steal anything.", LogType.ATTACK_MISS);
            p.Log(""+name+" tried to steal from you, but failed as he was detected!", LogType.ATTACKED);
            return;
        }
        if (roll < 9) {
            Log("While trying to steal from "+p.name+", you mistakenly caused attention, forcing you to retreat.", LogType.ATTACK_MISS);
            p.Log("While walking in your shelter, you notice something grabbing your attention. You try to see what it was, but find nothing is amiss.", LogType.ATTACKED_MISS);
            return;
        }
        int d3Stolen = (roll - 11)/2 + 2; // Food or materials.
        boolean foodNotMats = r.nextBoolean();
        Stat stolenStat = foodNotMats? Stat.FOOD : Stat.MATERIALS;
        float quantity = Dice.RollD6(d3Stolen) * 0.5f;
        if (quantity > p.Get(stolenStat))
            quantity = (float) p.Get(stolenStat);
        // Steal food for now?
        this.Adjust(stolenStat, quantity);
        p.Adjust(stolenStat, -quantity);
        // Calc success?
        String whatStolen = quantity+" units of "+stolenStat.name();
        Log("Stole "+whatStolen+" from "+p.name+"!", LogType.ATTACK);
        p.Log("Player "+name+" stole "+whatStolen+" from you!", LogType.ATTACKED);
    }

    void LookForPlayer(DAction da, Game game)
    {
        // Determine chances.
        // Search query.
        // Found?
        String name = this.da.requiredArguments.get(1).value;
        Player player = null;
        switch (this.da.requiredArguments.get(0).value)
        {
            default:
            case "Exactly": player = game.GetPlayer(name); break;
            case "Starts with":
            case "StartsWith":
                player = game.GetPlayer(name, false, true);
                break;
            case "Contains":
                player = game.GetPlayer(name, true, false);
                break;
        }
        if (player == null) {
            int randInt = r.nextInt(100);
            Log("Despite searching, you were unable to find a player called "+name+".", LogType.ACTION_FAILURE);
            // Add chance to find random other players?
            if (randInt < 50) {
                Player randomPlayer = game.RandomPlayer(KnownNamesSelfIncluded());
                if (randomPlayer == null)
                    return;
                String newPlayer = randomPlayer.name;
                if (newPlayer != null && newPlayer != name) {
                    Log("However, while searching, you stumbled upon another player!", LogType.SUCCESS);
                    name = newPlayer;
//                    knownPlayerNames.add(newPlayer);
                }
                else // In-case you already know all the players in the game already.
                    return;
            }
            else
                return;
        }
        Log("You found the player named "+name+"! You can now interact with that player.", LogType.SUCCESS);
        knownPlayerNames.add(name);
        Log("knownNAmes: "+knownPlayerNames, LogType.INFO);
    }

    private ArrayList<String> KnownNamesSelfIncluded() {
        ArrayList<String> knownNames = new ArrayList<>();
        for (int i = 0; i < knownPlayerNames.size(); ++i)
            knownNames.add(knownPlayerNames.get(i));
        knownNames.add(name);
        return knownNames;
    }

    private void Craft(DAction da)
    {
        float emit = ConsumeMaterials(hoursPerAction * 0.5f);
        // How many times to random.
        float toRandom = 0.5f + hoursPerAction; // Roll once for each hour?
        toRandom *= t_starvingModifier;
        toRandom *= CalcMaterialModifier();
        String s = da.text+": ";
        // Check if inveting has been queued for any special item?
        if (da.requiredArguments.size() == 0) {
            System.out.println("required argument in Player.Craft");
            System.exit(14);
        }
        String whatToCraft = da.requiredArguments.get(0).value;
        whatToCraft = whatToCraft.trim();
        Invention toCraft = null;
        for (int i = 0; i < inventions.size(); ++i)
        {
            Invention inv = inventions.get(i);
            System.out.println("Invention names: "+inv.name+", toCraft: "+whatToCraft);
            if (inv.name.equals(whatToCraft))
                toCraft = inv;
        }
        if (toCraft == null) {
            Log("toCraft null, what did you wanna craft again?", LogType.Error);
            return;
        }
        int progressRequired = toCraft.Get(InventionStat.ProgressRequiredToCraft);
        float progress = 0.0f;
        boolean craftedSomething = false;
        System.out.println("toRandom iterations: "+toRandom);
        successiveCraftingAttempts = 0;
        float progressGained = 0;
        for (int i = 0; i < toRandom; ++i) // Times to random.
        {
            float relativeChance = toRandom > 1.0 ? 1 : toRandom;
            relativeChance += successiveCraftingAttempts * 0.05f;
            progress += Dice.RollD3(1) * relativeChance;
            ++successiveInventingAttempts;
            toRandom -= 1;
        }
        progressGained += progress;
        /// Success.
        if (progress > progressRequired)
        {
            // Crafted!
            Invention newWeapon = new Invention(toCraft);
            float ratioOverProgressed = (progress - progressRequired) / progressRequired;
            System.out.println("ratioOverProgressed: "+ratioOverProgressed);
            Random rCrafting = new Random(System.nanoTime());
            int levelAdjustment = 0;
            while(ratioOverProgressed > 0)
            {
                float randF = rCrafting.nextFloat();
                if (randF < ratioOverProgressed)
                {
                    System.out.println("level increased +1");
                }
                ratioOverProgressed -= randF;
            }
            newWeapon.Set(InventionStat.QualityLevel, newWeapon.Get(InventionStat.QualityLevel) + levelAdjustment);
            // Update quality level.
            newWeapon.UpdateWeaponStats();
            newWeapon.UpdateWeaponAdditionalEffect();
            inventory.add(newWeapon);
            Log("Crafting complete: "+newWeapon.name, LogType.INFO);
        }
        else
        {
            Log("Crafting progressed by "+progressGained+" units.", LogType.INFO);
            // Store as unfinished business?
        }
    }

    private void Invent(DAction inventAction)
    {
        float emit = ConsumeMaterials(hoursPerAction * 0.5f);
        // How many times to random.
        float toRandom = 0.5f + hoursPerAction; // Roll once for each hour?
        toRandom *= t_starvingModifier;
        toRandom *= CalcMaterialModifier();
        String s = da.text+": ";
        // Check if inveting has been queued for any special item?
        boolean inventedSomething = false;
        System.out.println("toRandom iterations: "+toRandom);
        for (int i = 0; i < toRandom; ++i) // Times to random.
        {
            float relativeChance = toRandom > 1.0 ? 1 : toRandom;
            InventionType type = null;
//            System.out.println("Args? "+da.requiredArguments.size());
            if (da.requiredArguments.size() > 0)
            {
                String typeStr = da.requiredArguments.get(0).value;
  //              System.out.println("Typestr: "+typeStr);
                type = InventionType.GetFromString(typeStr);
            }
            if (type == null) {
                Log("Bad invention type", LogType.Error);
                return;
            }
            if (type == InventionType.Any)
            {
                type = InventionType.RandomType();
                relativeChance += 0.05f; // + 5% chance of inventing if random?
                System.out.println("Type: "+type.name());
            }
            Invention inv = AttemptInvent(type, relativeChance);
            // Craft it on success?
            if (inv != null){
                successiveInventingAttempts = 0;
                inventedSomething = true;
                // Add it to inventory too.
                inventory.add(new Invention(inv));
                // Don't auto-equip... Present dialog for it.
             //   Equip(inv);
            }
            ++successiveInventingAttempts;
            toRandom -= 1;
        }
        if (inventedSomething == false)
            Log(s+"Failed to invent anything new.", LogType.INFO);
    }
    /// Tries to equip target invention.
    public void Equip(Invention inv) {
        Invention currentlyEquipped = GetEquipped(inv.type);
        if (currentlyEquipped != null)
            currentlyEquipped.Set(InventionStat.Equipped, -1); // Set as not equipped.
        inv.Set(InventionStat.Equipped, (int) Get(Stat.ID));
    }

    public float getInventionProgress(InventionType type, int subType)
    {
        float progress = 0;
        for (int i = 0; i < inventions.size(); ++i)
        {
            Invention inv = inventions.get(i);
            if (inv.type != type)
                continue;
            if (subType >= 0 && inv.Get(InventionStat.SubType) != subType)
                continue;
            progress +=  Math.pow(inv.Get(InventionStat.QualityLevel) + 1, 2) * inv.Get(InventionStat.TimesInvented); // Gives bonus for higher quality items, 1, 4, 9, 16, 25 for levels 1-5 respectively.
        }
        return progress;
    }
    Invention AttemptInvent(InventionType type, float relativeChance)
    {
        Invention inv = new Invention(type);
        int subType = inv.RandomizeSubType();
        // 1.0 base, + 0.1 for each level in Inventing, +0.25 of the invention progress in the same category/main invention type and 1x subtype progress.
        float bonusFromInventingBefore = 0.001f * getInventionProgress(type, -1) + 0.025f * getInventionProgress(type, subType);
        float inventingLevel = Get(Skill.Inventing).Level();
        float rMax = (1.f + 0.05f * inventingLevel + bonusFromInventingBefore) * relativeChance;
        float random = r.nextFloat() * rMax; // Random 0 to 1 + 0.1 max for each Inventing level, * relChance
        float successThreshold = 0.75f - successiveInventingAttempts * 0.03f - inventingLevel * 0.02f; // Increase success possibility with higher inventing skill, and for each attempt in succession.
        System.out.println("randomed: "+random+" out of 0-"+rMax+", successThreshold: "+successThreshold+" bonusFromBefore: "+bonusFromInventingBefore);
        if (random < successThreshold)
            return null;            // No success.
        // Determine high-quality ratio.
        int levelSuccess = (int) ((random - 0.8f) / 0.2f); // HQ1@ 1, HQ2@ 1.2, HQ3@ 1.4, HQ4@ 1.6, HQ5@ 1.8, etc.
        if (levelSuccess < 0)
            levelSuccess = 0;
        inv.Set(InventionStat.QualityLevel, levelSuccess);
        inv.RandomizeDetails();
        System.out.println("Level success: " + levelSuccess+" item name: "+inv.name);
        for (int i = 0; i < inventions.size(); ++i) {
            Invention i2 = inventions.get(i);
            if (i2.name.equals(inv.name)) {
                // Save type of invention? Add progress to the invention we already had?
                i2.Adjust(InventionStat.TimesInvented, 1);
                Log("While trying to invent something new, your thoughts go back to the old version of the "+inv.name+", perhaps you will be luckier next time.", LogType.INFO);
                return null;
            }
        }
        inventions.add(inv);
        Log("Invented a new " + inv.type.text() + ": " + inv.name, LogType.SUCCESS);
        return inv;
    }

    void Scout()
    {
        int speed = Speed();
        speed += CurrentTransport().Get(TransportStat.SpeedBonus);
        // Randomize something each hour? Many things with speed?
        float speedBonus = (float) Math.pow(speed, 0.5f);
        System.out.println("Speed: "+speed+" bonus(pow-modified): "+speedBonus);
        float toRandom = hoursPerAction * speedBonus;
        toRandom *= t_starvingModifier;
        Map<Finding, Integer> chances = new HashMap<Finding, Integer>();
        // Increase liklihood of encounters for each passing turn when scouting -> Scout early on is safer.
        // There will however be more of other resources available later on. :D
        int turn = (int) Get(Stat.TurnSurvived);
        chances.put(Finding.RandomEncounter, 5 + turn); // Increase chance of combat for each survived turn.
        chances.put(Finding.Nothing, 50);
        chances.put(Finding.Food, 15);
        chances.put(Finding.Materials, 15);
        chances.put(Finding.AbandonedShelter, 10 + turn);
        chances.put(Finding.RandomPlayerShelter, 10);
        chances.put(Finding.EnemyStronghold, 1+turn/3);
        int sumChances = 0;
        for (int i = 0; i < chances.size(); ++i) {
            sumChances += (Integer) chances.values().toArray()[i];
        }
//        System.out.println("Sum chances: "+sumChances);
        List<Finding> foundList = new ArrayList<Finding>();
        String foundStr = "Found: ";
        while(toRandom > 0) {
            toRandom -= 1;
            float chance = r.nextFloat() * sumChances;
            for (int i = 0; i < chances.size(); ++i) {
                int step = (Integer) chances.values().toArray()[i];
                chance -= step;
                if (chance < 0){ // Found it
                    foundStr += chances.keySet().toArray()[i]+", ";
                    foundList.add((Finding)chances.keySet().toArray()[i]);
                }
            }
        }

        System.out.println(foundStr);
        float amount = 0;
        int numFoodStashes = 0, numMatStashes = 0,  numEncounters = 0, numAbShelters = 0, numRPS = 0, numEnStrong = 0;
        float foodFound = 0, matFound = 0;
        String s = da.text+": While scouting the area, you ";
        for (int i = 0; i < foundList.size(); ++i)
        {
            Finding f = foundList.get(i);
            switch(f)            // Evaluate it.
            {
                case Nothing: break;
                case RandomEncounter: numEncounters  += 1; break;
                case Food: numFoodStashes += 1; foodFound += 1 + r.nextFloat() * 2; break;
                case Materials: numMatStashes += 1; matFound = 1 + r.nextFloat() * 2; break;
                case AbandonedShelter: numAbShelters += 1; playEvents = true; break;
                case RandomPlayerShelter: numRPS += 1; playEvents = true; break;
                case EnemyStronghold: numEnStrong += 1; break;
                default: s += "\n Not implemented: "+f.toString(); break;
            }
        }
        if (foundList.size() == 0)
            Log(new Log(LogTextID.scoutingFailure, LogType.INFO));
        else
            Log(new Log(LogTextID.scoutingSuccess, LogType.INFO));

        /// Check config for preferred display verbosity of the search results?
        Log(new Log(LogTextID.scoutRandomEncounter, LogType.INFO, numEncounters+""));
        s += numEncounters == 1? "\n- encounter a group of monsters from the Evergreen" : numEncounters > 1? "\n- encounter "+numEncounters+" groups of monsters" : "";
        for (int i = 0; i < numEncounters; ++i) {
            Encounter enc = new Encounter(Finding.RandomEncounter, this);
            encountersToProcess.add(enc);
        }
        s += numFoodStashes > 1? "\n- find "+numFoodStashes+" stashes of food, totalling at "+Stringify(foodFound)+" units" : numFoodStashes == 1? "\n a stash of food: "+Stringify(foodFound)+" units" : "";
        Adjust(Stat.FOOD, foodFound);
        s += numMatStashes > 1? "\n- find "+numMatStashes+" stashes of materials, totalling at "+Stringify(matFound)+" units" : numMatStashes == 1? "\n a stash of materials: "+Stringify(matFound)+" units" : "";
        Adjust(Stat.MATERIALS, amount);
        /// Advanced shit... Queue up for exploration later?
        s += numAbShelters > 1? "\n- find "+numAbShelters+" seemingly abandoned shelters" : numAbShelters == 1? "\n- find an abandoned shelter." : "";
        Adjust(Stat.ABANDONED_SHELTER, numAbShelters);
        // Find max 1 player shelter per scouting round?
        s += numRPS >= 1? "\n- find a shelter which seems to be inhabited" : "";
        Adjust(Stat.RANDOM_PLAYERS_SHELTERS, numRPS >= 1? 1 : 0);
        s += numEnStrong >= 1? "\n- find an enemy stronghold." : "";
        Adjust(Stat.ENEMY_STRONGHOLDS, numEnStrong >= 1? 1 : 0);

        Log(s, LogType.INFO);

    }
    void BuildDefenses()
    {
        float emit = ConsumeMaterials(hoursPerAction * 0.5f);
        float progress = Dice.RollD3(2 + Get(Skill.Architecting).Level());
        progress /= Get(Stat.SHELTER_DEFENSE);
        progress *= t_starvingModifier;
        CalcMaterialModifier();
        progress *= t_materialModifier;
        progress *= hoursPerAction;
        Adjust(Stat.SHELTER_DEFENSE_PROGRESS, progress);
        float requiredToNext = Get(Stat.SHELTER_DEFENSE) * 10;
        Log(da.text + ": Shelter defense progress increased by " + Stringify(progress) + " units. Progress is now at " + Stringify(Get(Stat.SHELTER_DEFENSE_PROGRESS)) + " units out of " + Stringify(requiredToNext)+ ".", LogType.INFO);
        if (Get(Stat.SHELTER_DEFENSE_PROGRESS) >= requiredToNext)
        {
            Adjust(Stat.SHELTER_DEFENSE_PROGRESS, -requiredToNext);
            Adjust(Stat.SHELTER_DEFENSE, 1);
            Log("Shelter defense reached level "+GetInt(Stat.SHELTER_DEFENSE)+"!", LogType.PROGRESS);
        }
    }
    // Varies with skills n stuff. Generates emissions.
    private float ConsumeMaterials(float baseAmount)
    {
        float s = baseAmount;
        int lMatEfficiency = Get(Skill.MaterialEfficiency).Level();
        switch(lMatEfficiency)
        {
            case 4: s *= 0.6f; // 0.3,   40,  Multiplicative effects for each timer :)
            case 3: s *= 0.7f; // 0.5,   20,
            case 2: s *= 0.8f; // 0.72,   10,
            case 1: s *= 0.9f; break; // 0.9,   5,
            default:
            case 0: break;
        }
        Adjust(Stat.MATERIALS, -s); // Consume! 1 unit of materials per hour?
        GenerateEmissions(s);
        return s;
    }
    private void GenerateEmissions(float amount)
    {
        Adjust(Stat.EMISSIONS, amount);
    }

    public Skill Get(Skill skillType) {
        int index = skillType.ordinal();
        return skills.get(index);
    }

    // To be called after taking required materials. Will calculate material modifier for giving penalties on material debts.
    float CalcMaterialModifier()
    {
        t_materialModifier = (Get(Stat.MATERIALS) < 0 ?  (1 / (1 + Math.abs(Get(Stat.MATERIALS)))): 1);
        if (t_materialModifier < 1) // IF negative, add warning message and reset to 0 - cannot go further negative.
        {
            Log("A lack of materials reduced progress.", LogType.PROBLEM_NOTIFICATION);
            SetInt(Stat.MATERIALS, 0); // Reset materials to 0.
        }
        return t_materialModifier;
    }
    public void GainEXP(int expGained)
    {
        // Check queued skills.
        int xp = expGained;
        while (xp > 0 && skillTrainingQueue.size() > 0)
        {
            Skill next = Skill.GetFromString(skillTrainingQueue.get(0));
            if (next == null){
                System.out.println("Bad skill String: "+skillTrainingQueue.get(0));
                System.exit(16);
            }
            next.ordinal();
            Skill toSkillUp = skills.get(next.ordinal());
            int needed = toSkillUp.EXPToNext();
            if (needed <= 0)
            {
                System.out.println("Needed " + needed + ": skipping this skill, de-queueing.");
                skillTrainingQueue.remove(0);
                continue;
            }
            System.out.println("EXP to next level? "+needed);
            int toGain = xp;
            if (needed < xp)
                toGain = needed;
            xp -= toGain;
            int oldLevel = toSkillUp.Level();
            int levelReached = toSkillUp.GainExp(toGain);
            int newLevel = toSkillUp.Level();
            if (newLevel > oldLevel)
            {
                Log("Skill " + toSkillUp.text + " reached level " + levelReached + "!", LogType.EXP);
                skillTrainingQueue.remove(0);
            }
        }
        // If queue empty, place in unallocated points.
        Adjust(Stat.UNALLOCATED_EXP, xp);
    }

    public void PrepareForCombat(boolean defendingShelter) {
        // Load data into the Combatable variables from the more persistant ones saved here.
        Combatable c = (Combatable) this;
        c.attack = defendingShelter?  ShelterAttack() : OnTransportAttack();
        c.defense = defendingShelter? ShelterDefense() : OnTransportDefense();
        c.hp = GetInt(Stat.HP);
        c.maxHP = MaxHP();
        c.attackDamage = Damage();
        c.attacksPerTurn = AttacksPerTurn();
        c.fleeSkill = Get(Skill.FleetRetreat).Level();
        c.fleeBonusFromTransport = CurrentTransport().Get(TransportStat.FleeBonus);
        c.ranAway = false; // Reset temporary variables such as fleeing.
        consecutiveFleeAttempts = 0;
    }
    private int AttacksPerTurn()
    {
        int attacks = 1;
        attacks += (UnarmedCombatBonus() - 1) / 2;
        if (GetEquippedWeapon() != null)
            attacks += GetEquippedWeapon().Get(InventionStat.BonusAttacks);
        return attacks;
    }

    public List<String> KnownPlayerNames()
    {
        List<String> ll = new ArrayList<>();
        return ll;
    }

    public static Player NewAI(String name)
    {
        Player p =  new Player();
        p.SetName(name);
        p.isAI = true;
        return p;
    }

    public void PrintAll() {
        System.out.print("\nName: " + name +" stats:");
        for (int i = 0; i < statArr.length; ++i)
            System.out.print(" "+statArr[i]);
        System.out.println("\n skills:");
        for (int i = 0; i < skills.size(); ++i)
        {
            Skill s = skills.get(i);
            if (s == null)
                continue;
            System.out.print(" "+s.text+":"+s.Level()+":"+s.TotalExp());
        }
        System.out.print("\n inventions:");
        for (int i = 0; i < inventions.size(); ++i)
        {
            Invention inv = inventions.get(i);
            System.out.print(" "+inv.type.text()+": \""+inv.name+"\", ");
        }
        System.out.print("\n inventory:");
        for (int i = 0; i < inventory.size(); ++i)
        {
            Invention inv = inventory.get(i);
            System.out.print(" "+inv.type.text()+": \""+inv.name+"\", ");
        }
        System.out.println();
    }

    public static Player fromByteArray(byte[] bytes) {
        Player player = new Player();
        boolean ok = player.fromByteArr(bytes);
        if (ok)
            return player;
        return null;
    }

    public boolean CredentialsMatch(Player playerInSystem) {
        if (name.equals(playerInSystem.name) == false)
            return false;
        if (password.equals(playerInSystem.password) == false)
            return false;
        return true;
    }

    public String DailyActionsAsString() {
        String s = "";
        for (int i = 0; i < dailyActions.size(); ++i)
            s += dailyActions.get(i)+"; ";
        return s;
    }

    public void SaveFromClient(Player player) {
        dailyActions = player.dailyActions; // Copy over.
        skillTrainingQueue = player.skillTrainingQueue;
        // Other stuff? Equipment

    }

    public void InformListeners() {
        for (int i = 0; i < listeners.size(); ++i) {
            PlayerListener pl = listeners.get(i);
            if (!IsAlive())
                pl.OnPlayerDied(this);
        }
    }

    public void SaveLog(List<LogType> filterToSkip, String folder) {
        String path = folder+"/"+gameID+"_player_log_"+name+".txt";
        System.out.println("SavePlayerLog, dumping logs to file "+path);
        try {
            FileOutputStream file = new FileOutputStream(path);
            for (int i = 0; i < log.size(); ++i) {
                Log l = log.get(i);
                boolean skip = false;
                for (int j = 0; j < filterToSkip.size(); ++j) {
                    if (filterToSkip.get(j).ordinal() == l.type.ordinal())
                        skip = true;
                }
                if (skip)
                    continue;;
                file.write((log.get(i).text + "\n").getBytes());
            }
            file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addLogListener(LogListener logListener) {
        logListeners.add(logListener);
    }

    public float AggregateAttackBonus() {
        return BaseAttack() * (1 + 0.12f * (Damage().Average() - 3.5f)) * (1 + 0.5f * (AttacksPerTurn() - 1));
    }
}
