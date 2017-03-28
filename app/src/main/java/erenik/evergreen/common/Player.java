package erenik.evergreen.common;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import erenik.evergreen.Game;
import erenik.evergreen.Simulator;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Invention.InventionStat;
import erenik.evergreen.common.Invention.InventionType;
//import erenik.evergreen.android.App;
import erenik.evergreen.common.combat.Combatable;
import erenik.evergreen.common.encounter.Encounter;
import erenik.evergreen.common.event.Event;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogListener;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.logging.LogType;
import erenik.evergreen.common.player.*;
import erenik.util.Byter;
import erenik.util.Dice;
import erenik.util.EList;
import erenik.weka.transport.TransportOccurrence;
import erenik.weka.transport.TransportType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static erenik.evergreen.common.Invention.InventionStat.InventingBonus;
import static erenik.evergreen.common.Invention.InventionStat.ParryBonus;

/**
 * Created by Emil on 2016-10-25.
 */
public class Player extends Combatable implements Serializable {
    public void UpdateFrom(ClientData cd) { // Client-side.
        long idSeen = (long) Get(Config.LatestLogMessageIDSeen); // Those configs that we may have manipulated during updates, save them....!
        this.cd = cd; // Just assign it, should be fine. NO!
        Set(Config.LatestLogMessageIDSeen, idSeen);
    }

    /// Why need the copy..?
    public ClientData GetClientData() { // Server-side.
//        cd.PrintDetails();
        return cd;
    }




    // The client data as held, to reduce abuse..?
    public ClientData cd = new ClientData();

    int totalLogMessagesOnServer = 0;
    /// For logs or extent of them to send along.
    public static final int SEND_ALL = 0;
    public static final int SEND_CLIENT_SEEN_MESSAGES = 1;
    public static final int SEND_SERVER_NEW_MESSAGES = 2; // Those confirmed by client & server and new messages from server only (pretty much 2 days worth of log messages).
    public static final int SEND_MESSAGES_SINCE_LAST_NEW_DAY = 3;
    public static final int SEND_NO_LOG_MESSAGES = 4;
    // For whole data or parts of it. SEND_ALL or CREDENTIALS_ONLY
    public static final int CREDENTIALS_ONLY = 5;

    EList<PlayerListener> listeners = null;
    EList<LogListener> logListeners = null;
    EList<Encounter> encountersToProcess = null;
    public String email = "";
    private String bonus = ""; // A string?
    public int sendLogs = SEND_ALL; // Locally used variable to manipulate what is sent to the object streams when saving and loading.
    public String sysmsg = ""; // System message. Should present to user if it has not been presented earlier (check via preferences the oldest saved system message).

    /// Statistical fields only used Server-side.
    public EList<Date> updatesFromClient = null;
    public int sendAll = CREDENTIALS_ONLY;

    /// Creates stuff not added automatically - after creation, or after loading from file via e.g. readObject.
    void Init() {
        cd = new ClientData();
        cd.Init();
        encountersToProcess = new EList<>();
        transports = Transport.DefaultTransports();
        listeners = new EList<>();
        logListeners = new EList<>();

        updatesFromClient = new EList<>();
        DefaultLogTypesToShow();
    }

    // Adds a listener for state changes to the player.
    public void addStateListener(PlayerListener listener){ listeners.add(listener);};
    /// Local simulator for proceeding to the next day. Isn't used for the multiplayer games.
    Simulator simulator = Simulator.getSingleton();
    /// The current game you are playing in.
//    Game game = null; // Re-point it, or create locally as needed.

    public long lastSaveTimeSystemMs = 0; // Last time it was saved? Or returned to us from the server?
    public long lastEditSystemMs = 0; // Last time we edited it?

    static Random r = new Random(System.nanoTime());
    // Based on the Transport enum as defined in player/Transport.java
    // Only variable we care about for transports as far as configurability is concerned.
    // Contains the seconds the transports were detected during the last 24 or 36 hours.
    public EList<Transport> transports = null;

    public int activeAction = -1;
    public boolean playEvents = false;
    /// Increment every passing day. Stop once dying.
    public int TurnsSurvived() {
        return (int) Math.round(Get(Stat.TurnSurvived));
    };
    /// Temporary/chaning starving modifier, changes each turn. Default 1, lower when starving.
    float t_starvingModifier = 1;
    float t_materialModifier = 1; // Same as starving, lowers when negative (debt) on action requirements of materials.

    // Serialization version.
    public static final long serialVersionUID = 1L;
    
    Invention GetEquipped(InventionType queryType) {
        EList<Invention> equipped = GetEquippedInventions();
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

    /// EList of events to evaluate/process/play-mini-games. Old events are removed from the list.
    EList<Event> events = new EList<Event>();
    /// Log of messages for this specific player.
    public EList<Log> log = new EList<Log>();
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

    public EList<String> knownStrongholds = new EList<>();
    public EList<String> knownPlayerNames = new EList<>();

    // Auto-created. Start-using whenever.
    public EList<LogType> logTypesToShow = null;
    public void DefaultLogTypesToShow(){
        logTypesToShow = new EList<LogType>(LogType.values());
    };
    public boolean isAI = false;
    /// Used for clients/single-simulator for self.
    public Player() {
        Init();
        SetName("Parlais Haux Le'deur");
        SetDefaultStats();
        Set(Config.CreationTime, System.currentTimeMillis());
    }
    // Delivers a String based on using ObjectOutputStream, then saving the bytes.
    public byte[] toByteArr() {
        return Byter.toByteArray(this);
    }
    public static Player fromByteArr(byte[] bytes) {
        Object p = Byter.toObject(bytes);
        if (p instanceof Player)
            return (Player) p;
        return null;
    }

    private static final int VERSION_1_EMAIL = 1,
        VERSION_2_SYSMSG = 2,
            VERSION_3_TRANSPORT_ARR_LIST = 3,
            VERSION_4_CREDENTIALS_ONLY = 4,
            VERSION_5_CREDENTIALS_NO_PW = 5;

    // To conform to the Serializable interface.
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        writeTo(out);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        readFrom(in);
    }

    public void writeTo(java.io.ObjectOutputStream out) throws IOException {
        System.out.println("Player writeObject");
        int version = VERSION_4_CREDENTIALS_ONLY; // 0 - Added active actions. 1- email
        out.writeInt(version);
        out.writeInt(gameID);
        out.writeInt(sendAll);
        if (sendAll == CREDENTIALS_ONLY){             // Then send only the credentials..
            out.writeObject(name);
            out.writeObject(email);
            out.writeObject(password);
            return;
        }

        out.writeObject(sysmsg);
        out.writeObject(name);
//        System.out.println("name: "+name);
        out.writeObject(email);
        out.writeObject(password);

        out.writeInt(transports.size());
        for (int i = 0; i < transports.size(); ++i)
            transports.get(i).writeTo(out);

        if (!cd.writeTo(out)) // Write the client-data
            throw new IOException("Failed writing ClientData");
        writeLogs(out);

        out.writeObject(inventionCurrentlyBeingCrafted);

        out.writeObject(knownStrongholds);
        out.writeObject(knownPlayerNames);
        out.writeObject(logTypesToShow);

        out.writeBoolean(isAI);
        out.writeLong(lastEditSystemMs);
        out.writeLong(lastSaveTimeSystemMs);
    }

    public boolean readFrom(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        System.out.println("Player start");
        try {
            int version = in.readInt();
            Init();
            SetDefaultStats();
            // Actually load here
            gameID = in.readInt();
            if (version >= VERSION_4_CREDENTIALS_ONLY)
                sendAll = in.readInt();
            if (sendAll == CREDENTIALS_ONLY){             // Then send only the credentials..
                name = (String) in.readObject();
                email = (String) in.readObject();
                password = (String) in.readObject();
                System.out.println("Player read - Credentials only");
                return true;
            }
            System.out.println("Player read - All");

            if (version >= VERSION_2_SYSMSG)
                sysmsg = (String) in.readObject();
            name = (String) in.readObject();
            //        System.out.println("name: "+name);
            email = (String) in.readObject();
            password = (String) in.readObject();

            int numTransports = in.readInt();
            transports.clear();
            for (int i = 0; i < numTransports; ++i){
                Transport t = Transport.readFrom(in);
                if (t != null)
                    transports.add(t);
            }

            System.out.println("Player read - before log");

            if (!cd.readFrom(in)){
                System.out.println("Failed to read client-data.");
                return false;
            }
            readLogs(in);

            System.out.println("Player read - after log");
            inventionCurrentlyBeingCrafted = (Invention) in.readObject();

            knownStrongholds = (EList<String>) in.readObject();
            knownPlayerNames = (EList<String>) in.readObject();
            logTypesToShow = (EList<LogType>) in.readObject();

            isAI = in.readBoolean();
            lastEditSystemMs = in.readLong();
            lastSaveTimeSystemMs = in.readLong();
            if (log == null)
                log = new EList<>();
            if (logTypesToShow == null || logTypesToShow.size() == 0)
                DefaultLogTypesToShow();
        }catch (ClassCastException e){
            System.out.println("Tis bad.");
            e.printStackTrace();
            return false;
        }
//        System.out.println("Init from readOBject!");
        return true;
    }

    private void writeLogs(ObjectOutputStream out) throws IOException {
        out.writeInt(sendLogs);
        switch (sendLogs){
            case SEND_NO_LOG_MESSAGES:
                // Save an empty EList instead of the regular one.
                // out.writeObject(new EList<>());
                break;
            case SEND_CLIENT_SEEN_MESSAGES:
                EList<Log> clientSeen = new EList<>();
                for (int i = 0; i < log.size(); ++i){
                    Log l = log.get(i);
                    if (l.displayedToEndUser == 1)
                        clientSeen.add(l);
                }
                System.out.println("Client seen messages only: "+clientSeen.size()+" / "+log.size());
                out.writeObject(clientSeen);
                break;
            case SEND_SERVER_NEW_MESSAGES:
                EList<Log> serverConfirmed = new EList<>();
                int startIndex = log.size() > 100? log.size() - 100 : 0; // Send at most 100 messages?
                for (int i = startIndex; i < log.size(); ++i){
                    Log l = log.get(i);
//                    System.out.println(i+" Displayed to end user: "+l.displayedToEndUser);
                    if (l.displayedToEndUser == 0)
                        serverConfirmed.add(l);
                }
                System.out.println("Server new messages only: "+serverConfirmed.size()+" / "+log.size());
                out.writeObject(serverConfirmed);
                break;
            case SEND_MESSAGES_SINCE_LAST_NEW_DAY:
                EList<Log> sinceLastDay = new EList<>();
                int indexOfLastNewDay = GetLastNewDayLogIndex();
                for (int i = indexOfLastNewDay; i < log.size(); ++i){
                    Log l = log.get(i);
                    sinceLastDay.add(l);
                }
                System.out.println("Since last-day messages only: "+sinceLastDay.size()+" / "+log.size());
                out.writeObject(sinceLastDay);
                break;
            case SEND_ALL:
                out.writeObject(log);
                break;
            default:
                System.out.println("SHOULDN'T BE HERE, Player write object.");
                System.exit(14);
        }
    }

    private void readLogs(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int logMessagesRead = in.readInt();
        switch(logMessagesRead){
            case SEND_NO_LOG_MESSAGES:
                log = new EList<Log>();
                break;
            default:
                //        if (logMessagesRead != SEND_ALL) // If not all (not saving locally), display what it is that we got.
                //          System.out.println("log messages read/received: "+logMessagesRead);
                log = (EList<Log>) in.readObject();
        }
    }

    private void readObjectNoData() throws ObjectStreamException
    {

    }

    private int GetLastNewDayLogIndex() {
        for (int i = log.size() - 1; i >= 0; --i){
            if (log.get(i).TextID() == LogTextID.newDayPlayerTurnPlayed)
                return i;
        }
        return 0;
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
    int Speed() {
        return GetInt(Stat.SPEED);
    }
    public int UnarmedCombatBonus() {
        return GetEquippedWeapon() == null? Get(Skill.UnarmedCombat).Level() : 0;
    }
    public EList<Invention> GetEquippedInventions() {
        EList<Invention> equipped = new EList<>();
        for (int i = 0; i < cd.inventory.size(); ++i) {
            Invention item = cd.inventory.get(i);
            if (item.Get(InventionStat.Equipped) >= 0) {
                equipped.add(item);
            }
        }
        return equipped;
    }
    /// Fetches total form all equipped gear.
    public int GetEquipped(InventionStat stat) {
        EList<Invention> equipped = GetEquippedInventions();
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
    public void SetDefaultStats() {
        isPlayer = true;
        // Default stats?
        for (int i = 0; i < Stat.values().length; ++i)
            cd.statArr[i] = Stat.values()[i].defaultValue;
        for (int i = 0; i < cd.skills.size(); ++i) // Reset EXP in each skill?
            cd.skills.get(i).setTotalEXP(0);
        if (cd.inventions != null)
            cd.inventions.clear();
        transports = Transport.DefaultTransports();
        // Clear both queues.
        cd.dailyActions.clear();
        cd.skillTrainingQueue.clear();
    }

    public void Adjust(Stat s, float amount) {
        cd.statArr[s.ordinal()] += amount;
        if (s == Stat.HP && cd.statArr[s.ordinal()] <= 0) {
            System.out.println("Died, informing listeners");
            for (int i = 0; i < listeners.size(); ++i)
                listeners.get(i).OnPlayerDied(this);
        }
    }
    // Getter for main stats.
    float Get(int stat)
    {
        return cd.statArr[stat];
    }
    public float Get(Stat s)
    {
        return cd.statArr[s.ordinal()];
    }
    public int GetInt(Stat s)
    {
        return (int) cd.statArr[s.ordinal()];
    }
    void SetInt(Stat s, int am)
    {
        cd.statArr[s.ordinal()] = am;
    }
    public void Set(Stat stat, float value) {
        cd.statArr[stat.ordinal()] = value;
    }
    public void Set(Config conf, float value){ cd.configArr[conf.ordinal()] = value; }
    public float Get(Config conf) { return  cd.configArr[conf.ordinal()]; }
    /// Saves to local "preferences"
    void AddLog(Log l){
        log.add(l);
        for (int i = 0; i < logListeners.size(); ++i) {
            LogListener ll = logListeners.get(i);
            ll.OnLog(l, this);
        }
    }
    public void LogInfo(LogTextID id) {
        Log l = new Log(id, LogType.INFO);
        AddLog(l);
    }
    public void LogInfo(LogTextID id, String arg1) {
        Log l = new Log(id, LogType.INFO, arg1);
        AddLog(l);
    }
    public void LogInfo(LogTextID id, String arg1, String arg2) {
        Log l = new Log(id, LogType.INFO, arg1, arg2);
        AddLog(l);
    }
    public void LogInfo(LogTextID id, String arg1, String arg2, String arg3) {
        Log l = new Log(id, LogType.INFO, arg1, arg2, arg3);
        AddLog(l);
    }
    public void Log(LogTextID id, LogType lt) {
        Log l = new Log(id, lt);
        AddLog(l);
    }
    public void Log(LogTextID id, LogType lt, String arg1) {
        Log l = new Log(id, lt, arg1);
        AddLog(l);
    }
    public void Log(LogTextID id, LogType lt, String arg1, String arg2) {
        Log l = new Log(id, lt, arg1, arg2);
        AddLog(l);
    }

    public Transport CurrentTransport() {
        for (int i = 0; i < transports.size(); ++i) {
            Transport t = transports.get(i);
            if (Get(Stat.CurrentTransport) == t.tt.ordinal())
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
        updatesFromClient.clear(); // Clear this array when new days are processed.
        if (GetInt(Stat.HP) <= 0) {
            // TODO: Add a listener-callback mechanism for when the player dies.
            //            App.GameOver();
            return;
        }
        NewDay();  // New day :3
        LogInfo(LogTextID.newDayPlayerTurnPlayed, ""+(int) Get(Stat.TurnPlayed));
        Transport t = Transport.RandomOf(transports);
       // System.out.println("Randomed transport.. "+t.name());
        Set(Stat.CurrentTransport, t.tt.ordinal());

        // Calculate the general transport bonus granted from the previous day?


        // Or just give bonuses and random events based on the distances traveled etc?


        // Bonuses for greening habits?
        LogInfo(LogTextID.transportOfTheDay, CurrentTransport().tt.name());
        float emissionsToGenerate = CurrentTransport().Get(TransportStat.EmissionsPerDay);
        if (emissionsToGenerate > 0) // Really print this? Better have it more secret? and more random feeling.
            // Log("Generated "+Stringify(emissionsToGenerate)+" units of emissions.", LogType.INFO);
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
            Log(LogTextID.starvingLostHP, LogType.OtherDamage, ""+(-loss));
            if (!IsAlive()) {
                Log(LogTextID.diedOfStarvation, LogType.DEFEATED);
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
        if (IsAlive() == false)
            KnockOut();
        for (int i = 0; i < listeners.size(); ++i) // Inform listeners, new day is over.
            listeners.get(i).OnPlayerNewDay(this);

        // Clear queue of daily actions to ensure people actually think what they will do? No?
    //    dailyActions.clear();
    }

    private void KnockOut() {
        System.out.println("Knocking out...!");
        Adjust(Stat.Lives, -1);
        if (Get(Stat.Lives) <= 0){ // Fully dead?
            return;
        }
        Set(Stat.HP, Get(Stat.MAX_HP) / 2); // Reset HP to 25% of max.
        Adjust(Stat.FOOD, Get(Stat.FOOD) * -0.75f); // Reduce food.
        Adjust(Stat.MATERIALS, Get(Stat.MATERIALS) * -0.75f); // Reduce food.
        // Reduce other stuff?
        LogInfo(LogTextID.secondLife);
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
    Action da;
    void EvaluateActions(Game game) {
        t_starvingModifier = GetInt(Stat.HP) >= 0? 1.0f : 1 / (1 + Math.abs(Get(Stat.HP)) * 0.5f);
        // Have this increase with some skill?
        float hoursSimulated = 6.f;
        int div = cd.dailyActions.size();
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
            ; //Log("Having scheduled too much to do during the day, you manage to lose a lot of time between the actions you had intended to do. You are even forced to cancel entirely some of the actions you wanted to do.", LogType.PROBLEM_NOTIFICATION);
        else if (cd.dailyActions.size() > 4)
            Log(LogTextID.tooManyDailyActionsLossOfTime, LogType.PROBLEM_NOTIFICATION);
        hoursPerAction = hoursSimulated / div;
//        System.out.println("hoursPerAction: "+hoursPerAction);
        foodHarvested = 0.0f;
        // Execute at most 8 actions per day, regardless of queue.
        for (int i = 0; i < cd.dailyActions.size() && i < MAX_ACTIONS; ++i) {
            /// Parse actions and execute them.
            da = cd.dailyActions.get(i);
            EvaluateAction(da, game);
        }
    }

    String formattedFloat(float value, int decimals){
        String format = "%."+decimals+"f"; // e.g. "%.2f"
        String formattedFloat = String.format(Locale.ENGLISH, format, value);
        return formattedFloat;
    }


    void EvaluateAction(Action a, Game game) {
        float units = 1;
        String playerName = a.GetPlayerName();
        Player player = null;
        if (playerName != null)
            player = game.GetPlayer(playerName);
        /// THEN ACTIVE ACTIONS
        if (a.aaType != null)
            switch (a.aaType){
                case GiveResources:
                    if (player == null){
                        System.out.println("NULL player");
                        return;
                    }
                    Stat s = null;
                    String type = a.Get(ActionArgument.ResourceType);
                    if (type.equals("Food"))
                        s = Stat.FOOD;
                    else if (type.equals("Materials"))
                        s = Stat.MATERIALS;
                    else {
                        System.out.println("Bad resource type");
                        return;
                    }
                    String quantity = a.Get(ActionArgument.ResourceQuantity);
                    float qu = Float.parseFloat(quantity);
                    float quantAvail = Get(s);
                    float toSend = Math.min(qu, quantAvail);
                    if (toSend < 0) {
                        System.out.println("negative quant");
                        return;
                    }
                    player.Adjust(s, toSend);
                    Adjust(s, -toSend);
                    LogInfo(LogTextID.SentPlayerResources, playerName, type, formattedFloat(toSend, 2));
                    player.LogInfo(LogTextID.ReceivedPlayerResources, this.name, type, formattedFloat(toSend, 2));
                    break;
                case SendMessage:
                    if (player == null){
                        System.out.println("NULL player");
                        return;
                    }
                    LogInfo(LogTextID.MessageSentToPlayer, playerName);
                    player.LogInfo(LogTextID.MessageReceivedFromPlayer, playerName, a.Get(ActionArgument.Text));
                    break;
            }

        if (a.daType != null)
            switch (a.daType) {
                case GatherFood:
                    units = Dice.RollD3(2);  // 2 to 6 base.
                    units += Get(Skill.Foraging).Level(); // + Foraging level constant?
                    units += CurrentTransport().Get(TransportStat.ForagingBonus);
                    units += GetEquipped(InventionStat.HarvestBonus); // Add harvest bonus from equipment.
                    if (units < 1) units = 1; // Get at least 1.
                    units *= t_starvingModifier;
                    units *= hoursPerAction;
                    Adjust(Stat.FOOD, units);
                    LogInfo(LogTextID.foragingFood, Stringify(units));
                    break;
                case GatherMaterials:
                    units = Dice.RollD3(2); // 2 to 6 base.
                    units += CurrentTransport().Get(TransportStat.MaterialGatheringBonus);
                    units += GetEquipped(InventionStat.ScavengingBonus); // Add scavenging bonus from equipment.
                    units *= t_starvingModifier;
                    units *= hoursPerAction;
                    LogInfo(LogTextID.gatherMaterials, Stringify(units));
                    Adjust(Stat.MATERIALS, units);
                    break;
                case Scout:
                    Scout();
                    break;
                case Recover:
                    units = (1 + 0.5f * Get(Skill.Survival).Level()); // 1 + 0.5 for each survival skill.
                    units += GetEquipped(InventionStat.RecoveryBonus); // +2 for First aid kit, for example.
                    units *= 0.5f * hoursPerAction; // Recovery +50/100/150/200/250%
                    units *= t_starvingModifier;
                    Adjust(Stat.HP, units);
                    ClampHP();
                    LogInfo(LogTextID.recoverRecovered, ""+units);
                    break;
                case BuildDefenses: BuildDefenses(); break;
    //            case AugmentTransport: AugmentTransport(); break;
                case LookForPlayer: LookForPlayer(a, game); break;
    //            case Expedition: Log("Not implemented yet - Expedition", LogType.ATTACKED); break;
                case Invent: Invent(a); break;
                case Craft: Craft(a); break;
                case Steal: Steal(a, game); break;
                case AttackAPlayer: AttackAPlayer(a, game);
                    break;
                case Study:
                    // Gain exp into current skill perhaps..?
                    int toGain = Dice.RollD3(2) + Get(Skill.Studious).Level();
                    Log(LogTextID.studiesEXP, LogType.PROGRESS);
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
        float amount = 0;
        for (int i = 0; i < hoursPerAction; ++i) {
            int diceRoll = r.nextInt(100);
            if (diceRoll > 50){
                amount += Dice.RollD3(1);
                ++successful;
            }
        }
        if (successful > hoursPerAction * 0.75) {
            amount += Get(Stat.EMISSIONS) * 0.25f; // Successful, get bonus 25% of total.
            Log(LogTextID.reduceEmissionsSuccessful, LogType.SUCCESS, Stringify(amount));
        }
        else if (successful > hoursPerAction * 0.5) {
            amount += Get(Stat.EMISSIONS) * 0.1f; // Successful, get bonus 25% of total.
            Log(LogTextID.reduceEmissionsMostlySuccessful, LogType.SUCCESS, Stringify(amount));
        }
        else if (successful > hoursPerAction * 0.25) {
            amount += Get(Stat.EMISSIONS) * 0.05f; // Successful, get bonus 25% of total.
            Log(LogTextID.reduceEmissionsNotSoSuccessful, LogType.SUCCESS, Stringify(amount));
        }
        else
            Log(LogTextID.reduceEmissionsFailed, LogType.SUCCESS, Stringify(amount));
        Adjust(Stat.EMISSIONS, -amount);
        if (Get(Stat.EMISSIONS) < 0) {
            Set(Stat.EMISSIONS, 0);
        }
    }

    private void AttackAPlayer(Action da, Game game) {
        // Check if a player was provided?
        String targetName = da.requiredArguments.get(0).value;
        Player p = game.GetPlayer(targetName);
        if (p == null) {
            Log(LogTextID.unableToFindPlayerByName, LogType.ACTION_FAILURE, targetName);
            return;
        }
        Log(LogTextID.attackPlayer, LogType.ATTACK, targetName);
        p.Log(LogTextID.attackedByPlayer, LogType.ATTACKED, name);
        // Create a custom encounter for this.
        Encounter enc = new Encounter(this, p);
        enc.Simulate(); // Simulate it.

    }

    private void AugmentTransport() {
        /*
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
        */
    }
    private void Steal(Action da, Game game) {
        String targetPlayerName = da.requiredArguments.get(0).value;
        Player p = game.GetPlayer(targetPlayerName);
        if (p == null) {
            Log(LogTextID.unableToFindPlayerByName, LogType.ACTION_FAILURE, targetPlayerName);
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
            Log(LogTextID.stealFailedDetected, LogType.ATTACK_MISS, targetPlayerName);
            p.Log(LogTextID.playerTriedToStealFromYouFailedDetected, LogType.ATTACKED);
            /// Add stealing player into target player's list of known players, so that they may retaliate?
            p.FoundPlayer(name);

//            Log("While trying to steal from "+targetPlayerName+" you were detected! You didn't manage to steal anything.", LogType.ATTACK_MISS);
//            p.Log(""+name+" tried to steal from you, but failed as he was detected!", LogType.ATTACKED);
            return;
        }
        if (roll < 9) {
            Log(LogTextID.stealFailed, LogType.ATTACK_MISS);
            p.Log(LogTextID.somethingAmiss, LogType.INFO);
           // Log("While trying to steal from "+p.name+", you mistakenly caused attention, forcing you to retreat.", LogType.ATTACK_MISS);
//            p.Log("While walking in your shelter, you notice something grabbing your attention. You try to see what it was, but find nothing is amiss.", LogType.ATTACKED_MISS);
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
        Log(LogTextID.stealSuccess_whatName, LogType.ATTACK);
        p.Log(LogTextID.stolen, LogType.ATTACKED);
//        Log("Stole "+whatStolen+" from "+p.name+"!", LogType.ATTACK);
//        p.Log("Player "+name+" stole "+whatStolen+" from you!", LogType.ATTACKED);
    }

    void LookForPlayer(Action da, Game game) {
        // Determine chances.
        // Search query.
        // Found?
        String name = this.da.requiredArguments.get(0).value;
        Player player = null;
        // Try search for players now then.
        player = game.GetPlayer(name);
        if (player == null)
            player = game.GetPlayer(name, false, true);
        if (player == null)
            player = game.GetPlayer(name, true, false);
        if (player == null) {
            int randInt = r.nextInt(100);
            Log(LogTextID.searchPlayerFailed, LogType.ACTION_FAILURE, name);
//            Log("Despite searching, you were unable to find a player called "+name+".", LogType.ACTION_FAILURE);
            // Add chance to find random other players?
            if (randInt < 50) {
                Player randomPlayer = game.RandomPlayer(KnownNamesSelfIncluded());
                if (randomPlayer == null)
                    return;
                String newPlayer = randomPlayer.name;
                if (newPlayer != null && newPlayer != name) {
                    Log(LogTextID.searchPlayer_foundAnother, LogType.SUCCESS);
                    name = newPlayer;
//                    knownPlayerNames.add(newPlayer);
                }
                else // In-case you already know all the players in the game already.
                    return;
            }
            return;
        }
        FoundPlayer(name);
        Log(LogTextID.debug, LogType.INFO, "knownNAmes: "+knownPlayerNames);
    }

    private void FoundPlayer(String playerName) {
        if (knownPlayerNames == null)
            knownPlayerNames = new EList<String>();
        for (int i = 0; i < knownPlayerNames.size(); ++i){
            String n = knownPlayerNames.get(i);
            if (n.equals(playerName))
                return; // Already know this player. o-o
        }
        if (playerName.equals(this.name)){
            Log(LogTextID.foundPlayerSelfLol, LogType.SUCCESS, playerName);
        }
        Log(LogTextID.foundPlayer, LogType.SUCCESS, playerName);
//        Log("You found the player named "+name+"! You can now interact with that player.", LogType.SUCCESS);
        knownPlayerNames.add(playerName);
    }

    private EList<String> KnownNamesSelfIncluded() {
        EList<String> knownNames = new EList<>();
        if (knownPlayerNames == null)
            knownPlayerNames = new EList<String>();
        for (int i = 0; i < knownPlayerNames.size(); ++i)
            knownNames.add(knownPlayerNames.get(i));
        knownNames.add(name);
        return knownNames;
    }

    private void Craft(Action da) {
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
        for (int i = 0; i < cd.inventions.size(); ++i) {
            Invention inv = cd.inventions.get(i);
            System.out.println("Invention names: "+inv.name+", toCraft: "+whatToCraft);
            if (inv.name.equals(whatToCraft))
                toCraft = inv;
        }
        if (toCraft == null) {
            System.out.println("toCraft null, what did you wanna craft again?");
            System.exit(15);
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
        if (progress >= progressRequired) {
            // Crafted!
            Invention newlyCraftedObject = toCraft.CraftInventionFromBlueprint();
            float ratioOverProgressed = (progress - progressRequired) / progressRequired;
            System.out.println("ratioOverProgressed: "+ratioOverProgressed);
            Random rCrafting = new Random(System.nanoTime());
            int levelAdjustment = 0;
            while(ratioOverProgressed > 0) {
                float randF = rCrafting.nextFloat();
                if (randF < ratioOverProgressed) {
                    System.out.println("level increased +1");
                }
                ratioOverProgressed -= randF;
            }
            newlyCraftedObject.Set(InventionStat.QualityLevel, newlyCraftedObject.Get(InventionStat.QualityLevel) + levelAdjustment);
            // Update quality level.
            newlyCraftedObject.UpdateDetails();
            cd.inventory.add(newlyCraftedObject);
            LogInfo(LogTextID.craftingComplete, newlyCraftedObject.name);
        }
        else
        {
            LogInfo(LogTextID.craftingProgressed, ""+progress, ""+progressGained, ""+progressRequired);
//            Log("Crafting progressed by "+progress+" units. Progress is now at "+progressGained+" out of "+progressRequired+".", LogType.INFO);
            // Store as unfinished business?
        }
    }

    private void Invent(Action inventAction) {
        float emit = ConsumeMaterials(hoursPerAction * 0.5f);
        // How many times to random.
        float toRandom = 0.5f + hoursPerAction; // Roll once for each hour?
        toRandom *= t_starvingModifier;
        toRandom *= CalcMaterialModifier();
        String s = da.text+": ";
        // Check if inveting has been queued for any special item?
        boolean inventedSomething = false;
        System.out.println("toRandom iterations: "+toRandom);
        for (int i = 0; i < toRandom; ++i) { // Times to random.
            float relativeChance = toRandom > 1.0 ? 1 : toRandom;
            InventionType type = null;
            if (da.requiredArguments.size() > 0) {
                String typeStr = da.requiredArguments.get(0).value;
                type = InventionType.GetFromString(typeStr);
            }
            if (type == null) {
                System.out.println("Bad invention type.");
//                Log("Bad invention type", LogType.Error);
                System.exit(14);
                return;
            }
            if (type == InventionType.Any) {
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
                cd.inventory.add(inv.CraftInventionFromBlueprint());
                // Don't auto-equip... Present dialog for it.
             //   Equip(inv);
            }
            ++successiveInventingAttempts;
            toRandom -= 1;
        }
        if (inventedSomething == false)
            LogInfo(LogTextID.inventFailed);
//            Log(s+"Failed to invent anything new.", LogType.INFO);
    }
    /// Tries to equip target invention.
    public void Equip(Invention inv) {
        if (inv == null)
            return;
        Invention currentlyEquipped = GetEquipped(inv.type);
        if (currentlyEquipped != null)
            currentlyEquipped.Set(InventionStat.Equipped, -1); // Set as not equipped.
        inv.Set(InventionStat.Equipped, (int) Get(Stat.ID));
    }

    public float getInventionProgress(InventionType type, int subType)
    {
        float progress = 0;
        for (int i = 0; i < cd.inventions.size(); ++i)
        {
            Invention inv = cd.inventions.get(i);
            if (inv.type != type)
                continue;
            if (subType >= 0 && inv.Get(InventionStat.SubType) != subType)
                continue;
            progress +=  Math.pow(inv.Get(InventionStat.QualityLevel) + 1, 2) * inv.Get(InventionStat.TimesInvented); // Gives bonus for higher quality items, 1, 4, 9, 16, 25 for levels 1-5 respectively.
        }
        return progress;
    }
    Invention AttemptInvent(InventionType type, float relativeChance) {
        Invention inv = Invention.CreateBlueprint(type);
        int subType = inv.RandomizeSubType();
        // 1.0 base, + 0.1 for each level in Inventing, +0.25 of the invention progress in the same category/main invention type and 1x subtype progress.
        float bonusFromInventingBefore = 0.001f * getInventionProgress(type, -1) + 0.025f * getInventionProgress(type, subType);
        float inventingLevel = Get(Skill.Inventing).Level();
        float rMax = (1.f + 0.05f * inventingLevel + bonusFromInventingBefore) * relativeChance;
        rMax += 0.07f * GetEquipped(InventingBonus);  /// Random max increases by +0.07 (7%) for each point in inventing gear.
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
        for (int i = 0; i < cd.inventions.size(); ++i) {
            Invention i2 = cd.inventions.get(i);
            if (i2.name.equals(inv.name)) {
                // Save type of invention? Add progress to the invention we already had?
                i2.Adjust(InventionStat.TimesInvented, 1);
                LogInfo(LogTextID.inventingOldThoughts, inv.name);
               // Log("While trying to invent something new, your thoughts go back to the old version of the "+inv.name+", perhaps you will be luckier next time.", LogType.INFO);
                return null;
            }
        }
        cd.inventions.add(inv);
        Log(LogTextID.inventSuccess, LogType.SUCCESS, inv.type.text(), inv.name);
//        Log("Invented a new " + inv.type.text() + ": " + inv.name, LogType.SUCCESS);
        return inv;
    }

    void Scout() {
        int speed = Speed(); //  1 base.
        speed += CurrentTransport().Get(TransportStat.SpeedBonus); // + based on transport.
        speed += GetEquipped(InventionStat.ScoutingBonus); // Add scouting bonus from gear. e.g. +2
        // Randomize something each hour? Many things with speed?
        float speedBonus = (float) Math.pow(speed, 0.5f); // Square root we made earlier.
        System.out.println("Speed: "+speed+" bonus(pow-modified): "+speedBonus);
        float toRandom = hoursPerAction * speedBonus;
        toRandom *= t_starvingModifier;
        Map<Finding, Integer> chances = new HashMap<Finding, Integer>();
        // Increase liklihood of encounters for each passing turn when scouting -> Scout early on is safer.
        // There will however be more of other resources available later on. :D
        int turn = (int) Get(Stat.TurnSurvived);
        int randomEncounter = 5 + turn; // Increase chance of combat for each survived turn, and maybe emissions as well?
        randomEncounter /= (1 + 0.5f * Get(Skill.SilentScouting).Level()); // Each level of scouting reduces encounter rate.
        // -33% at 1st level, -50% at 2nd level, -60% at 3rd level, -66% at 4th, -71.5%, etc. (/1.5, /2, /2.5, /3, /3.5)
        chances.put(Finding.RandomEncounter, randomEncounter);
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
        EList<Finding> foundList = new EList<Finding>();
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
            Log(LogTextID.scoutingFailure, LogType.INFO);
        else
            Log(LogTextID.scoutingSuccess, LogType.INFO);

        /// Check config for preferred display verbosity of the search results?
        if (numEncounters > 0)
            Log(LogTextID.scoutRandomEncounter, LogType.INFO, numEncounters+"");
        for (int i = 0; i < numEncounters; ++i) {
            Encounter enc = new Encounter(Finding.RandomEncounter, this);
            encountersToProcess.add(enc);
        }
        if (foodFound > 0) {
            LogInfo(LogTextID.scoutFoodStashes, "" + numFoodStashes, Stringify(foodFound));
            Adjust(Stat.FOOD, foodFound);
        }
        if (matFound > 0) {
            LogInfo(LogTextID.scoutMatStashes, "" + numMatStashes, Stringify(matFound));
//            s += numMatStashes > 1 ? "\n- find " + numMatStashes + " stashes of materials, totalling at " + Stringify(matFound) + " units" : numMatStashes == 1 ? "\n a stash of materials: " + Stringify(matFound) + " units" : "";
            Adjust(Stat.MATERIALS, matFound);
        }
        /// Advanced shit... Queue up for exploration later?
        s += numAbShelters > 1? "\n- find "+numAbShelters+" seemingly abandoned shelters" : numAbShelters == 1? "\n- find an abandoned shelter." : "";
        Adjust(Stat.ABANDONED_SHELTER, numAbShelters);

        // Find max 1 player shelter per scouting round?
        s += numRPS >= 1? "\n- find a shelter which seems to be inhabited" : "";
        Adjust(Stat.RANDOM_PLAYERS_SHELTERS, numRPS >= 1? 1 : 0);

        s += numEnStrong >= 1? "\n- find an enemy stronghold." : "";
        Adjust(Stat.ENEMY_STRONGHOLDS, numEnStrong >= 1? 1 : 0);

//        Log(s, LogType.INFO);
    }
    void BuildDefenses() {
        float emit = ConsumeMaterials(hoursPerAction * 0.5f);
        float progress = Dice.RollD3(2 + Get(Skill.Architecting).Level()); // 2 to 6, increases by 1D3 for each skill in architecting.
        progress *= 1 + 0.25f * GetEquipped(InventionStat.ConstructionBonus); // + Construction bonus from gear, in percentage., e.g. + 50% for a basic construction kit.
        progress /= Get(Stat.SHELTER_DEFENSE);
        progress *= t_starvingModifier;
        CalcMaterialModifier();
        progress *= t_materialModifier;
        progress *= hoursPerAction;
        Adjust(Stat.SHELTER_DEFENSE_PROGRESS, progress);
        float requiredToNext = Get(Stat.SHELTER_DEFENSE) * 10;
        LogInfo(LogTextID.buildDefensesProgress, Stringify(progress), Stringify(Get(Stat.SHELTER_DEFENSE_PROGRESS)), Stringify(requiredToNext));
//        Log(da.text + ": Shelter defense progress increased by " + Stringify(progress) + " units. Progress is now at " + Stringify(Get(Stat.SHELTER_DEFENSE_PROGRESS)) + " units out of " + Stringify(requiredToNext)+ ".", LogType.INFO);
        if (Get(Stat.SHELTER_DEFENSE_PROGRESS) >= requiredToNext) {
            Adjust(Stat.SHELTER_DEFENSE_PROGRESS, -requiredToNext);
            Adjust(Stat.SHELTER_DEFENSE, 1);
            Log(LogTextID.defensesReachedLevel, LogType.PROGRESS, ""+GetInt(Stat.SHELTER_DEFENSE));
//            Log("Shelter defense reached level "+GetInt(Stat.SHELTER_DEFENSE)+"!", LogType.PROGRESS);
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
        return cd.skills.get(index);
    }

    // To be called after taking required materials. Will calculate material modifier for giving penalties on material debts.
    float CalcMaterialModifier() {
        t_materialModifier = (Get(Stat.MATERIALS) < 0 ?  (1 / (1 + Math.abs(Get(Stat.MATERIALS)))): 1);
        if (t_materialModifier < 1){ // IF negative, add warning message and reset to 0 - cannot go further negative.
            Log(LogTextID.materialShortageAffectingProgress, LogType.PROBLEM_NOTIFICATION);
            SetInt(Stat.MATERIALS, 0); // Reset materials to 0.
        }
        return t_materialModifier;
    }
    public void GainEXP(int expGained)
    {
        // Check queued skills.
        int xp = expGained;
        while (xp > 0 && cd.skillTrainingQueue.size() > 0)
        {
            Skill next = Skill.GetFromString(cd.skillTrainingQueue.get(0));
            if (next == null){
                System.out.println("Bad skill String: "+cd.skillTrainingQueue.get(0));
                System.exit(16);
            }
            next.ordinal();
            Skill toSkillUp = cd.skills.get(next.ordinal());
            int needed = toSkillUp.EXPToNext();
            if (needed <= 0)
            {
                System.out.println("Needed " + needed + ": skipping this skill, de-queueing.");
                cd.skillTrainingQueue.remove(0);
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
            if (newLevel > oldLevel) {
                Log(LogTextID.skillLeveledUp, LogType.EXP, toSkillUp.text, ""+levelReached);
                //Log("Skill " + toSkillUp.text + " reached level " + levelReached + "!", LogType.EXP);
                cd.skillTrainingQueue.remove(0);
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

    public EList<String> KnownPlayerNames()
    {
        EList<String> ll = new EList<>();
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
        for (int i = 0; i < cd.statArr.length; ++i)
            System.out.print(" "+cd.statArr[i]);
        System.out.println("\n skills:");
        for (int i = 0; i < cd.skills.size(); ++i)
        {
            Skill s = cd.skills.get(i);
            if (s == null)
                continue;
            System.out.print(" "+s.text+":"+s.Level()+":"+s.TotalExp());
        }
        System.out.print("\n inventions:");
        for (int i = 0; i < cd.inventions.size(); ++i)
        {
            Invention inv = cd.inventions.get(i);
            System.out.print(" "+inv.type.text()+": \""+inv.name+"\", ");
        }
        System.out.print("\n inventory:");
        for (int i = 0; i < cd.inventory.size(); ++i)
        {
            Invention inv = cd.inventory.get(i);
            System.out.print(" "+inv.type.text()+": \""+inv.name+"\", ");
        }
        System.out.println();
    }

    public static Player fromByteArray(byte[] bytes) throws Exception {
        try {
            return Player.fromByteArr(bytes);
        } catch (Exception e){
            throw new Exception("Could not parse properly");
        }
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
        for (int i = 0; i < cd.dailyActions.size(); ++i)
            s += cd.dailyActions.get(i)+"; ";
        return s;
    }

    public void SaveFromClient(Player clientPlayer) {
        updatesFromClient.add(new Date());
        cd.dailyActions = clientPlayer.cd.dailyActions; // Copy over queued actions.
        cd.skillTrainingQueue = clientPlayer.cd.skillTrainingQueue; // Skill training queue.
        cd.queuedActiveActions = clientPlayer.cd.queuedActiveActions; // And queued active actions.
        for (int i = 0; i < log.size() && i < clientPlayer.log.size(); ++i) {
            Log clientLog = clientPlayer.log.get(i);
            Log serverEquivalent = GetLog(clientLog.LogID());
            if (serverEquivalent == null){
                System.out.println("SOMETHING IS HORRIBLY WRONG syncing log messages");
                continue;
            }
            // Save the state of old log messages that have already been viewed by the player.
            serverEquivalent.displayedToEndUser = clientLog.displayedToEndUser;
            if (serverEquivalent.displayedToEndUser == 1)
                serverEquivalent.displayedToEndUser = 2;
        }
        transports = clientPlayer.transports; // Copy over all transports.
        System.out.println("SaveFromClient, transports: "+clientPlayer.TopTransportsAsString(3)); // Print the new transport data we received.
        EList<Invention> equipped = clientPlayer.GetEquippedInventions();         // Equip those items as requested by the player as well.
        for (int i = 0; i < equipped.size(); ++i){
            Invention item = equipped.get(i);
            boolean ok = EquipItemWithID(item.GetID());
            if (!ok){
                System.out.println("ERROR: Mismatch in item IDs!");
            }
            else
                System.out.println("Equipped: "+GetItemByID(item.GetID()).name);
        }

    }

    private Log GetLog(long id) {
        for (int i = 0; i < log.size(); ++i){
            if (log.get(i).LogID() == id)
                return log.get(i);
        }
        return null;
    }

    private Invention GetItemByID(long id) {
        for (int i = 0; i < cd.inventory.size(); ++i){
            Invention item = cd.inventory.get(i);
            if (item.GetID() == id) {
                return item;
            }
        }
        return null;
    }

    private boolean EquipItemWithID(long id) {
        Invention item = GetItemByID(id);
        if (item != null){
            Equip(item);
            return true;
        }
        return false;
    }

    public void InformListeners() {
        for (int i = 0; i < listeners.size(); ++i) {
            PlayerListener pl = listeners.get(i);
            if (!IsAlive())
                pl.OnPlayerDied(this);
        }
    }

    public void SaveLog(EList<LogType> filterToSkip, String folder) {
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
                file.write((l + "\n").getBytes());
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
    public float AggregateDefenseBonus() {
        return BaseDefense() * (1 + 0.12f * GetEquipped(ParryBonus)); // Add parry bonus to reflect total defense.
    }

    public void AssignResourcesBasedOnDifficulty() {
        int diff = (int) Get(Config.Difficulty); // from 0 easiest to 5 Wipeout.
        int multiplier = 6 - diff;
        Set(Stat.MAX_HP, Stat.MAX_HP.defaultValue + 2 * BonusFromDifficulty());
        Set(Stat.FOOD, Stat.FOOD.defaultValue * multiplier);
        Set(Stat.MATERIALS, Stat.MATERIALS.defaultValue * multiplier);
        Set(Stat.BASE_ATTACK, Stat.BASE_ATTACK.defaultValue + multiplier);
        Set(Stat.BASE_DEFENSE, Stat.BASE_ATTACK.defaultValue + multiplier);
        // Heal HP.
        Set(Stat.HP, Get(Stat.MAX_HP));
    }

    /// Just 6 - Difficulty.
    public int BonusFromDifficulty() {
        return (int) (6 - Get(Config.Difficulty));
    }

    public void ReviveRestart() {
        // Keep name, email, password, starting bonus, difficulty,
        log = new EList<Log>(); // Clear the log though?
        SetDefaultStats();        // reset the rest.
        AssignResourcesBasedOnDifficulty();        // Grant the starting bonus again.
        AssignResourcesBasedOnStartingBonus(); // Assign starting bonus.
    }

    // Random item from the inventory, can literally be anything.
    public Invention RandomItem() {
        if (cd.inventions.size() == 0)
            return null;
        return cd.inventions.get(r.nextInt(cd.inventions.size()) % cd.inventions.size());
    }
    // o-o
    public void MarkLogMessagesAsReadByClient() {
        for (int i = 0; i < log.size(); ++i)
            log.get(i).displayedToEndUser = 1;
    }

    public void UpdateTransportMinutes(EList<TransportOccurrence> transportOccurrences) {
        // Clear the array.
        for (int i = 0; i < transports.size(); ++i)
            transports.get(i).secondsUsed = 0;
        for (int i = 0; i < transportOccurrences.size(); ++i){
            TransportOccurrence to = transportOccurrences.get(i);
            System.out.println(to.transport.name()+" dur: "+to.DurationSeconds()+"s ratio: "+to.ratioUsed);
            long durS = to.DurationSeconds();
            Get(to.transport).secondsUsed += durS;
        }
    }

    private Transport Get(TransportType transport) {
        for (int i = 0; i < transports.size(); ++i){
            if (transports.get(i).tt.ordinal() == transport.ordinal())
                return transports.get(i);
        }
        return null;
    }

    // Prints the transports in the order of highest occurrence.
    public void PrintTopTransports(int topNum) {
        System.out.println("Top "+topNum+":");
        EList<Transport> sorted = TransportsSortedBySeconds();
        long totalTimeSeconds = TotalTransportSeconds();
        for (int i = 0; i < topNum; ++i){
            Transport highest = sorted.get(i);
            System.out.println(highest.tt.name()+" "+highest.secondsUsed/(float)totalTimeSeconds+"%, "+highest.secondsUsed+"s");
        }
    }

    private long TotalTransportSeconds() {
        long tot = 0;
        for (int i = 0; i < transports.size(); ++i){
            tot += transports.get(i).secondsUsed;
        }
        return tot;
    }

    private EList<Transport> TransportsSortedBySeconds() {
        /// Update list before returning it?
        EList<Transport> newList = new EList<>();
        while(newList.size() != transports.size()) {
            Transport longest = null;
            for (int i = 0; i < transports.size(); ++i) {
                Transport t = transports.get(i);
                if (newList.contains(t))
                    continue;
                if (longest == null)
                    longest = t;
                else if (t.secondsUsed > longest.secondsUsed)
                    longest = t;
            }
            newList.add(longest);
        }
        return newList;
    }

    // Returns e.g. "Idle: 3252s, Foot: 1235s, Car: 123s"
    public String TopTransportsAsString(int topNum){
        System.out.println("Top "+topNum+":");
        EList<Transport> to = TransportsSortedBySeconds();
        float totalTimeSeconds = TotalTransportSeconds();
        String s = "";
        for (int i = 0; i < topNum; ++i){
            Transport highest = to.get(i);;
            String formatedPercentage = String.format(Locale.ENGLISH, "%.2f", highest.secondsUsed/totalTimeSeconds);
            s += ""+highest.tt.name()+": "+formatedPercentage+"%/ "+highest.secondsUsed+"s";
            if (i < topNum - 1)
                s += ", ";
        }
        return s;
    }

    private long TotalMinutes(EList<TransportOccurrence> to) {
        return TotalSeconds(to) / 60;
    }
    private long TotalSeconds(EList<TransportOccurrence> to){
        long secs = 0;
        for (int i = 0; i < to.size(); ++i)
            secs += to.get(i).DurationSeconds();
        return secs;
    }

    private TransportOccurrence HighestOf(EList<TransportOccurrence> to) {
        TransportOccurrence highest = to.get(0);
        for (int i = 1; i < to.size(); ++i){
            if (to.get(i).DurationSeconds() > highest.DurationSeconds())
                highest = to.get(i);
        }
        return highest;
    }

    public void UpdateLogMessages(EList<Log> messages) {
        EList<Log> newLog = new EList<>();
        EList<Long> logIDs = new EList<>(),
            logIDs2 = new EList<>();
        for (int i = 0; i < log.size(); ++i){ // Check IDs of all
            logIDs.add(log.get(i).LogID());
        }
        for (int i = 0; i < messages.size(); ++i){ // IDs of all.
            logIDs2.add(messages.get(i).LogID());
        }
        long lastID = log.get(log.size() - 1).LogID();
        long lastID2 = messages.get(messages.size() - 1).LogID();
        long largest = lastID > lastID2? lastID : lastID2;
        for (long i = 0; i < largest; ++i){
            if (logIDs2.contains(i))
                newLog.add(GetLogByID(messages, i));
            if (logIDs.contains(i))
                newLog.add(GetLogByID(log, i));
        }
        log = newLog; // Done and sorted.
    }

    private Log GetLogByID(EList<Log> logs, long id) {
        for (int i = 0; i < logs.size(); ++i){
            Log l = logs.get(i);
            if (l.LogID() == id)
                return l;
        }
        return null;
    }

    public EList<Log> LogSublist(int startIndex, int endIndexInclusive, long oldestIDtoInclude) {
        if (log.size() == 0)
            return log;
        if (startIndex < 0)
            startIndex = 0;
        if (startIndex >= log.size())
            startIndex = log.size() - 1;
        if (endIndexInclusive < 0)
            endIndexInclusive = 0;
        if (endIndexInclusive >= log.size())
            endIndexInclusive = log.size() - 1;

        EList<Log> partList = log.subList(startIndex, endIndexInclusive);
        for (int i = 0; i < partList.size(); ++i){ // Filter based on oldest ID.
            Log l = partList.get(i);
            if (l.LogID() < oldestIDtoInclude) {
                partList.remove(l);
                --i;
            };
        }
        for (int i = 0; i < partList.size(); ++i){
            System.out.println(partList.get(i));
        }
        return partList;
    }

    public void ClearLog() {
        log = new EList<Log>();
    }

    public enum StartingBonus{
        FoodSupply("Food supply"),
        MaterialsSupply("Materials supply"),
        Weapon("A Weapon"),
        Armor("A body armor"),
        Tool("A tool"),
        Inventions("2 inventions"),
        ;
        StartingBonus(String text){
            this.text = text;
        }
        String text;
    }

    public void AssignResourcesBasedOnStartingBonus() {
        int startingBonusIndex = (int) Get(Config.StartingBonus);
        if (startingBonusIndex < 0 || startingBonusIndex >= StartingBonus.values().length){
            System.out.println("Bad.");
            LogInfo(LogTextID.startingBonusNone);
            return;
        }
        StartingBonus sb = StartingBonus.values()[startingBonusIndex];
        switch (sb){
            case FoodSupply:
                LogInfo(LogTextID.startingBonusFood, ""+20);
                Adjust(Stat.FOOD, 20);
                break;
            case MaterialsSupply:
                LogInfo(LogTextID.startingBonusMaterials, ""+10);
                Adjust(Stat.MATERIALS, 10);
                break;
            case Weapon:
                Invention randomWeapon = Invention.RandomWeapon(BonusFromDifficulty()/3);
                LogInfo(LogTextID.startingBonusItem, randomWeapon.name);
                cd.inventory.add(randomWeapon);
                Equip(randomWeapon); // Equip it from start?
                break;
            case Armor:
                Invention armor = Invention.RandomArmor(BonusFromDifficulty()/3);
                LogInfo(LogTextID.startingBonusItem, armor.name);
                cd.inventory.add(armor);
                Equip(armor); // Equip it from start?
                break;
            case Tool:
                Invention tool = Invention.RandomTool(BonusFromDifficulty()/3);
                LogInfo(LogTextID.startingBonusItem, tool.name);
                cd.inventory.add(tool);
                Equip(tool);
                break;
            case Inventions:
                LogInfo(LogTextID.startingBonusInventions, ""+2);
                cd.inventions.add(Invention.Random(BonusFromDifficulty()/3));
                cd.inventions.add(Invention.Random(BonusFromDifficulty()/3));
                break;
        }
    }
}
