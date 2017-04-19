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
import erenik.evergreen.GameID;
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
import erenik.util.Printer;
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

import static erenik.evergreen.common.Invention.InventionStat.CraftingBonus;
import static erenik.evergreen.common.Invention.InventionStat.Equipped;
import static erenik.evergreen.common.Invention.InventionStat.ForagingBonus;
import static erenik.evergreen.common.Invention.InventionStat.InventingBonus;
import static erenik.evergreen.common.Invention.InventionStat.MaterialCost;
import static erenik.evergreen.common.Invention.InventionStat.ParryBonus;
import static erenik.evergreen.common.Invention.InventionStat.QualityLevel;
import static erenik.evergreen.common.Invention.InventionStat.ScoutingBonus;
import static erenik.evergreen.common.Invention.InventionStat.StealthBonus;
import static erenik.evergreen.common.Invention.InventionStat.ToRecyle;
import static erenik.evergreen.common.logging.LogTextID.wheelOmen2;

/**
 * Created by Emil on 2016-10-25.
 */
public class Player extends Combatable implements Serializable {
    private static final long serialVersionUID = 1L;

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

    // Used on server-only for specific characters (such as Erenik).
    public AI ai = null;

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
        logTypesToFilter = DefaultLogTypesToFilter();
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

    public boolean playEvents = false;
    /// Increment every passing day. Stop once dying.
    public int TurnsSurvived() {
        return (int) Math.round(Get(Stat.TurnSurvived));
    };
    /// Temporary/chaning starving modifier, changes each turn. Default 1, lower when starving.
    float t_starvingModifier = 1;
    float t_materialModifier = 1; // Same as starving, lowers when negative (debt) on action requirements of materials.

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
    /// To increase bonuses/chance of invention if failing a lot in series - save this across sessions?
    private int successiveInventingAttempts = 0;
    private int successiveCraftingAttempts = 0;
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


    public int TotalEmissions(){
        return (int) (Get(Stat.AccumulatedEmissions) + Get(Stat.InheritedEmissions));
    }

    // Auto-created. Start-using whenever.
    public EList<LogType> logTypesToFilter = DefaultLogTypesToFilter();
    public static EList<LogType> DefaultLogTypesToFilter(){
        return new EList<>();
//        return new EList<LogType>(LogType.values());
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
        if (name.length() > 30){ // Truncate name length if not already done.
            name = name.substring(0, 30);
        }

        // Printer.out("Player writeObject");
        int version = VERSION_4_CREDENTIALS_ONLY; // 0 - Added active actions. 1- email
        out.writeInt(version);
        out.writeInt(gameID);
        out.writeInt(sendAll);

        if (sendAll == CREDENTIALS_ONLY && gameID != GameID.LocalGame){             // Then send only the credentials..
            out.writeObject(name);
            out.writeObject(email);
            out.writeObject(password);
            return;
        }

        out.writeObject(sysmsg);
        out.writeObject(name);
//        Printer.out("name: "+name);
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
        out.writeObject(logTypesToFilter);

        out.writeBoolean(isAI);
        out.writeLong(lastEditSystemMs);
        out.writeLong(lastSaveTimeSystemMs);
    }

    public boolean readFrom(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//        Printer.out("Player start");
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
                if (name.length() > 30){ // Truncate name length if not already done.
                    name = name.substring(0, 30);
                }

                email = (String) in.readObject();
                password = (String) in.readObject();
                if (gameID == GameID.LocalGame) {
                    Printer.out("Player read - Credentials only - in local game....?");
                    new Exception("Player read with only credentials in local game").printStackTrace();
                    return false;
                }
                return true;
            }
          //  Printer.out("Player read - All");

            if (version >= VERSION_2_SYSMSG)
                sysmsg = (String) in.readObject();
            name = (String) in.readObject();
            if (name.length() > 30){ // Truncate name length if not already done.
                name = name.substring(0, 30);
            }

            //        Printer.out("name: "+name);
            email = (String) in.readObject();
            password = (String) in.readObject();

            int numTransports = in.readInt();
            transports.clear();
            for (int i = 0; i < numTransports; ++i){
                Transport t = Transport.readFrom(in);
                if (t != null)
                    transports.add(t);
            }

//            Printer.out("Player read - before log");

            if (!cd.readFrom(in)){
                Printer.out("Failed to read client-data.");
                return false;
            }
            for (int i = 0; i < cd.knownPlayerNames.size(); ++i){ // Remove self from known players if it exists there.
                String name = cd.knownPlayerNames.get(i);
                if (name.equalsIgnoreCase(name)) {
                    cd.knownPlayerNames.remove(i);
                    --i;
                }
            }


            readLogs(in);

//            Printer.out("Player read - after log");
            inventionCurrentlyBeingCrafted = (Invention) in.readObject();

            knownStrongholds = (EList<String>) in.readObject();
            logTypesToFilter = (EList<LogType>) in.readObject();

            isAI = in.readBoolean();
            lastEditSystemMs = in.readLong();
            lastSaveTimeSystemMs = in.readLong();
            if (log == null)
                log = new EList<>();
            if (logTypesToFilter == null || logTypesToFilter.size() == 0)
                logTypesToFilter = DefaultLogTypesToFilter();
        }catch (ClassCastException e){
            Printer.out("Tis bad.");
            e.printStackTrace();
            return false;
        }
//        Printer.out("Init from readOBject!");
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
                Printer.out("Client seen messages only: "+clientSeen.size()+" / "+log.size());
                out.writeObject(clientSeen);
                break;
            case SEND_SERVER_NEW_MESSAGES:
                EList<Log> serverConfirmed = new EList<>();
                int startIndex = log.size() > 100? log.size() - 100 : 0; // Send at most 100 messages?
                for (int i = startIndex; i < log.size(); ++i){
                    Log l = log.get(i);
//                    Printer.out(i+" Displayed to end user: "+l.displayedToEndUser);
                    if (l.displayedToEndUser == 0)
                        serverConfirmed.add(l);
                }
                Printer.out("Server new messages only: "+serverConfirmed.size()+" / "+log.size());
                out.writeObject(serverConfirmed);
                break;
            case SEND_MESSAGES_SINCE_LAST_NEW_DAY:
                EList<Log> sinceLastDay = new EList<>();
                int indexOfLastNewDay = GetLastNewDayLogIndex();
                for (int i = indexOfLastNewDay; i < log.size(); ++i){
                    Log l = log.get(i);
                    sinceLastDay.add(l);
                }
                Printer.out("Since last-day messages only: "+sinceLastDay.size()+" / "+log.size());
                out.writeObject(sinceLastDay);
                break;
            case SEND_ALL:
                out.writeObject(log);
                break;
            default:
                Printer.out("SHOULDN'T BE HERE, Player write object.");
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
                //          Printer.out("log messages read/received: "+logMessagesRead);
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

    public float HP(){
        return Get(Stat.HP);
    }

    // This should be used instead of Get(Stat.MAX_HP) ?
    public int MaxHP() {
        int maxHP = (int) cd.statArr[Stat.MAX_HP.ordinal()];
        for (int i = 0; i < Get(SkillType.Survival).Level(); ++i)
            maxHP += i + 1; // 1/3/6/10/15,  +1,2,3,4,5 increases
        return  maxHP;
    }
    /// Since the different stats are used differently.
    public boolean IsAliveOutsideCombat(){
        return GetInt(Stat.HP) > 0;
    }
    /// Since the different stats are used differently.
    public boolean IsAliveInCombat() {
        return hp > 0;
    }
    int Speed() {
        return GetInt(Stat.SPEED);
    }
    public int UnarmedCombatBonus() {
        return GetEquippedWeapon() == null? Get(SkillType.UnarmedCombat).Level() : 0;
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
        int att = GetInt(Stat.BASE_ATTACK);
        att += UnarmedCombatBonus(); // Max +9 Attack
        att += GetEquipped(InventionStat.AttackBonus);
        att += GetEquippedWeapon() != null ?  (Get(SkillType.WeaponizedCombat).Level() + 1) / 2 : 0;
        return att;
    }
    public int OnTransportAttack() {
        return (int) (BaseAttack() + Get(TransportStat.SocialSupport));
    }
    // Returns the weighted stat of transport bonuses.
    static int attemptsToGetTransportStat = 0;
    public float Get(TransportStat stat) {
        long totalSeconds = GetWeightedTransportSeconds();
        if (totalSeconds == 0){
            Printer.out("Total seconds 0! Generating random transport data and using it instead.");
            // Use default values? Randmize?
            RandomizeGenerateTransportUsageData();
            ++attemptsToGetTransportStat;
            if (attemptsToGetTransportStat > 3){
                return 1;
            }
            return Get(stat);
//            return 0;
            // Randomize it?
           // totalSeconds = GetWeightedTransportSeconds();
            // totalSeconds =
        }
     //   Printer.out("totalSeconds: "+totalSeconds);
        float totalWeighted = 0;
     //   System.out.print("Ratios: ");
        for (int i = 0; i < transports.size(); ++i){
            Transport t = transports.get(i);
            float ratio = t.secondsUsed / (float)totalSeconds * t.Get(TransportStat.Weight);
        //    System.out.print(String.format(Locale.ENGLISH, "%.2f", ratio)+", ");
            totalWeighted += ratio * t.Get(stat);
        }
      //  Printer.out();
   //     Printer.out("Total weighted "+stat.name()+": "+totalWeighted);
        return totalWeighted;
    }

    private void GenerateEventsBasedOnTransportsUsed() {
        // Get sorted.
        EList<Transport> sorted = TransportsSortedBySeconds();
        long idleSeconds = 0;
        for (int i = 0; i < sorted.size(); ++i){
            // Probably Idle first.
            Transport transport = sorted.get(i);
            switch (transport.tt){
                case Idle:
                    idleSeconds = transport.secondsUsed;
                    continue;
                default:
                    if (transport.secondsUsed < 300 || transport.secondsUsed < idleSeconds * 0.03f) // Needs to be at least 5 minutes and at least 3% or..
                        continue; // ..larger compared to the Idle time to be worth considering?
            }
            if (Dice.RollD6(2) < 8) // Random each day, don't random event all the time! 2-12, 8+ to get an event, assuming you have the required seconds or ratio mentioned above.
                continue;
            int quantityDiv300 = (int) (transport.secondsUsed / 300); // In general, 1 per 300 seconds detected?
            int quantityDiv600 = (int) (transport.secondsUsed / 600);
            if (quantityDiv600 < 1)
                quantityDiv600 = 1;
            String transportString = "";
            switch (transport.tt){
                case Foot:
                    transportString = "walking";
                    // Some chance for event.
                    switch (Dice.RollD6(1)){
                        default:
                        case 1: TransportStumbledUponSome(transportString, Finding.Food, quantityDiv300); break;
                        case 2: TransportStumbledUponSome(transportString, Finding.Materials, quantityDiv300); break;
                        case 6: TransportStumbledUponSome(transportString, Finding.RandomItem, quantityDiv300); break;
                    }
                    break;
                case Bike:
                    transportString = "biking";
                    switch (Dice.RollD6(1)){
                        case 1: TransportStumbledUponSome(transportString, Finding.Food, quantityDiv300); break;
                        default:  TransportStumbledUponSome(transportString, Finding.Materials, quantityDiv300); break;
                        case 6: TransportStumbledUponSome(transportString, Finding.RandomItem, quantityDiv300); break;
                    }
                    break;
                case Car:
                    transportString = "on the streets";
                    switch (Dice.RollD6(1)){
                        case 1: TransportStumbledUponSome(transportString, Finding.Materials, quantityDiv600); break;
                        case 2: TransportStumbledUponSome(transportString, Finding.WheelAccident, quantityDiv600); break;
                        default: TransportStumbledUponSome(transportString, Finding.WheelOmen, quantityDiv300); break;
                        case 5:
                        case 6: // Nothing happens.
                    }
                    break;
                case Bus:
                    transportString = "on the streets";
                    switch (Dice.RollD6(1)){
                        case 2:
                        case 3: break; // Nothing happens.
                        case 1: TransportStumbledUponSome(transportString, Finding.Materials, quantityDiv600); break;
                        default: TransportStumbledUponSome(transportString, Finding.WheelOmen, quantityDiv300); break;
                    }
                    break;
                // No events for now?
                case Train:
                case Tram:
                case Subway:
                    transportString = "along the track";
                    // Some chance for event.
                    switch (Dice.RollD6(3)){
                        case 1: TransportStumbledUponSome(transportString, Finding.Food, quantityDiv600); break;
                        case 3:
                        case 2: TransportStumbledUponSome(transportString, Finding.Materials, quantityDiv600); break;
                        case 6: TransportStumbledUponSome(transportString, Finding.RandomItem, quantityDiv600); break;
                        default:
                            // Nothing happens, or make a new rail-specific event later.
                    }
                    break;
                case Plane:
                    transportString = "flying";
                    switch (Dice.RollD3(3)){
                        case 1:
                            break;
                        default:
                            TransportStumbledUponSome(transportString, Finding.FlyingOmen, quantityDiv300);
                            break;
                    }
                    break;
                case Boat:
                    // Nothing for now?
                    break;
                default:
                    Printer.out("Forgot to add an event, hm...");
                    new Exception().printStackTrace();
                    break;
            }
        }
       // for (int i = 0; i < )
    }

    int ClampInt(int value, int min, int max){
        if (value > max)
            return max;
        if (value < min)
            return min;
        return value;
    }

    void TransportStumbledUponSome(String transportString, Finding find, int quantity){
        switch (find) {
            case Food:
                int foodQuantity = Dice.RollD3(ClampInt(quantity, 1, 3));
                Adjust(Stat.FOOD, foodQuantity);
                LogInfo(LogTextID.stumbledUponFood, transportString, "" + foodQuantity);
                break;
            case Materials:
                int materialsQuantity = Dice.RollD3(ClampInt(quantity, 1, 3));
                Adjust(Stat.MATERIALS, materialsQuantity);
                LogInfo(LogTextID.stumbledUponMaterials, transportString, "" + materialsQuantity);
                break;
            case RandomItem:
                int quality = ClampInt(quantity / 2, 0, 3);
                Invention item = Invention.Random(quality); // Need to walk or bike a lot to get this one good!
                LogInfo(LogTextID.stumbledUponItem, transportString, item.name);
                cd.inventory.add(item);
                break;
            case WheelAccident:
                int diceDamageToTake = ClampInt(quantity, 1, 2);
                int damage = Dice.RollD3(diceDamageToTake);
                Adjust(Stat.HP, -damage);
                Log(LogTextID.wheelAccident, LogType.OtherDamage, "" + damage);
                break;
            case WheelOmen:{
                // Random incident/story.
                int dice = ClampInt(quantity, 1, 3);
                switch (Dice.RollD3(1)) {
                    default:
                        Log(LogTextID.wheelOmen1, LogType.INFO);
                        Adjust(Stat.AccumulatedEmissions, Dice.RollD3(dice));
                        break;
                    case 1:
                        Log(LogTextID.wheelOmen2, LogType.INFO);
                        Adjust(Stat.AccumulatedEmissions, Dice.RollD3(dice + 1));
                        break;
                    case 2:
                        Log(LogTextID.wheelOmen3, LogType.INFO);
                        Adjust(Stat.AccumulatedEmissions, Dice.RollD3(dice + 2));
                        break;
                }
                break;
            }
            case FlyingOmen: {
                int dice = ClampInt(quantity, 2, 6); // 2d3 to 6d3 based on detected duration (1d3 for each 300 seconds analyzed.
                switch (Dice.RollD3(1)){
                    default:
                        Log(LogTextID.flyingOmen1, LogType.INFO);
                        Adjust(Stat.AccumulatedEmissions, Dice.RollD3(dice));
                        break;
                    case 2:
                        Log(LogTextID.flyingOmen2, LogType.INFO);
                        Adjust(Stat.AccumulatedEmissions, Dice.RollD3(dice));
                        break;
                }
                break;
            }
            default:
                Printer.out("Not implemented D:");
                new Exception().printStackTrace();
                break;
        }
    }

    private long GetWeightedTransportSeconds() {
        long secondsUsed = 0;
        for (int i = 0; i < transports.size(); ++i){
            Transport t = transports.get(i);
            secondsUsed += t.secondsUsed * t.Get(TransportStat.Weight);
        }
        return secondsUsed;
    }

    private long GetTotalTransportSeconds() {
        long secondsUsed = 0;
        for (int i = 0; i < transports.size(); ++i){
            secondsUsed += transports.get(i).secondsUsed;
        }
        return secondsUsed;
    }

    /// Attack when in shelter? + shelter bonus, yo? or? D:
    public int ShelterAttack() {
        return BaseAttack() + GetInt(Stat.SHELTER_DEFENSE) / 2;
    }
    public int ShelterDefense() {
        return BaseDefense() + GetInt(Stat.SHELTER_DEFENSE);
    }
    public int OnTransportDefense() {
        return (int) (BaseDefense() + Get(TransportStat.SocialSupport));
    }
    public int BaseDefense() {
        int def = GetInt(Stat.BASE_DEFENSE);
        def += (UnarmedCombatBonus()-1) / 3;
        def += GetEquipped(InventionStat.DefenseBonus);
        def += Get(SkillType.DefensiveTraining).Level();
        return def;
    }
    public Dice Damage() {
        // Base damage, 1D6, no bonuses.
        Dice damage = new Dice(6, 1, 0);
        damage.bonus += UnarmedCombatBonus()/2; // Max +4 damage
        Invention weapon = GetEquippedWeapon();
        if (weapon != null) { // Weapon equipped.
            damage.diceType = weapon.Get(InventionStat.AttackDamageDiceType);
            damage.dice = weapon.Get(InventionStat.AttackDamageDice);
            damage.bonus = weapon.Get(InventionStat.AttackDamageBonus);
            damage.bonus += Get(SkillType.WeaponizedCombat).Level() / 2;
       //     Printer.out("Damage: "+damage.dice+"D"+damage.diceType+"+"+damage.bonus);
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
        if (cd.inventionBlueprints != null)
            cd.inventionBlueprints.clear();
        transports = Transport.DefaultTransports();
        // Clear both queues.
        cd.dailyActions.clear();
        cd.skillTrainingQueue.clear();
    }

    public void Adjust(Statistic statistic, long amount) {
        cd.statistics[(statistic.ordinal())] += amount;
    }
    public void Adjust(Stat s, float amount) {
        cd.statArr[s.ordinal()] += amount;
        if (s == Stat.HP){
            float value = cd.statArr[s.ordinal()];
            ClampHP();
            if (cd.statArr[s.ordinal()] <= 0) {
                Printer.out("Died, informing listeners");
                for (int i = 0; i < listeners.size(); ++i)
                    listeners.get(i).OnPlayerDied(this);
            }
        }
    }
    // Getter for main stats.
    float Get(int stat)
    {
        return cd.statArr[stat];
    }
    public float Get(Stat s) {
        if (s == Stat.AccumulatedEmissions && cd.statArr[s.ordinal()] < 0) // IF negative emissions, correct it here.
            cd.statArr[s.ordinal()] = 0;
        if (s == Stat.HP){
            if (cd.statArr[Stat.HP.ordinal()] > MaxHP()) { // Cap HP!!
                cd.statArr[Stat.HP.ordinal()] = MaxHP();
                return MaxHP();
            }
        }
        return cd.statArr[s.ordinal()];
    }
    public int GetInt(Stat s) {
        ClampHP();
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
        if (log.size() > 5000){
            /// Exceeds 5k? then dump old part?
            log = log.subList(log.size() / 2);
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
    public Transport GetCurrentTransport() {
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
//        Printer.out("Turn: " + Get(Stat.TurnPlayed));
    }
    static Random emissionsRand = new Random(System.currentTimeMillis());
    /// Adjusts stats, generates events based on chosen actions to be played, logged
    public void NextDay(Game game) {
        if (GameID() == GameID.LocalGame){
            Log.logIDEnumerator.value = (long) Get(Config.LatestLogMessageIDSeen); // If the player is from a local game, reset the Log static enumerator to the last viewed log message, or the Results-Screen feature will not work as intended.
        }

        updatesFromClient.clear(); // Clear this array when new days are processed.
        if (GetInt(Stat.HP) <= 0) {
            // TODO: Add a listener-callback mechanism for when the player dies.
            //            App.GameOver();
            return;
        }
        NewDay();  // New day :3
        LogInfo(LogTextID.newDayPlayerTurnPlayed, ""+(int) Get(Stat.TurnPlayed));
  //      Transport t = Transport.HighestOf(transports);
       // Printer.out("Randomed transport.. "+t.name());
    //    Set(Stat.CurrentTransport, t.tt.ordinal());

     //   CalcAggregateTransportBonuses();         // Calculate the general transport bonus granted from the previous day?
        GenerateEventsBasedOnTransportsUsed();         // Or just give bonuses and random events based on the distances traveled etc?

        // Bonuses for greening habits?
//        LogInfo(LogTextID.transportOfTheDay, GetCurrentTransport().tt.name());


        float emissionsToGenerate = Get(TransportStat.EmissionsPerDay);
        emissionsToGenerate *= emissionsRand.nextFloat() * 0.5f + 0.75f;// Add some randomness to emissions-generation? +/-25%
        int iEm = Math.round(emissionsToGenerate);         //if (emissionsToGenerate > 0) // Really print this? Better have it more secret? and more random feeling?
        if (iEm > 0)
            GenerateEmissions(iEm);         // Log("Generated "+Stringify(emissionsToGenerate)+" units of emissions.", LogType.INFO);
        else {
            Adjust(Statistic.TotalEmissionsReduced, -iEm);
            Adjust(Stat.AccumulatedEmissions, iEm);
        }
        if (Get(Stat.InheritedEmissions) > 0) {         // Decrement inherited emissions if any.
            float oldValue = Get(Stat.InheritedEmissions);
            float newValue = oldValue * 0.9f - 2; // Decrease by 10% per turn + 2.
            if (newValue <= 0)
                newValue = 0;
            Set(Stat.InheritedEmissions, newValue);
        }

        // Yeah.
        Adjust(Stat.FOOD, -2);
        if (GetInt(Stat.FOOD) >= 0) {
            Adjust(Stat.HP, 1);
            ClampHP();
        }
        else {
            float loss = Get(Stat.FOOD) / 5;
            int lossInt = Math.round(loss);
            if (lossInt < 1)
                lossInt = 1;
            Adjust(Stat.HP, lossInt);
            Log(LogTextID.starvingLostHP, LogType.OtherDamage, ""+lossInt);
            if (!IsAliveOutsideCombat()) {
                Log(LogTextID.diedOfStarvation, LogType.DEFEATED);
                return;
            }
        }
        /// Gain EXP? Spend unspent EXP?
        int expToGain = (int) (2 + Get(SkillType.Studious).Level() + Get(Stat.UNALLOCATED_EXP));
        // Erase unallocated exp. It will be re-added later perhaps.
        Set(Stat.UNALLOCATED_EXP, 0);
 //       Printer.out("Exp gained: "+expToGain);
        GainEXP(expToGain);

        EvaluateDailyActions(game); // Daily actions if any
        ProcessQueuedActiveActions(game); // ActiveActions if any

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
        // Clear queue of daily actions to ensure people actually think what they will do? No?
        if (Get(Config.RetainDailyActionsOnNewTurn) == 0)
            cd.dailyActions.clear();
    }
    // ow, ow! wat....
    public void ReviveIfKnockedOut() {
        Printer.out("Player.ReviveIfKnockedOut");
        Adjust(Stat.Lives, -1);
        if (Get(Stat.Lives) <= 0){ // Fully dead?
            return;
        }
        float lossPerKO = Difficulty.LossRatio((int) Get(Config.Difficulty));
        float newRate = 1 - lossPerKO;
        Set(Stat.FOOD, Get(Stat.FOOD) * newRate); // Reduce food and materials
        Set(Stat.MATERIALS, Get(Stat.MATERIALS) * newRate);  // Reduce food and materials
        for (int i = 0; i < cd.skills.size(); ++i){
            Skill s = cd.skills.get(i);
            int oldLevel = s.Level();
            s.setTotalEXP((int) (s.TotalExp() * newRate)); // Reduce exp in all skills. They can level-down.
            int newLevel = s.Level();
            if (newLevel < oldLevel){
                LogInfo(LogTextID.skillLeveledDown, s.name(), ""+s.Level());
                Adjust(Statistic.SkillLevelDowns, 1);
            }
        }
        /// Adjust max HP after skills have potentially been lowered.
        Set(Stat.HP, MaxHP() * (0.5f + 0.05f * Get(SkillType.Survival).Level())); // Reset HP to 25% of max. + 5% per level in Survival?
        LogInfo(LogTextID.secondLife);
    }

    public void ClampHP() {
        float hp = cd.statArr[Stat.HP.ordinal()];
        if (hp > MaxHP())
            hp = MaxHP();
        else if (hp < 0)
            hp = 0;
        cd.statArr[Stat.HP.ordinal()] = hp;
    }

    String Stringify(float value) {
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
    void EvaluateDailyActions(Game game) {
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
//        Printer.out("hoursPerAction: "+hoursPerAction);
        foodHarvested = 0.0f;
        // Execute at most 8 actions per day, regardless of queue.
        int stealAttempts = 0, attackAttempts = 0;
        for (int i = 0; i < cd.dailyActions.size() && i < MAX_ACTIONS; ++i) {
            /// Parse actions and execute them.
            da = cd.dailyActions.get(i);
            switch (da.DailyAction()){
                case Steal:
                    ++stealAttempts;
                    if (stealAttempts > 1)
                        continue;
                case AttackAPlayer:
                    ++attackAttempts;
                    if (attackAttempts > 1)
                        continue;
            }
            EvaluateAction(da, game);
        }
        if (cd.dailyActions.size() == 0){
            LogInfo(LogTextID.didNothing);
        }
    }

    String formattedFloat(float value, int decimals){
        String format = "%."+decimals+"f"; // e.g. "%.2f"
        String formattedFloat = String.format(Locale.ENGLISH, format, value);
        return formattedFloat;
    }

    void EvaluateAction(Action action, Game game) {
        String playerName = action.GetPlayerName();
        Player player = null;
        if (playerName != null) {
            player = game.GetPlayer(playerName);
            if (player == null){ // If they don't exist at all?
                Log(LogTextID.couldNotFindPlayer, LogType.ACTION_FAILURE, playerName);
                return;
            }
            if (!player.IsAliveOutsideCombat()) { // And if they just died?
                Log(LogTextID.couldNotFindPlayer, LogType.ACTION_FAILURE, playerName); // Don't hint their death?   // LogInfo(LogTextID.playerNotAliveAnymore, playerName);
                return;
            }
        }
        /// THEN ACTIVE ACTIONS
        if (action.ActiveAction() != null) {
//            Printer.out("EvaluationAction - Active action. "+a);
            switch (action.ActiveAction()) {
                default:
                    Printer.out("Unsure how to evaluate action: "+action);
                    new Exception().printStackTrace();
                    System.exit(154);
                    break;
                case GiveItem:
                //    Printer.out("GiveItem");
                    String itemName = action.Get(ActionArgument.Item).trim();
                    Invention item = GetItemByName(itemName);
                    if (player == null) {
                        Printer.out("Player null, name: "+playerName);
                        return;
                    }
                    if (item == null) {
                        Printer.out("Couldn't find item by name: " + itemName);
                        return;
                    }
                    cd.inventory.remove(item); // Do the transfer
                    player.cd.inventory.add(item);
                    item.Set(Equipped, -1); // Unequip it.
                    LogInfo(LogTextID.gaveItemToPlayer, itemName, playerName); // Log it.
                    Adjust(Statistic.ItemsGiven, 1);
                    player.LogInfo(LogTextID.receivedItemFromPlayer, itemName, name);
              //      Printer.out("GiveItem - gave item to player");
                    break;
                case GiveBlueprint:
                    String blueprint = action.Get(ActionArgument.Blueprint).trim();
                    Invention bp = GetBlueprintByName(blueprint);
              //      Printer.out("GiveBlueprint");
                    if (player == null) {
                        Printer.out("Player null, name: " + playerName);
                        return;
                    }
                    if (bp == null){
                        // Printer.out("Couldn't find blueprint by name: "+blueprint);
                        return;
                    }
                    cd.inventionBlueprints.remove(bp);
                    player.cd.inventionBlueprints.add(bp);
                    LogInfo(LogTextID.gaveBlueprintToPlayer, blueprint, playerName);
                    Adjust(Statistic.BlueprintsGiven, 1);
                    player.LogInfo(LogTextID.receivedBlueprintFromPlayer, blueprint, name);
                //    Printer.out("GiveBlueprint - gave blueprint to player");
                    break;
                case GiveResources:
                //    Printer.out("GiveResources");
                    if (player == null) {
                        Printer.out("Player null");
                        return;
                    }
                    Stat s = null;
                    String type = action.Get(ActionArgument.ResourceType).trim();
                    if (type.equals("Food"))
                        s = Stat.FOOD;
                    else if (type.equals("Materials"))
                        s = Stat.MATERIALS;
                    else {
                        Printer.out("Bad resource type");
                        Log(LogTextID.debug, LogType.INFO, "Bad resource type");
                        return;
                    }
                    String quantity = action.Get(ActionArgument.ResourceQuantity);
                    float qu = Float.parseFloat(quantity);
                    float quantAvail = Get(s);
                    long toSend = (long) Math.min(qu, quantAvail);
                    if (toSend < 0) {
                        LogInfo(LogTextID.sendPlayerResourcesFailed, type, playerName);
                        ; // Printer.out("negative quant");
                        return;
                    }
                    Adjust(Statistic.ResourcesGiven, toSend);
                    player.Adjust(s, toSend);
                    player.Adjust(Statistic.ResourcesReceived, toSend);
                    Adjust(s, -toSend);
                    LogInfo(LogTextID.SentPlayerResources, playerName, formattedFloat(toSend, 2), type);
                    player.LogInfo(LogTextID.ReceivedPlayerResources, this.name, formattedFloat(toSend, 2), type);
                    break;
                case SendMessage:
                    if (player == null) {
                        Printer.out("NULL player");
                        return;
                    }
                    Adjust(Statistic.MessagesSent, 1);
                    LogInfo(LogTextID.MessageSentToPlayer, playerName);
                    player.LogInfo(LogTextID.MessageReceivedFromPlayer, name, action.Get(ActionArgument.Text));
                    break;
                case ShareKnownPlayers: {
                    if (player == null){
                        Printer.out("Null player");
                        return;
                    }
                    Adjust(Statistic.PlayerKnowledgeSharings, 1);
                    LogInfo(LogTextID.SharedPlayerKnowledgeWithPlayer, playerName);
                    player.LogInfo(LogTextID.PlayerSharedPlayerKnowledgeWithYou, this.name);
                    int numObtained = 0;
                    EList<String> namesToShare = cd.knownPlayerNames.clone();
                    namesToShare.add(this.name);
                    for (int i = 0; i < namesToShare.size(); ++i){
                        String knownName = namesToShare.get(i);
                        if (!player.KnowsThisPlayer(knownName) // If the player doesn't know this player
                                && !knownName.equalsIgnoreCase(player.name)){ // And it isn't their own name
                            Player playerFound = game.GetPlayer(knownName);
                            if (playerFound == null)
                                continue;
                            player.FoundPlayer(playerFound);
                            ++numObtained;
                        }
                    }

                    if (numObtained == 0){
                        player.LogInfo(LogTextID.AlreadyHasPlayerKnowledge, this.name);
                    }
                    break;
                }
            }
            return;
        }

        if (action.DailyAction() != null)
            switch (action.DailyAction()) {
                case GatherFood: {
                    int totalFoodGathered = 0;
                    float hoursToRandomize = hoursPerAction * t_starvingModifier;
                    // Randomize each hour.
                    for (int i = 0; i < hoursToRandomize; ++i) {
                        int foundSomething = (int) (Dice.RollD6(2) + Get(SkillType.Foraging).Level() + Get(TransportStat.ForagingBonus) + GetEquipped(ForagingBonus)); // Roll 2d6 + Foraging skill, gear bonuses, etc..
                        int diceFound = (foundSomething - 5) / 3;
                        if (diceFound < 1)
                            continue;
                        totalFoodGathered += Dice.RollD2(diceFound);  // 1 to 3 base dice for each occurrence.
                    }
                    if (totalFoodGathered < 2)
                        totalFoodGathered = 1 + Get(SkillType.Foraging).Level(); // Get at least 1.
                    Adjust(Statistic.FoodGathered, totalFoodGathered);
                    Adjust(Stat.FOOD, totalFoodGathered);
                    LogInfo(LogTextID.foragingFood, "" + totalFoodGathered);
                    break;
                }
                case GatherMaterials: {
                    int totalMaterialsGathered = 0;
                    float hoursToRandomize = hoursPerAction * t_starvingModifier;
                    // Randomize each hour.
                    for (int i = 0; i < hoursToRandomize; ++i) {
                        int foundSomething = (int) (Dice.RollD6(2) + Get(SkillType.Foraging).Level() + Get(TransportStat.ForagingBonus) + GetEquipped(ForagingBonus)); // Roll 2d6 + Foraging skill, gear bonuses, etc..
                        int diceFound = (foundSomething - 5) / 3;
                        if (diceFound < 1)
                            continue;
                        totalMaterialsGathered += Dice.RollD2(diceFound);  // 1 to 3 base dice for each occurrence.
                    }
                    if (totalMaterialsGathered < 2)
                        totalMaterialsGathered = 1 + Get(SkillType.Scavenging).Level(); // Get at least 1 + Scavenging skill
                    Adjust(Statistic.MaterialsGathered, totalMaterialsGathered);
                    LogInfo(LogTextID.gatherMaterials, "" + totalMaterialsGathered);
                    Adjust(Stat.MATERIALS, totalMaterialsGathered);
                    break;
                }
                case Scout:
                    Scout(game);
                    break;
                case Recover:
                    float units = (1 + 0.5f * Get(SkillType.Survival).Level()); // 1 + 0.5 for each survival skill.
                    units += GetEquipped(InventionStat.RecoveryBonus); // +2 for First aid kit, for example.
                    units *= 0.5f * hoursPerAction; // Recovery +50/100/150/200/250%
                    units *= t_starvingModifier;
                    Adjust(Stat.HP, units);
                    ClampHP();
                    LogInfo(LogTextID.recoverRecovered, ""+String.format(Locale.ENGLISH, "%.2f", units));
                    break;
                case BuildDefenses: BuildDefenses(); break;
    //            case AugmentTransport: AugmentTransport(); break;
                case LookForPlayer: LookForPlayer(action, game); break;
                case LookForPlayers: LookForPlayers(game); break;
    //            case Expedition: Log("Not implemented yet - Expedition", LogType.ATTACKED); break;
                case Invent: Invent(action); break;
                case Craft: Craft(action); break;
                case Steal: Steal(action, game); break;
                case AttackAPlayer: AttackAPlayer(action, game);
                    break;
                case Study:
                    // Gain exp into current skill perhaps..?
                    int toGain = Dice.RollD3(2) + Get(SkillType.Studious).Level();
                    Log(LogTextID.studiesEXP, LogType.PROGRESS, ""+toGain);
                    GainEXP(toGain);
                    break;
                case ReduceEmissions: ReduceEmissions(); break;
                default:
                    Printer.out("Uncoded daily action");
                    System.exit(15);
            }
    }

    private Invention GetItemByName(String itemName) {
        for (int i = 0; i < cd.inventory.size(); ++i){
            Invention item = cd.inventory.get(i);
            if (item.name.equals(itemName))
                return item;
        }
        return null;
    }
    private Invention GetBlueprintByName(String itemName) {
        for (int i = 0; i < cd.inventionBlueprints.size(); ++i){
            Invention item = cd.inventionBlueprints.get(i);
            if (item.name.equals(itemName))
                return item;
        }
        return null;
    }

    private void ReduceEmissions() {
        int successful = 0;
        float amount = 0;
        int toRandom = Math.round(hoursPerAction) + Get(SkillType.Recycling).Level(); // Hours + Recycling skill # of random-times.
        for (int i = 0; i < toRandom; ++i) {
            int diceRoll = r.nextInt(100) + Get(SkillType.Recycling).Level(); // Increase emission reductions with the recycling skill.
            if (diceRoll > 50){
                amount += Dice.RollD3(1);
                ++successful;
            }
        }
        if (successful > hoursPerAction * 0.75) {
            amount += Get(Stat.AccumulatedEmissions) * 0.25f; // Successful, get bonus 25% of total.
        }
        else if (successful > hoursPerAction * 0.5) {
            amount += Get(Stat.AccumulatedEmissions) * 0.1f; // Successful, get bonus 25% of total.
        }
        else if (successful > hoursPerAction * 0.25) {
            amount += Get(Stat.AccumulatedEmissions) * 0.05f; // Successful, get bonus 25% of total.
        }
        amount *= t_starvingModifier;
        float cap = Get(Stat.AccumulatedEmissions) * 0.5f;
        if (amount > cap)
            amount = cap; // Cap it at 50% of the total?
        if (amount < 1)
            amount = 1;

        Log(LogTextID.reduceEmissionsSuccessful, LogType.SUCCESS, Stringify(amount));
//        Log(LogTextID.reduceEmissionsMostlySuccessful, LogType.SUCCESS, Stringify(amount));
  //      Log(LogTextID.reduceEmissionsNotSoSuccessful, LogType.SUCCESS, Stringify(amount));
    //    Log(LogTextID.reduceEmissionsFailed, LogType.SUCCESS, Stringify(amount));
        Adjust(Statistic.TotalEmissionsReduced, (long) amount);
        Adjust(Stat.AccumulatedEmissions, -amount);
        if (Get(Stat.AccumulatedEmissions) < 0) {
            Set(Stat.AccumulatedEmissions, 0);
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
        int roll = Dice.RollD6(2 + Get(SkillType.Thief).Level() + GetEquipped(StealthBonus)); // Roll 2D6 + D6s for Thief skill, + D6s from bonuses from gear.
        if (roll < 7) { // Failed and detected.
            Log(LogTextID.stealFailedDetected, LogType.ATTACK_MISS, targetPlayerName);
            p.Log(LogTextID.playerTriedToStealFromYouFailedDetected, LogType.ATTACKED, name);
            /// Add stealing player into target player's list of known players, so that they may retaliate?
            p.FoundPlayer(p);

//            Log("While trying to steal from "+targetPlayerName+" you were detected! You didn't manage to steal anything.", LogType.ATTACK_MISS);
//            p.Log(""+name+" tried to steal from you, but failed as he was detected!", LogType.ATTACKED);
            return;
        }
        if (roll < 9) {
            Log(LogTextID.stealFailed, LogType.ATTACK_MISS, targetPlayerName);
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
        Log(LogTextID.stealSuccess_whatName, LogType.ATTACK, formattedFloat(quantity, 2)+" units of "+stolenStat.name(), targetPlayerName);
        p.Log(LogTextID.stolen, LogType.ATTACKED, formattedFloat(quantity, 2)+" units of "+stolenStat.name());
//        Log("Stole "+whatStolen+" from "+p.name+"!", LogType.ATTACK);
//        p.Log("Player "+name+" stole "+whatStolen+" from you!", LogType.ATTACKED);
    }

    private void LookForPlayers(Game game) {
        int searchChances = (int) (hoursPerAction * (1 + 0.5f * Speed()) + 0.25f * GetEquipped(ScoutingBonus)); // Attempts per hour?
        EList<Player> playersFound = new EList<>();
        int failedAttempts = 0;
        for (int i = 0; i < searchChances; ++i) {
            int randInt = r.nextInt(100);
            if (randInt > 90 - failedAttempts) { // 10% chance at each iteration, +1% or each failed attempt until success - reset to 10%.
                failedAttempts = 0;
                Player randomPlayer = game.RandomLivingPlayer(KnownNamesSelfIncluded());
                if (randomPlayer == null)
                    return;
                String newPlayer = randomPlayer.name;
                if (newPlayer != null && newPlayer != name) {
                    if (KnowsThisPlayer(newPlayer))
                        return; // Skip it then.
                    playersFound.add(randomPlayer);
                    //                    knownPlayerNames.add(newPlayer);
                }
            }
            else
                ++failedAttempts;

        }
        if (playersFound.size() > 0){
            Log(LogTextID.searchPlayers_success, LogType.SUCCESS, playersFound.size()+"");
            for (int i = 0; i < playersFound.size(); ++i){
                FoundPlayer(playersFound.get(i));
            }
        }
        else
            Log(LogTextID.searchPlayers_failed, LogType.ACTION_NO_PROGRESS);
        //        Log(LogTextID.debug, LogType.INFO, "knownNAmes: "+cd.knownPlayerNames);


    }

    void LookForPlayer(Action da, Game game) {
        String name = this.da.requiredArguments.get(0).value;
        name = name.trim();
        Player player = null;
        int searchBonus = (int) (GetEquipped(ScoutingBonus) + Get(TransportStat.SpeedBonus) + Get(Stat.SPEED));
        // Try search for players now then...
        for (int i = 0; i < hoursPerAction + 1 + searchBonus; ++i) {
            if (r.nextInt(100) > 80) {
                player = game.GetPlayer(name);
            }
        }
        if (KnowsThisPlayer(name)){
            LogInfo(LogTextID.searchPlayer_alreadyFound, name);
            return;
        }
        if (player == null)
            player = game.GetPlayer(name, false, true);
        if (player == null)
            player = game.GetPlayer(name, true, false);
        if (player == null) {
            int randInt = r.nextInt(100 + GetEquipped(ScoutingBonus));
            Log(LogTextID.searchPlayerFailed, LogType.ACTION_FAILURE, name);
//            Log("Despite searching, you were unable to find a player called "+name+".", LogType.ACTION_FAILURE);
            // Add chance to find random other players?
            if (randInt < 50) {
                Player randomPlayer = game.RandomLivingPlayer(KnownNamesSelfIncluded());
                if (randomPlayer == this) // Return if self, no need to imply stupid in vain
                    return;
                if (randomPlayer == null)
                    return;
                String newPlayer = randomPlayer.name;
                if (newPlayer != null && // IF the player is valid
                        !newPlayer.equalsIgnoreCase(name)) { // And it is NOT your own name...
                    if (KnowsThisPlayer(newPlayer))
                        return; // Skip it then.
                    Log(LogTextID.searchPlayer_foundAnother, LogType.SUCCESS);
                    player = randomPlayer;
//                    knownPlayerNames.add(newPlayer);
                }
                else // In-case you already know all the players in the game already.
                    return;
            }
            else
                return;
        }
        if (player != null)
            FoundPlayer(player);
//        Log(LogTextID.debug, LogType.INFO, "knownNAmes: "+cd.knownPlayerNames);
    }

    // Returns true if THIS player knows target player.
    boolean KnowsThisPlayer(String playerName){
        for (int i = 0; i < cd.knownPlayerNames.size(); ++i){
            String n = cd.knownPlayerNames.get(i);
            if (n.equalsIgnoreCase(playerName))
                return true; // Already know this player. o-o
        }
        return false;
    }

    private void FoundPlayer(Player player) {
        if (player.IsAliveOutsideCombat() == false)
            return; // Lolno wat.
        if (player == this) // Just remove the stupid message of finding yourself... just causing more bugs than benefit right now.
            return;
        String playerName = player.name;
        if (cd.knownPlayerNames == null)
            cd.knownPlayerNames = new EList<String>();
        if (KnowsThisPlayer(playerName))
            return;
        if (playerName.equalsIgnoreCase(this.name)){
            Log(LogTextID.foundPlayerSelfLol, LogType.SUCCESS, playerName);
            return;
        }
        Log(LogTextID.foundPlayer, LogType.SUCCESS, playerName);
//        Log("You found the player named "+name+"! You can now interact with that player.", LogType.SUCCESS);
        cd.knownPlayerNames.add(playerName);
        Adjust(Statistic.PlayerDiscoveries, 1);
    }

    private EList<String> KnownNamesSelfIncluded() {
        EList<String> knownNames = new EList<>();
        if (cd.knownPlayerNames == null)
            cd.knownPlayerNames = new EList<String>();
        for (int i = 0; i < cd.knownPlayerNames.size(); ++i)
            knownNames.add(cd.knownPlayerNames.get(i));
        knownNames.add(name);
        return knownNames;
    }

    private void Craft(Action da) {
        float emit = ConsumeMaterials(1.f + hoursPerAction);
        // How many times to random.
        float toRandom = 0.5f + hoursPerAction; // Roll once for each hour?
        toRandom *= t_starvingModifier;
        toRandom *= CalcMaterialModifier();
        toRandom *= 1 + 0.5f * Get(SkillType.Crafting).Level() + 0.25f * GetEquipped(CraftingBonus); // Increase random chances by 50% for each level in crafting.
        String s = da.text+": ";
        // Check if inveting has been queued for any special item?
        if (da.requiredArguments.size() == 0) {
            Printer.out("required argument in Player.Craft");
            System.exit(14);
        }
        String whatToCraft = da.requiredArguments.get(0).value;
        whatToCraft = whatToCraft.trim();
        Invention toCraft = null;
        toCraft = GetInventionBlueprint(whatToCraft);
        if (toCraft == null) {
            Log(LogTextID.craftingFailedNullItem, LogType.ACTION_FAILURE);
       //     Printer.out("toCraft null, what did you wanna craft again?");
            return;
        }
        int progressRequired = toCraft.Get(InventionStat.ProgressRequiredToCraft);
        float progress = 0.0f;
//        Printer.out("toRandom iterations: "+toRandom);
        successiveCraftingAttempts = 0;
        float progressGained = 0;
        for (int i = 0; i < toRandom; ++i){ // Times to random.
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
            Adjust(Statistic.ItemsCrafted, 1);
            float ratioOverProgressed = (progress - progressRequired) / progressRequired;
          //  Printer.out("ratioOverProgressed: "+ratioOverProgressed);
            Random rCrafting = new Random(System.nanoTime());
            int levelAdjustment = 0;
            while(ratioOverProgressed > 0) {
                float randF = rCrafting.nextFloat();
                if (randF < ratioOverProgressed) {
            //        Printer.out("level increased +1");
                    ++levelAdjustment;
                }
                ratioOverProgressed -= randF;
            }
        //    Printer.out("HQ item obtained! Log it?");
            newlyCraftedObject.Set(InventionStat.QualityLevel, newlyCraftedObject.Get(InventionStat.QualityLevel) + levelAdjustment);
            newlyCraftedObject.UpdateDetails();             // Update quality level.
            cd.inventory.add(newlyCraftedObject);
            LogInfo(LogTextID.craftingComplete, newlyCraftedObject.name);
            switch (newlyCraftedObject.Get(QualityLevel)){
                case 0: Adjust(Statistic.ItemCrafted_level_0, 1); break;
                case 1: Adjust(Statistic.ItemCrafted_level_1, 1);break;
                case 2: Adjust(Statistic.ItemCrafted_level_2, 1);break;
                case 3: Adjust(Statistic.ItemCrafted_level_3, 1);break;
                case 4: Adjust(Statistic.ItemCrafted_level_4, 1);break;
                default:
                case 5: Adjust(Statistic.ItemCrafted_level_5, 1);break;
            }
        }
        else {
            LogInfo(LogTextID.craftingProgressed, ""+progress, ""+progressGained, ""+progressRequired);
//            Log("Crafting progressed by "+progress+" units. Progress is now at "+progressGained+" out of "+progressRequired+".", LogType.INFO);
            // Store as unfinished business?
        }
    }

    private Invention GetInventionBlueprint(String nameOfBlueprint) {
        for (int i = 0; i < cd.inventionBlueprints.size(); ++i) {
            Invention inv = cd.inventionBlueprints.get(i);
            if (inv.name.equals(nameOfBlueprint)) {
                return inv;
            }
        }
   //     Printer.out("Couldn't find invention of given name: "+nameOfBlueprint);
        return null;
    }

    private void Invent(Action inventAction) {
        float emit = ConsumeMaterials(hoursPerAction * 1.5f);
        // How many times to random.
        float toRandom = 0.5f + hoursPerAction; // Roll once for each hour?
        toRandom *= t_starvingModifier;
        toRandom *= CalcMaterialModifier();
        toRandom *= 1.f + 0.25f * GetEquipped(InventingBonus);
        String s = da.text+": ";
        // Check if inveting has been queued for any special item?
        boolean inventedSomething = false;
     //   Printer.out("toRandom iterations: "+toRandom);
        for (int i = 0; i < toRandom; ++i) { // Times to random.
            float relativeChance = toRandom > 1.0 ? 1 : toRandom;
            InventionType type = null;
            if (da.requiredArguments.size() > 0) {
                String typeStr = da.requiredArguments.get(0).value;
                type = InventionType.GetFromString(typeStr);
            }
            if (type == null) {
                Printer.out("Bad invention type.");
//                Log("Bad invention type", LogType.Error);
                System.exit(14);
                return;
            }
            if (type == InventionType.Any) {
                type = InventionType.RandomType();
                relativeChance += 0.05f; // + 5% chance of inventing if random?
       //         Printer.out("Type: "+type.name());
            }
            Invention inv = AttemptInvent(type, relativeChance);
            // Craft it on success?
            if (inv != null){
                successiveInventingAttempts = 0;
                inventedSomething = true;
                // Based on number of iterations toRandom, give a chance of higher quality if it succeeded early - i.e. time to polish it for the remaining duration?
                Adjust(Statistic.ItemsInvented, 1);
                // Add it to inventory too.
                cd.inventory.add(inv.CraftInventionFromBlueprint());
                // Don't auto-equip... Present dialog for it?
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

    // For when inventing...?
    public float getInventionProgress(InventionType type, int subType) {
        float progress = 0;
        for (int i = 0; i < cd.inventionBlueprints.size(); ++i) {
            Invention inv = cd.inventionBlueprints.get(i);
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
        float inventingLevel = Get(SkillType.Inventing).Level();
        float rMax = (1.f + 0.05f * inventingLevel + bonusFromInventingBefore) * relativeChance; // + 5% chance for each skill level in Inventing.
        rMax += 0.07f * GetEquipped(InventingBonus);  /// Random max increases by +0.07 (7%) for each point in inventing gear.
        float random = r.nextFloat() * rMax; // Random 0 to 1 + 0.1 max for each Inventing level, * relChance
        float successThreshold = 0.75f - successiveInventingAttempts * 0.03f - inventingLevel * 0.02f; // Increase success possibility with higher inventing skill, and for each attempt in succession.
     //   Printer.out("randomed: "+random+" out of 0-"+rMax+", successThreshold: "+successThreshold+" bonusFromBefore: "+bonusFromInventingBefore);
        if (random < successThreshold)
            return null;            // No success.
        // Determine high-quality ratio.
        int levelSuccess = (int) ((random - 0.8f) / 0.2f); // HQ1@ 1, HQ2@ 1.2, HQ3@ 1.4, HQ4@ 1.6, HQ5@ 1.8, etc.
        if (levelSuccess < 0)
            levelSuccess = 0;
        inv.Set(InventionStat.QualityLevel, levelSuccess);
        inv.RandomizeDetails();
     //   Printer.out("Level success: " + levelSuccess+" item name: "+inv.name);
        Invention existingBlueprint = GetBlueprintByName(inv.name);
        if (existingBlueprint != null){
            existingBlueprint.Adjust(InventionStat.TimesInvented, 1);             // Save type of invention? Add progress to the invention we already had?
            LogInfo(LogTextID.inventingOldThoughts, inv.name); // Log("While trying to invent something new, your thoughts go back to the old version of the "+inv.name+", perhaps you will be luckier next time.", LogType.INFO);
            return null;
        }
        switch (inv.Get(QualityLevel)){
            case 0: Adjust(Statistic.InventionsLevel_0, 1); break;
            case 1: Adjust(Statistic.InventionsLevel_1, 1); break;
            case 2: Adjust(Statistic.InventionsLevel_2, 1); break;
            case 3: Adjust(Statistic.InventionsLevel_3, 1); break;
            case 4: Adjust(Statistic.InventionsLevel_4, 1); break;
            default:
            case 5: Adjust(Statistic.InventionsLevel_5, 1); break;
        }
        cd.inventionBlueprints.add(inv);
        Log(LogTextID.inventSuccess, LogType.SUCCESS, inv.type.text(), inv.name);
//        Log("Invented a new " + inv.type.text() + ": " + inv.name, LogType.SUCCESS);
        return inv;
    }

    void Scout(Game game) {
        int speed = Speed(); //  1 base.
        speed += Get(TransportStat.SpeedBonus); // + based on transport.
        speed += GetEquipped(InventionStat.ScoutingBonus); // Add scouting bonus from gear. e.g. +2
        // Randomize something each hour? Many things with speed?
        float speedBonus = (float) Math.pow(speed, 0.5f); // Square root we made earlier.
     //   Printer.out("Speed: "+speed+" bonus(pow-modified): "+speedBonus);
        float toRandom = hoursPerAction * speedBonus;
        toRandom *= t_starvingModifier;
        Map<Finding, Integer> chances = new HashMap<Finding, Integer>();
        // Increase liklihood of encounters for each passing turn when scouting -> Scout early on is safer.
        // There will however be more of other resources available later on. :D
        int turn = (int) Get(Stat.TurnSurvived);
        int randomEncounter = 5 + turn - GetEquipped(ScoutingBonus); // Increase chance of combat for each survived turn, and maybe emissions as well?
        randomEncounter /= (1 + 0.5f * Get(SkillType.SilentScouting).Level()); // Each level of silent scouting reduces encounter rate.
        // -33% at 1st level, -50% at 2nd level, -60% at 3rd level, -66% at 4th, -71.5%, etc. (/1.5, /2, /2.5, /3, /3.5)
        chances.put(Finding.RandomEncounter, randomEncounter);
        chances.put(Finding.Nothing, 50);
        chances.put(Finding.Food, 15);
        chances.put(Finding.Materials, 15);
        chances.put(Finding.FoodHotSpot, 5);
        chances.put(Finding.MaterialsDepot, 5);
        chances.put(Finding.RandomPlayer, 5);
        chances.put(Finding.RandomItem, turn / 2); // Chance to find L0 items, increases as the game progresses?
        chances.put(Finding.RandomItemLevel1, turn / 4); // Chance to find L1 items, also increases, but slower.
        chances.put(Finding.RandomItemLevel2, turn / 8); // Chance to find L2 items.
//        chances.put(Finding.AbandonedShelter, 10 + turn);
  //      chances.put(Finding.RandomPlayerShelter, 10);
    //    chances.put(Finding.EnemyStronghold, 1+turn/3);
        int sumChances = 0;
        for (int i = 0; i < chances.size(); ++i) {
            sumChances += (Integer) chances.values().toArray()[i];
        }
        Printer.out("Sum chances: "+sumChances);
        EList<Finding> foundList = new EList<Finding>();
     //   String foundStr = "Found: ";
        int totalFound = 0;
        while(toRandom > 0) {
            toRandom -= 1;
            float baseChance = r.nextFloat() * sumChances;
            float chance = baseChance;
            for (int i = 0; i < chances.size(); ++i) {
                int step = (Integer) chances.values().toArray()[i];
                chance -= step;
                if (chance < 0){ // Found it
       //             foundStr += chances.keySet().toArray()[i]+", ";
                    Finding found = (Finding)chances.keySet().toArray()[i];
                    Printer.out("Found: "+found.name()+" at chance: "+chance);
                    if (found != Finding.Nothing){
                        ++totalFound;
                    }
                    else {
                    }
                    foundList.add(found);
                    // Break the inner loop.
                    i = chances.size();
                    break;
                }
            }
        }

//        Printer.out(foundStr);
        float amount = 0;
        int numFoodStashes = 0, numMatStashes = 0,  numEncounters = 0, numAbShelters = 0, numRPS = 0, numEnStrong = 0, numRandPlayers = 0, numRandItems = 0,
            numRandItemsL1 = 0, numRandItemsL2 = 0;
        int foodFound = 0, matFound = 0;
        String s = da.text+": While scouting the area, you ";
        for (int i = 0; i < foundList.size(); ++i) {
            Finding f = foundList.get(i);
            switch(f){            // Evaluate it.
                case Nothing: break;
                case RandomEncounter: numEncounters  += 1; break;
                case Food: numFoodStashes += 1; foodFound += Dice.RollD2(1) + Get(SkillType.Foraging).Level() ; break;
                case Materials: numMatStashes += 1; matFound += Dice.RollD2(1) + Get(SkillType.Scavenging).Level() ; break;
                case RandomPlayer: numRandPlayers += 1; break;
                case RandomItem: numRandItems +=1; break;
                case RandomItemLevel1: numRandItemsL1 += 1; break;
                case RandomItemLevel2: numRandItemsL2 += 1; break;
        //        case FoodHotSpot: ++numFoodHotSpots;
      //          case MaterialsDepot: ++numMatDepos;
//                case AbandonedShelter: numAbShelters += 1; playEvents = true; break;
  //              case RandomPlayerShelter: numRPS += 1; playEvents = true; break;
    //            case EnemyStronghold: numEnStrong += 1; break;
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
            LogInfo(LogTextID.scoutFoodStashes, "" + numFoodStashes, ""+foodFound);
            Adjust(Stat.FOOD, foodFound);
            Adjust(Statistic.ResourcesFoundByScouting, foodFound);
        }
        if (matFound > 0) {
            LogInfo(LogTextID.scoutMatStashes, "" + numMatStashes, ""+matFound);
//            s += numMatStashes > 1 ? "\n- find " + numMatStashes + " stashes of materials, totalling at " + Stringify(matFound) + " units" : numMatStashes == 1 ? "\n a stash of materials: " + Stringify(matFound) + " units" : "";
            Adjust(Stat.MATERIALS, matFound);
            Adjust(Statistic.ResourcesFoundByScouting, matFound);
        }
        if (numRandPlayers > 0){
            // Just log that you found players, no need to display more stuff before...
            for (int i = 0; i < numRandPlayers; ++i) {
                FindRandomPlayer(5, game);
            }
        }
        int totalRandItemsFounds = numRandItems + numRandItemsL1 + numRandItemsL2;
        if (totalRandItemsFounds > 0){
            LogInfo(LogTextID.scoutFoundItems, ""+totalRandItemsFounds);
            for (int i = 0; i < numRandItems; ++i){
                Invention inv = Invention.Random(0);
                Obtained(inv);
            }
            for (int i = 0; i < numRandItemsL1; ++i){
                Invention inv = Invention.Random(1);
                Obtained(inv);
            }
            for (int i = 0; i < numRandItemsL2; ++i){
                Invention inv = Invention.Random(2);
                Obtained(inv);
            }
            Adjust(Statistic.ItemsFoundByScouting, totalRandItemsFounds);
        }
        /// Advanced shit... Queue up for exploration later?
//        s += numAbShelters > 1? "\n- find "+numAbShelters+" seemingly abandoned shelters" : numAbShelters == 1? "\n- find an abandoned shelter." : "";
  //      Adjust(Stat.ABANDONED_SHELTER, numAbShelters);
        // Find max 1 player shelter per scouting round?
    //    s += numRPS >= 1? "\n- find a shelter which seems to be inhabited" : "";
      //  Adjust(Stat.RANDOM_PLAYERS_SHELTERS, numRPS >= 1? 1 : 0);
//        s += numEnStrong >= 1? "\n- find an enemy stronghold." : "";
  //      Adjust(Stat.ENEMY_STRONGHOLDS, numEnStrong >= 1? 1 : 0);
//        Log(s, LogType.INFO);
    }

    /// Finds a random player - if any are still not found. # of attempts will give the likelihood that one is actually found that was not already known.
    static Random findRPR = new Random(System.currentTimeMillis());
    private boolean FindRandomPlayer(int numRandomAttempts, Game game) {
        for (int i = 0; i < numRandomAttempts; ++i){
            Player p = game.RandomLivingPlayer(cd.knownPlayerNames);
            if (p == this) {
                Printer.out("Skipping self...");
                continue;
            }
            if (p == null)
                continue;
            if (KnowsThisPlayer(p.name))
                continue;
            FoundPlayer(p);
            return true;
        }
        return false;
    }

    void BuildDefenses() {
        float emit = ConsumeMaterials(hoursPerAction * 0.5f);
        float progress = Dice.RollD3(2 + Get(SkillType.Architecting).Level()); // 2 to 6, increases by 1D3 for each skill in architecting.
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
    private float ConsumeMaterials(float baseAmount) {
        float s = baseAmount;
        int lMatEfficiency = Get(SkillType.MaterialEfficiency).Level();
        switch(lMatEfficiency) {
            case 4: s *= 0.6f; // 0.3,   40,  Multiplicative effects for each timer :)
            case 3: s *= 0.7f; // 0.5,   20,
            case 2: s *= 0.8f; // 0.72,   10,
            case 1: s *= 0.9f; break; // 0.9,   5,
            default:
            case 0: break;
        }
        Adjust(Stat.MATERIALS, -s); // Consume! 1 unit of materials per hour?
        float emissionsToGenerate = s / (1 + 0.5f * Get(SkillType.Recycling).Level()); // 33%, 50%, 75%, 80%, etc. 1/1.5, 1/2, 1/2.5, 1/3
        int toGenerate = Math.round(emissionsToGenerate);
        GenerateEmissions(toGenerate); // Generate emissions based on materials consumed.
        return s;
    }
    private void GenerateEmissions(int amount) {
        Adjust(Stat.AccumulatedEmissions, amount);
        Adjust(Statistic.TotalEmissionsGenerated, amount);
    }

    public Skill Get(SkillType skillType) {
        int index = skillType.ordinal();
        return cd.skills.get(index);
    }

    public Skill Get(Skill skill) {
        int index = skill.GetSkillType().ordinal();
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
    public void GainEXP(int expGained) {
      //  Printer.out("EXP gained: "+expGained);
        // Check queued skills.
        int xp = expGained;
        while (xp > 0 && cd.skillTrainingQueue.size() > 0) {
            SkillType next = SkillType.GetFromString(cd.skillTrainingQueue.get(0));
            if (next == null){
                Printer.out("Bad skill String: "+cd.skillTrainingQueue.get(0));
                new Exception().printStackTrace();
                System.exit(16);
            }
            Skill toSkillUp = cd.skills.get(next.ordinal());
            int needed = toSkillUp.EXPToNext();
            if (needed <= 0) {
       //         Printer.out("Needed " + needed + ": skipping this skill, de-queueing.");
                cd.skillTrainingQueue.remove(0);
                continue;
            }
      //      Printer.out("EXP to next level? "+needed);
            int toGain = xp;
            if (needed < xp)
                toGain = needed;
            xp -= toGain;
            int oldLevel = toSkillUp.Level();
            int levelReached = toSkillUp.GainExp(toGain);
            int newLevel = toSkillUp.Level();
            if (newLevel > oldLevel) {
                Log(LogTextID.skillLeveledUp, LogType.EXP, toSkillUp.Text(), ""+levelReached);
                Adjust(Statistic.MonsterKills.SkillLevelUps, 1);
             //   Printer.out("Skill leveled up: "+toSkillUp.Text());
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
        this.attack = defendingShelter?  ShelterAttack() : OnTransportAttack();
        this.defense = defendingShelter? ShelterDefense() : OnTransportDefense();
        this.hp = GetInt(Stat.HP);
        this.maxHP = MaxHP();
        c.attackDamage = Damage();
        c.attacksPerTurn = AttacksPerTurn();
        c.fleeSkill = Get(SkillType.FleetRetreat).Level();
        c.fleeBonusFromTransport = Get(TransportStat.FleeBonus);
        c.ranAway = false; // Reset temporary variables such as fleeing.
        c.parry = (int) TotalParryingBonus(); //  GetEquipped(InventingBonus.ParryBonus) + Get(SkillType.Parrying).Level();
        c.stunnedRounds = 0;
        c.ensnared.clear();
        consecutiveFleeAttempts = 0;
    }

    @Override
    protected void OnKilled(Combatable targetYouKilled) {
        if (targetYouKilled instanceof Player) {
            Adjust(Statistic.PlayerKills, 1);
            // Take their stuff?
            Player tP = (Player) targetYouKilled;
            float ratio = tP.Get(Stat.Lives) > 0? 0.5f : 1.0f; // Get all if the player actually died the final life.
            int emissionsToInherit = (int) (tP.Get(Stat.AccumulatedEmissions) * ratio * 0.5f); // Only inherit, say, half..?
            tP.Adjust(Stat.AccumulatedEmissions, -emissionsToInherit);  // Decrease emissions from the player who was defeated.
            if (ratio == 1.f) {
                emissionsToInherit += 10; // + 10 if the target died.
                tP.Set(Stat.InheritedEmissions, 0);
            }
            Adjust(Stat.InheritedEmissions, emissionsToInherit); // Add emissions to the attacker.
            Adjust(Statistic.InheritedEmissions, emissionsToInherit);

            for (int i = 0; i < tP.cd.inventory.size(); ++i){ /// Transfer all items.
                Obtained(tP.cd.inventory.get(i));
            }
            tP.cd.inventory.clear();
            for (int i = 0; i < tP.cd.inventionBlueprints.size(); ++i) {            /// And blueprints?
                Obtained(tP.cd.inventionBlueprints.get(i));
            }
            tP.cd.inventionBlueprints.clear();
        }
        else
            Adjust(Statistic.MonsterKills, 1);
    }
    // Adds items to inventory and displays an "Obtained ..." log-message. Also works for blueprints.
    private void Obtained(Invention invention) {
        // Check blueprint or item?
        if (invention.Get(InventionStat.Blueprint) == 1){
            cd.inventionBlueprints.add(invention);
            LogInfo(LogTextID.obtainedBlueprint, invention.name);
            return;
        }
        cd.inventory.add(invention);
        LogInfo(LogTextID.obtainedItem, invention.name);
    }

    @Override
    protected void OnDied(Combatable attackerWhoKilledYou) {
        Adjust(Statistic.TimesKnockedOut, 1);
        Set(Stat.TurnSurvived, 0); // Every time you're knocked out, reset the turn-survived counter so that the player won't be slaughtered again by AI at least.
        Set(Stat.EntHatred, Get(Stat.EntHatred) *  1 - Difficulty.LossRatio((int) Get(Config.Difficulty))); // Reduce ent-hatred based on difficulty, i.e. easiest 98% reduced, next-hardest 50% reduced
        if (!IsAliveInCombat()){ // If truly not alive anymore, inform all listeners.
            for (int i = 0; i < listeners.size(); ++i) {
                PlayerListener pl = listeners.get(i);
                pl.OnPlayerDied(this);
            }
        }
    }

    @Override
    protected void OnDealtDamage(Combatable target, int damage, Encounter enc) {
        Adjust(Statistic.TotalDamageDealt, damage);
        Invention weapon =  GetEquippedWeapon();
        if (weapon != null){
            int consussive = weapon.Get(InventionStat.Concussive);
            if (consussive > 0){
                if (Dice.RollD6(2) + consussive >= 12){
                    // Stun it for 1D3 + concussive rounds?
                    target.stunnedRounds = Dice.RollD3(1) + 1; //2-4, 1st will disappear instantly, so 1-3 rounds in practice.
                    enc.LogEnc(new Log(LogTextID.targetStunned, LogType.ENC_INFO, target.name));
                }
            }
        }
    }

    @Override
    protected void OnReceivedDamage(Combatable fromTarget, int damage, Encounter enc) {
        Adjust(Statistic.TotalDamageTaken, damage);
    }

    @Override
    public boolean Attack(Combatable target, Encounter enc) {
        // Add more actions later perhaps... such as the ranged weapons...? o.O
        return DefaultAttack(target, enc);
    }

    private int AttacksPerTurn() {
        int attacks = 1;
        attacks += (UnarmedCombatBonus() - 1) / 2;
        if (GetEquippedWeapon() != null)
            attacks += GetEquippedWeapon().Get(InventionStat.BonusAttacks);
        return attacks;
    }

    public EList<String> KnownPlayerNames() {
        if (cd.knownPlayerNames == null)
            cd.knownPlayerNames = new EList<>();
        return cd.knownPlayerNames;
    }

    public static Player NewAI(String name) {
        Player p =  new Player();
        p.SetName(name);
        p.isAI = true;
        return p;
    }

    public void PrintAll() {
        Printer.out("\nName: " + name +" stats:");
        for (int i = 0; i < cd.statArr.length; ++i)
            Printer.out(" "+cd.statArr[i]);

        Printer.out("Max HP: "+MaxHP()+" Attack: "+BaseAttack()+", shelterAttack "+ShelterAttack()
                +" Base Defense "+BaseDefense()+", shelterDefense "+ShelterDefense()
                +" Parrying: "+TotalParryingBonus()+"\nAttacks per round: "+AttacksPerTurn());

        Printer.out("\n skills:");
        for (int i = 0; i < cd.skills.size(); ++i) {
            Skill s = cd.skills.get(i);
            if (s == null)
                continue;
            Printer.out(" "+s.Text()+":"+s.Level()+":"+s.TotalExp());
        }
        Printer.out("\n inventions:");
        for (int i = 0; i < cd.inventionBlueprints.size(); ++i) {
            Invention inv = cd.inventionBlueprints.get(i);
            Printer.out(" "+inv.type.text()+": \""+inv.name+"\", ");
        }
        Printer.out("\n inventory:");
        for (int i = 0; i < cd.inventory.size(); ++i) {
            Invention inv = cd.inventory.get(i);
            Printer.out(" "+inv.type.text()+": \""+inv.name+"\", ");
        }
        Printer.out();
    }

    private float TotalParryingBonus() {
        return Get(SkillType.Parrying).Level() + GetEquipped(ParryBonus);
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

    public int ProcessQueuedActiveActions(Game game){
        int numProcessed = 0;
        if (cd.queuedActiveActions.size() > 0)
            ;// Printer.out("Processing queued active actions: "+cd.queuedActiveActions.size());
        for (int i = 0; i < cd.queuedActiveActions.size(); ++i){
            Action a = cd.queuedActiveActions.get(i);
            EvaluateAction(a, game);
            ++numProcessed;
        }
        cd.queuedActiveActions.clear();
        return numProcessed;
    }

    public String DailyActionsAsString() {
        String s = "";
        for (int i = 0; i < cd.dailyActions.size(); ++i)
            s += cd.dailyActions.get(i)+"; ";
        return s;
    }
    // Or LoadFromClient
    public void SaveFromClient(Player clientPlayer) {
        updatesFromClient.add(new Date());
        if (Get(Stat.TurnSurvived) == clientPlayer.Get(Stat.TurnSurvived) ||            // Check if it's the same TurnSurvived, if not, don't copy over the actions?
                clientPlayer.Get(Config.RetainDailyActionsOnNewTurn) == 1 ) {       // Unless the player specifically has set the option to retain daily actions on new turns, then keep it.
            cd.dailyActions = clientPlayer.cd.dailyActions; // Copy over queued actions.
            cd.skillTrainingQueue = clientPlayer.cd.skillTrainingQueue; // Skill training queue.
        }
        // For skill training queue, hopefully this is changed less often, so the user can edit it later..?
        // Always add the queued active actions, regardless of if the turn is the same one or a new one.
        cd.queuedActiveActions = clientPlayer.cd.queuedActiveActions; // And queued active actions.

        /// Save config-elements that the player may adjust at any time.
        Set(Config.RetainDailyActionsOnNewTurn, clientPlayer.Get(Config.RetainDailyActionsOnNewTurn));

     //   Printer.out("Queued Active actions: "+cd.queuedActiveActions.size());
        if (log != null && clientPlayer.log != null) {
            for (int i = 0; i < log.size() && i < clientPlayer.log.size(); ++i) {
                Log clientLog = clientPlayer.log.get(i);
                Log serverEquivalent = GetLog(clientLog.LogID());
                if (serverEquivalent == null) {
                    Printer.out("SOMETHING IS HORRIBLY WRONG syncing log messages");
                    continue;
                }
                // Save the state of old log messages that have already been viewed by the player.
                serverEquivalent.displayedToEndUser = clientLog.displayedToEndUser;
                if (serverEquivalent.displayedToEndUser == 1)
                    serverEquivalent.displayedToEndUser = 2;
            }
        }
        transports = clientPlayer.transports; // Copy over all transports.
    //    Printer.out("SaveFromClient, transports: "+clientPlayer.TopTransportsAsString(3)); // Print the new transport data we received.
        EList<Invention> equipped = clientPlayer.GetEquippedInventions();         // Equip those items as requested by the player as well.
        for (int i = 0; i < equipped.size(); ++i){
            Invention clientItem = equipped.get(i);
            int index = clientPlayer.cd.inventory.indexOf(clientItem); // Still client here.
            Invention item = GetItemByIndex(index); // Now in server-data.
            if (item == null){
                Printer.out("ERROR: Mismatch in item indices, could not equip player requested items! Player: "+name+" index requested: "+index+" totalItems: "+cd.inventory.size());
  //              new Exception().printStackTrace();
                continue;
            }
            if (item.IsEquipped() == false) {
                Equip(item);
                Adjust(Statistic.EquipmentChanges, 1);
            }
        }
        // Check all to be recycled?
        EList<Invention> toBeRecycled = clientPlayer.GetGearToBeRecycled();
        for (int i = 0; i < toBeRecycled.size(); ++i){
            Invention clientItem = toBeRecycled.get(i);
            int clientIndex = clientPlayer.cd.inventory.indexOf(clientItem);
            Invention itemHere = GetItemByIndex(clientIndex);
            if (itemHere == null){
                Printer.out("Bad index to be recycled");
                continue;
            }
            // Recycle it!
            cd.inventory.remove(itemHere);
            int resourcesGained = 2 + Get(SkillType.Recycling).Level();
            Adjust(Stat.AccumulatedEmissions, -1); // Reduce emissions by 1 when doing this.
            LogInfo(LogTextID.recycled, itemHere.name, ""+resourcesGained);
        }

    }

    private EList<Invention> GetGearToBeRecycled() {
        EList<Invention> invs = new EList<>();
        for (int i = 0; i < cd.inventory.size(); ++i){
            Invention inv = cd.inventory.get(i);
            if (inv.Get(ToRecyle) == 1)
                invs.add(inv);
        }
        return invs;
    }

    private Log GetLog(long id) {
        for (int i = 0; i < log.size(); ++i){
            if (log.get(i).LogID() == id)
                return log.get(i);
        }
        return null;
    }

    private Invention GetItemByIndex(int index) {
        if (index >= 0 && index < cd.inventory.size())
            return cd.inventory.get(index);
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

    private boolean EquipItemByIndex(int index) {
        Invention item = GetItemByIndex(index);
        if (item != null){
            Equip(item);
            return true;
        }
        return false;
    }



    private boolean EquipItemWithID(long id) {
        Invention item = GetItemByID(id);
        if (item != null){
            Equip(item);
            return true;
        }
        return false;
    }

    public void SaveLog(EList<LogType> filterToSkip, String folder) {
        String path = folder+"/"+gameID+"_player_log_"+name+".txt";
//        Printer.out("SavePlayerLog, dumping logs to file "+path);
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
        int multiplier = 6 - diff; // 1 to 6 depending on difficulty.
        Set(Stat.MAX_HP, Stat.MAX_HP.defaultValue + 2 * BonusFromDifficulty());
        Set(Stat.FOOD, Stat.FOOD.defaultValue * multiplier);
        Set(Stat.MATERIALS, Stat.MATERIALS.defaultValue * multiplier);
        Set(Stat.BASE_ATTACK, Stat.BASE_ATTACK.defaultValue + multiplier / 2);
        Set(Stat.BASE_DEFENSE, Stat.BASE_DEFENSE.defaultValue + multiplier);
        Set(Stat.Lives, Difficulty.Lives(diff));
        // Heal HP.
        Set(Stat.HP, MaxHP());
    }

    /// Just 6 - Difficulty.
    public int BonusFromDifficulty() {
        return (int) (6 - Get(Config.Difficulty));
    }


    public void PrepareForTotalRestart(){
        cd.inventory.clear();
        cd.inventionBlueprints.clear();
    }

    public void ReviveRestart() {
        // Keep name, email, password, starting bonus, difficulty,
        log = new EList<Log>(); // Clear the log though?
        // Reset the config for last log ID seen.
        Set(Config.LatestLogMessageIDSeen, 0);
        SetDefaultStats();        // reset the rest.
        AssignResourcesBasedOnDifficulty();        // Grant the starting bonus again.
        AssignResourcesBasedOnStartingBonus(); // Assign starting bonus.
    }

    // Random item from the inventory, can literally be anything - no...
    public Invention RandomItem() {
        if (cd.inventory.size() == 0)
            return null;
        return cd.inventory.get(r.nextInt(cd.inventory.size()) % cd.inventory.size());
    }
    // o-o
    public void MarkLogMessagesAsReadByClient() {
        for (int i = 0; i < log.size(); ++i)
            log.get(i).displayedToEndUser = 1;
    }

    /// Server-side? client-side?
    public void UpdateTransportMinutes(EList<TransportOccurrence> transportOccurrences) {
        int mostUsedSetting = TransportOccurrence.GetMostUsedDetectionMethod(transportOccurrences);
        Printer.out("Most used setting: "+mostUsedSetting);
        for (int i = 0; i < transports.size(); ++i) {         // Clear the array.
            transports.get(i).secondsUsed = 0;
            transports.get(i).settingsUsed = mostUsedSetting;
        }
        for (int i = 0; i < transportOccurrences.size(); ++i){
            TransportOccurrence to = transportOccurrences.get(i);
            Printer.out(to.transport.name()+" dur: "+to.DurationSeconds()+"s ratio: "+to.ratioUsed);
            long durS = to.DurationSeconds();
            Transport transport = Get(to.transport);
            if (transport == null) {
                Printer.out("No such transpot such as "+to.transport.name());
                continue;
            }
            transport.secondsUsed += durS;
        }

    }

    private Transport Get(TransportType transport) {
        for (int i = 0; i < transports.size(); ++i){
            if (transports.get(i).tt.ordinal() == transport.ordinal())
                return transports.get(i);
        }
        Printer.out("Couldn't get transport: "+transport.name());
        return null;
    }

    // Prints the transports in the order of highest occurrence.
    public void PrintTopTransports(int topNum) {
        Printer.out(TopTransportsAsString(topNum));
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
     //   Printer.out("Top "+topNum+":");
        EList<Transport> to = TransportsSortedBySeconds();
        float totalTimeSeconds = TotalTransportSeconds();
        String s = "";
        for (int i = 0; i < topNum; ++i){
            Transport highest = to.get(i);;
            String formatedPercentage = String.format(Locale.ENGLISH, "%.2f", 100*highest.secondsUsed/totalTimeSeconds);
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
        if (startIndex >= log.size()){ // Index ends at log.size() - 1.
            // Really outside? then reply an empty list.
            return new EList<>();
        }
        if (endIndexInclusive < 0){ // Before the list event starts? return nothing, yo.
            return new EList<>();
        }
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
        /*
        for (int i = 0; i < partList.size(); ++i){
            Printer.out(partList.get(i));
        }*/
        return partList;
    }

    public void ClearLog() {
        log = new EList<Log>();
    }

    public int GameID() {
        return gameID;
    }

    // Emulate various transports.
    static Random transportRand = new Random(System.currentTimeMillis());
    // RandomizeTransportUsageData
    public void RandomizeGenerateTransportUsageData() {
        for (int i = 0; i < transports.size(); ++i) // Generate some other seconds of various degrees.
            transports.get(i).secondsUsed = 0;
        transports.get(TransportType.Idle.ordinal()).secondsUsed = 3600;
        for (int i = 0; i < 5; ++i) // Generate some other seconds of various degrees.
            transports.get(transportRand.nextInt(transports.size()) % transports.size()).secondsUsed += 3600 / (i + 1); // 3600, 1800, 1200, 900, etc.
    }

    public void OnPlayerDied(Player player) {
        /// OK, check if we knew this player - check the known player names.
        for (int i = 0; i < cd.knownPlayerNames.size(); ++i){
            String name = cd.knownPlayerNames.get(i);
            if (name.equals(player.name)){
                LogInfo(LogTextID.playerDied, player.name);
                Printer.out("Player "+name+" was informed of the death of "+player.name);
                cd.knownPlayerNames.remove(i);
            }
        }
    }

    public void SetLevel(SkillType weaponizedCombat, int newLevel) {
        Skill skill = Get(weaponizedCombat);
        skill.TotalExp();
        skill.GainExp(skill.EXPToLevelFrom0(newLevel)); // Gain some exp.
        Printer.out("Exp gained, now skill "+skill.name()+" is level: "+skill.Level());
    }

    public int TopTransportClassifierSettingUsed() {
        long[] settingsSeconds = new long[TransportOccurrence.DETECTION_METHODS];
        EList<Transport> to = TransportsSortedBySeconds();
        int biggestSettingIndex = 0;
        for (int i = 0; i < to.size(); ++i){
            Transport t = to.get(i);
            settingsSeconds[t.settingsUsed] += t.secondsUsed;
            if (settingsSeconds[t.settingsUsed] > settingsSeconds[biggestSettingIndex]){
                biggestSettingIndex = t.settingsUsed;
            }
        }
        return biggestSettingIndex;
    }

    public enum StartingBonus{
        FoodSupply("Food supply"),
        MaterialsSupply("Materials supply"),
        Weapon("A weapon"),
        Armor("A body armor"),
        Tool("A tool"),
        Inventions("2 inventions"),
        ;
        StartingBonus(String text){
            this.text = text;
        }
        public String text;
    }

    public void AssignResourcesBasedOnStartingBonus() {
        int startingBonusIndex = (int) Get(Config.StartingBonus);
        if (startingBonusIndex < 0 || startingBonusIndex >= StartingBonus.values().length){
            Printer.out("Bad.");
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
                cd.inventionBlueprints.add(Invention.Random(BonusFromDifficulty()/3));
                cd.inventionBlueprints.add(Invention.Random(BonusFromDifficulty()/3));
                break;
        }
    }
}
