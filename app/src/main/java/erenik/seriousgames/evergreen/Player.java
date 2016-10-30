package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



enum Stat
{
    HP(10), MAX_HP(10),
    FOOD(5),
    MATERIALS(3),
    BASE_ATTACK(10), ATTACK_BONUS(0),
    BASE_DEFENSE(10), DEFENSE_BONUS(0),
    SHELTER_DEFENSE(1), SHELTER_DEFENSE_PROGRESS(0), // Level and progress towards next level.
    SPEED(1),
    EMISSIONS(0),
    ABANDONED_SHELTER(0), // Abandoned shelters to explore! Own event for it.
    ENCOUNTERS(0),  // Random enemy encounters to defeat. Encountered while doing other tasks (scouting, gathering).
    RANDOM_PLAYERS_SHELTERS(0),  // Random player shelters to be randomly allocated shortly (ask server to whom it belongs?) or postpone until later.
    ENEMY_STRONGHOLDS(0), // Enemy strongholds that you've found.
    ;

    Stat(float defaultValue)
    {
        this.defaultValue = defaultValue;
    }
    float defaultValue;
};

enum LogType
{
    INFO, // General info
    PROBLEM_NOTIFICATION, // Warning/problem notifications.
    PROGRESS,
    ATTACKED, // For when taking damage.
    EVENT,
    ;
    /*
    int HexColor()
    {
        return getColor(getContext(), GetResourceColor());
    };*/ // Text font color for this message.
    int GetResourceColor()
    {
        switch(this)
        {
            case INFO: return R.color.info;
            case PROGRESS: return R.color.progress;
            case ATTACKED: return R.color.attacked;
            case EVENT: return R.color.event;
            case PROBLEM_NOTIFICATION: return R.color.problemNotification;
        }
        return R.color.black;

    }
};

class NotAliveException extends Exception
{
    NotAliveException() {
        super("HP reached below 0.");
    }
}

/// For the player game-log. To be color-coded etc.?
class Log
{
    Log(String s, LogType t)
    {
        text = s;
        type = t;
        date = new Date();
    }
    Date date; // Time-stamp of this log message.
    String text;
    LogType type;
};
/**
 * Created by Emil on 2016-10-25.
 */
public class Player
{
    static Random r = new Random(System.nanoTime());
    // Main stats.
//    float hp, food, materials, base_attack, base_defense, emissions;
    String name;
    float[] statArr = new float[Stat.values().length];
    /// Used in main menu, and also saved.
    List<DAction> dailyActions = new ArrayList<DAction>();
    int skill = -1;
    int activeAction = -1;
    /// Increment every passing day. Stop once dying.
    int turnSurvived = 1;
    /// Temporary/chaning starving modifier, changes each turn. Default 1, lower when starving.
    float t_starvingModifier = 1;
    float t_materialModifier = 1; // Same as starving, lowers when negative (debt) on action requirements of materials.

    /// List of events to evaluate/process/play-mini-games. Old events are removed from the list.
    List<Event> events = new ArrayList<Event>();
    /// Log of messages for this specific player.
    List<Log> log = new ArrayList<Log>();

    /// Array of exp in each Skill.
    List<Skill> skills =  new ArrayList<Skill>(Arrays.asList(Skill.values()));

    // Auto-created. Start-using whenever.
    static private Player player = new Player();
    static Player getSingleton()
    {
        return player;
    }
    private Player()
    {
        name = "Noname";
        SetDefaultStats();
    }
    boolean IsAlive()
    {
        return GetInt(Stat.HP) > 0;
    }
    int Speed()
    {
        return GetInt(Stat.SPEED);
    }
    int Attack()
    {
        return GetInt(Stat.BASE_ATTACK) + GetInt(Stat.ATTACK_BONUS);
    }
    int Defense()
    {
        return GetInt(Stat.BASE_DEFENSE) + GetInt(Stat.DEFENSE_BONUS);
    }
    int ShelterDefense()
    {
        return Defense() + GetInt(Stat.SHELTER_DEFENSE);
    }
    void SetDefaultStats()
    {
        // Default stats?
        for (int i = 0; i < Stat.values().length; ++i)
            statArr[i] = Stat.values()[i].defaultValue;
        for (int i = 0; i < skills.size(); ++i) // Reset EXP in each skill?
            skills.get(i).setTotalEXP(0);
    }
    void Adjust(Stat s, float amount)
    {
        statArr[s.ordinal()] += amount;
        if (s == Stat.HP ) {
            if (GetInt(Stat.HP) > GetInt(Stat.MAX_HP))
                SetInt(Stat.HP, GetInt(Stat.MAX_HP));
            if (GetInt(Stat.HP) <= 0)
            {
                Log("You died. Game over", LogType.ATTACKED);
                System.out.println("GaME OVER!!!");
                Intent i = new Intent(App.currentActivity.getBaseContext(), GameOver.class);
                App.currentActivity.startActivity(i);

                // Quit?
            }
        }
    }
    // Getter for main stats.
    float Get(int stat)
    {
        return statArr[stat];
    }
    float Get(Stat s)
    {
        return statArr[s.ordinal()];
    }
    int GetInt(Stat s)
    {
        return (int) statArr[s.ordinal()];
    }
    void SetInt(Stat s, int am)
    {
        statArr[s.ordinal()] = am;
    }
    void Set(int stat, float value)
    {
        statArr[stat] = value;
    }
    /// Saves to local "preferences"
    public boolean SaveLocally()
    {
        SharedPreferences sp = App.GetPreferences();
        SharedPreferences.Editor e = sp.edit();
        // Put.
        e.putString(Constants.NAME, name);
        e.putBoolean(Constants.SAVE_EXISTS, true);
        // Stats
        for (int i = 0; i < Stat.values().length; ++i)
        {
            System.out.println("Saving "+Stat.values()[i].toString());
            e.putFloat(Stat.values()[i].toString(), statArr[i]);
        }
        // Action
        e.putInt(Constants.ACTIVE_ACTION, activeAction);
        // Save daily actions as string?
        String s = "";
        for (int i = 0; i < dailyActions.size(); ++i)
        {
            DAction d = dailyActions.get(i);
            if (d == null)
                continue;
            s += d.text+";";
        }
        e.putString(Constants.DAILY_ACTIONS, s);
//        e.putInt(Constants.DAILY_ACTION, dailyAction);
        e.putInt(Constants.SKILL, skill);
        // Save/commit.
        boolean ok = e.commit();
        System.out.println("Save OK: "+ok);
        assert(ok);
        return ok;
    }
    /// Loads from local "preferences".
    public boolean LoadLocally()
    {
        SharedPreferences sp = App.GetPreferences();
        boolean hasSave = sp.getBoolean(Constants.SAVE_EXISTS, false);
        if (!hasSave)
            return false;
        name = sp.getString(Constants.NAME, "BadSaveName");
        // Stats
        for (int i = 0; i < Stat.values().length; ++i)
        {
            System.out.println("Saving "+Stat.values()[i].toString());
            statArr[i] = sp.getFloat(Stat.values()[i].toString(), Stat.values()[i].defaultValue);
        }
        // Choices.
        activeAction = sp.getInt(Constants.ACTIVE_ACTION, -1);
        // Save daily actions as string?
        String s = sp.getString(Constants.DAILY_ACTIONS, "");
        String[] split = s.split(";", 5);
        int numSemiColons = split.length;
        dailyActions.clear();
        for (int i = 0; i < split.length; ++i)
        {
            DAction da = DAction.GetFromString(split[i]);
            if (da == null)
                continue;
            dailyActions.add(da);
        }
        skill = sp.getInt(Constants.SKILL, -1);
        return true;
    }

    void Log(String text, LogType t)
    {
        log.add(new Log(text, t));
    }

    /// Stuff to process at the start of every day, also NewGame.
    void NewDay()
    {
        // New day, assign transport?
        ++player.turnSurvived;
    }
    /// Adjusts stats, generates events based on chosen actions to be played, logged
    void NextDay() throws NotAliveException
    {
        if (GetInt(Stat.HP) <= 0)
            throw new NotAliveException();
        Log("-- Day "+player.turnSurvived+" --", LogType.INFO);
        // Yeah.
        Adjust(Stat.FOOD, -2);
        if (GetInt(Stat.FOOD) >= 0) {
            Adjust(Stat.HP, 1);
        }
        else {
            float loss = Get(Stat.FOOD) / 5;
            Adjust(Stat.HP, loss);
            Log("Starving, lost "+(-loss)+" HP.", LogType.ATTACKED);
            if (!IsAlive())
                return;
        }
        // Reset food to 0? Or have a debt on eating perhaps?

        // Analyze some chosen hours of activity. Generate events and stuff for each?
        EvaluateActions();

        /// Force the player to play through the generated events before they proceed.
//        HandleGeneratedEvents();

        // Attacks of the evergreen?

        if (IsAlive() && AllEventsHandled())
            NewDay();
    }

    // Present dialogue-box for handled events.
    void HandleGeneratedEvents(FragmentManager fragMan)
    {
        EventDialogFragment event = new EventDialogFragment();
        event.type = Finding.Nothing;
        // Check events to run.
        if (Get(Stat.ENCOUNTERS) > 0)
            event.type = Finding.Encounter;
        else if (Get(Stat.ABANDONED_SHELTER) > 0)
            event.type = Finding.AbandonedShelter;
        else if (Get(Stat.RANDOM_PLAYERS_SHELTERS) > 0)
            event.type = Finding.RandomPlayerShelter;
        if (event.type != Finding.Nothing)
            event.show(fragMan, "event");
    }
    private boolean AllEventsHandled()
    {
        if (Get(Stat.ENCOUNTERS) > 0)
            return false;
        return true;
    }

    String Stringify(float value)
    {
        // Stringifies target value, based on some assumptions.
        int iValue = Math.round(value);
        float remainder = value - iValue;
        if (Math.abs(remainder) < 0.05f)
            return ""+iValue;
        return String.format("%.2f", value);
    }
    float hoursPerAction  = 1.f;
    float foodHarvested = 0.0f;
    DAction da;
    void EvaluateActions()
    {
        t_starvingModifier = GetInt(Stat.HP) >= 0? 1.0f : 1 / (1 + Math.abs(Get(Stat.HP)) * 0.5f);
        // Have this increase with some skill?
        float hoursSimulated = 6.f;
        int div = dailyActions.size();
        switch(div) // Add waste time if many actions are scheduled.
        {
            case 8: div += 4.f; break;
            case 7: div += 2.f; break;
            case 6: div += 1.f; break;
            case 5: div += 0.5f; break;
            case 4: div += 0.25f; break;
        }
        if (div > 8)
            div += 5.f;
        if (div > 6)
            Log("Having scheduled too much to do during the day, you manage to lose a lot of time between the actions you had intended to do. You are even forced to cancel entirely some of the actions you wanted to do.", LogType.PROBLEM_NOTIFICATION);
        else if (dailyActions.size() > 3)
            Log("During the day, you lose some time while changing tasks. Choose 3 or fewer actions per day for full efficiency.", LogType.PROBLEM_NOTIFICATION);
        hoursPerAction = hoursSimulated / div;
        System.out.println("hoursPerAction: "+hoursPerAction);
        foodHarvested = 0.0f;
        // Execute at most 8 actions per day, regardless of queue.
        for (int i = 0; i < dailyActions.size() && i < 6; ++i)
        {
            da = dailyActions.get(i);
            EvaluateAction(da);
        }
    }
    void EvaluateAction(DAction da)
    {
        float units = 1;
        switch (da)
        {
            case FOOD:
                units = r.nextInt(5) + 2;
                units *= t_starvingModifier;
                units *= hoursPerAction;
                Adjust(Stat.FOOD, units);
                Log(da.text+": Found " + Stringify(units) + " units of food.", LogType.INFO);
                events.add(new Event(EventType.PICKING_BERRIES, 10.f));
                break;
            case MATERIALS:
                units = r.nextInt(5) + 2;
                units *= t_starvingModifier;
                units *= hoursPerAction;
                Log(da.text+": Found " + Stringify(units) + " units of materials.", LogType.INFO);
                Adjust(Stat.MATERIALS, units);
                break;
            case SCOUT:
                Scout();
                break;
            case RECOVER:
                units = hoursPerAction * 0.5f;
                units *= t_starvingModifier;
                Adjust(Stat.HP, units);
                Log(da.text+": Recovered "+Stringify(units)+" HP.", LogType.INFO);
                break;
            case BUILD_DEF:
                BuildDefenses();
                break;
            default:
                System.out.println("Nooo");
        }
    }
    void Scout()
    {
        int speed = Speed();
        // Randomize something each hour? Many things with speed?
        float toRandom = 1 + speed * hoursPerAction;
        toRandom *= t_starvingModifier;
        String s = da.text+": While scouting the area, you ";
        Map<Finding, Integer> chances = new HashMap<Finding, Integer>();
        chances.put(Finding.Encounter, 4);
        chances.put(Finding.Nothing, 5);
        chances.put(Finding.Food, 4);
        chances.put(Finding.Materials, 3);
        chances.put(Finding.AbandonedShelter, 2);
        chances.put(Finding.RandomPlayerShelter, 1);
        chances.put(Finding.EnemyStronghold, 1);
        int sumChances = 0;
        for (int i = 0; i < chances.size(); ++i)
        {
            sumChances += (Integer) chances.values().toArray()[i];
        }
        System.out.println("Sum chances: "+sumChances);
        List<Finding> foundList = new ArrayList<Finding>();
        while(toRandom > 0)
        {
            toRandom -= 1;
            float chance = r.nextFloat() * sumChances;
            for (int i = 0; i < chances.size(); ++i)
            {
                int step = (Integer) chances.values().toArray()[i];
                chance -= step;
                if (chance < 0) // Found it
                {
                    foundList.add((Finding)chances.keySet().toArray()[i]);
                }
            }
        }
        float amount = 0;
        int numFoodStashes = 0, numMatStashes = 0,  numEncounters = 0, numAbShelters = 0, numRPS = 0, numEnStrong = 0;
        float foodFound = 0, matFound = 0;
        for (int i = 0; i < foundList.size(); ++i)
        {
            Finding f = foundList.get(i);
            switch(f)            // Evaluate it.
            {
                case Nothing: break;
                case Encounter: numEncounters  += 1; break;
                case Food: foodFound += 1 + r.nextFloat() * 2; break;
                case Materials: matFound = 1 + r.nextFloat() * 2; break;
                case AbandonedShelter: numAbShelters += 1; break;
                case RandomPlayerShelter: numRPS += 1; break;
                case EnemyStronghold: numEnStrong += 1; break;
                default: s += "\n Not implemented: "+f.toString(); break;
            }
        }
        /// Check config for preferred display verbosity of the search results?
        s += numEncounters == 1? "\n- encounter a group of monsters from the Evergreen" : numEncounters > 1? "\n encounter "+numEncounters+" groups of monsters" : "";
        Adjust(Stat.ENCOUNTERS, numEncounters);
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
        Adjust(Stat.MATERIALS, -hoursPerAction * 0.5f); // Consume! 1 unit of materials per hour?
        GenerateEmissionsFromMaterialsConsumption(hoursPerAction * 0.5f); // Generate emissions for building.
        float progress = 1 + r.nextFloat() * 5;
        progress /= Get(Stat.SHELTER_DEFENSE);
        progress *= t_starvingModifier;
        CalcMaterialModifier();
        progress *= t_materialModifier;
        progress *= hoursPerAction;
        Adjust(Stat.SHELTER_DEFENSE_PROGRESS, progress);
        float requiredToNext = Get(Stat.SHELTER_DEFENSE) * 10;
        Log(da.text+": Shelter defense progress increased by " + Stringify(progress) + " units. Progress is now at " + Stringify(Get(Stat.SHELTER_DEFENSE_PROGRESS))+" units out of "+requiredToNext+".", LogType.INFO);
        if (Get(Stat.SHELTER_DEFENSE_PROGRESS) >= requiredToNext)
        {
            Adjust(Stat.SHELTER_DEFENSE_PROGRESS, -requiredToNext);
            Adjust(Stat.SHELTER_DEFENSE, 1);
            Log("Shelter defense reached level "+GetInt(Stat.SHELTER_DEFENSE)+"!", LogType.PROGRESS);
        }
    }

    // Generates emissions, taking into consideration material consumption?
    private void GenerateEmissionsFromMaterialsConsumption(float baseEmissionsToBeGenerated)
    {
        float e = baseEmissionsToBeGenerated;
        int lMatEfficiency = Get(Skill.MaterialEfficiency).Level();
        switch(lMatEfficiency)
        {
            case 4: e *= 0.6f; // 0.3,   40,  Multiplicative effects for each timer :)
            case 3: e *= 0.7f; // 0.5,   20,
            case 2: e *= 0.8f; // 0.72,   10,
            case 1: e *= 0.9f; break; // 0.9,   5,
            default:
            case 0: break;
        }
        Adjust(Stat.EMISSIONS, e);
    }

    private Skill Get(Skill skillType) {
        int index = skillType.ordinal();
        return skills.get(index);
    }

    // To be called after taking required materials. Will calculate material modifier for giving penalties on material debts.
    void CalcMaterialModifier()
    {
        t_materialModifier = (Get(Stat.MATERIALS) < 0 ?  (1 / (1 + Math.abs(Get(Stat.MATERIALS)))): 1);
        if (t_materialModifier < 1) // IF negative, add warning message and reset to 0 - cannot go further negative.
        {
            Log("A lack of materials reduced progress.", LogType.PROBLEM_NOTIFICATION);
            SetInt(Stat.MATERIALS, 0); // Reset materials to 0.
        }
    }
}
