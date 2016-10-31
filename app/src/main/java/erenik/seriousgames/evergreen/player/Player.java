package erenik.seriousgames.evergreen.player;

import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import erenik.seriousgames.evergreen.App;
import erenik.seriousgames.evergreen.player.*;
import erenik.seriousgames.evergreen.Event;
import erenik.seriousgames.evergreen.logging.*;
import erenik.seriousgames.evergreen.act.EventDialogFragment;
import erenik.seriousgames.evergreen.combat.Combatable;
import erenik.seriousgames.evergreen.util.Dice;


;


/**
 * Created by Emil on 2016-10-25.
 */
public class Player extends Combatable
{
    static Random r = new Random(System.nanoTime());
    // Main stats.
//    float hp, food, materials, base_attack, base_defense, emissions;
    float[] statArr = new float[Stat.values().length];
    /// Used in main menu, and also saved.
    public List<DAction> dailyActions = new ArrayList<DAction>();
    public int skill = -1;
    public int activeAction = -1;
    /// Increment every passing day. Stop once dying.
    public int turn = 1;
    /// Temporary/chaning starving modifier, changes each turn. Default 1, lower when starving.
    float t_starvingModifier = 1;
    float t_materialModifier = 1; // Same as starving, lowers when negative (debt) on action requirements of materials.

    /// List of events to evaluate/process/play-mini-games. Old events are removed from the list.
    List<Event> events = new ArrayList<Event>();
    /// Log of messages for this specific player.
    public List<Log> log = new ArrayList<Log>();

    /// Array of exp in each Skill.
    List<Skill> skills =  new ArrayList<Skill>(Arrays.asList(Skill.values()));
    /// Queued skills to be leveled up.
    List<Skill> skillTrainingQueue = new ArrayList<Skill>();

    // Auto-created. Start-using whenever.
    static private Player player = new Player();
    static public Player getSingleton()
    {
        return player;
    }
    private Player()
    {
        name = "Parlais Haux Le'deur";
        isPlayer = true;
        SetDefaultStats();
    }
    public boolean IsAlive()
    {
        return GetInt(Stat.HP) > 0 && hp > 0;
    }
    int Speed()
    {
        return GetInt(Stat.SPEED);
    }
    public int Attack()
    {
        return GetInt(Stat.BASE_ATTACK) + GetInt(Stat.ATTACK_BONUS);
    }
    public int Defense()
    {
        return GetInt(Stat.BASE_DEFENSE) + GetInt(Stat.DEFENSE_BONUS);
    }
    int ShelterDefense()
    {
        return Defense() + GetInt(Stat.SHELTER_DEFENSE);
    }
    public void SetDefaultStats()
    {
        // Default stats?
        for (int i = 0; i < Stat.values().length; ++i)
            statArr[i] = Stat.values()[i].defaultValue;
        for (int i = 0; i < skills.size(); ++i) // Reset EXP in each skill?
            skills.get(i).setTotalEXP(0);
    }
    public void Adjust(Stat s, float amount)
    {
        statArr[s.ordinal()] += amount;
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
    public boolean SaveLocally()
    {
        SharedPreferences sp = App.GetPreferences();
        SharedPreferences.Editor e = sp.edit();
        // Put.
        e.putString(Constants.NAME, name);
        e.putBoolean(Constants.SAVE_EXISTS, true);
        e.putInt(Constants.TURN, turn);
        // Stats
        System.out.println("Saving stats");
        for (int i = 0; i < Stat.values().length; ++i)
        {
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
        turn = sp.getInt(Constants.TURN, 0);
        // Stats
        System.out.println("Loading stats");
        for (int i = 0; i < Stat.values().length; ++i)
        {
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
        ++player.turn;
        System.out.println("Turn: " + player.turn);
    }
    /// Adjusts stats, generates events based on chosen actions to be played, logged
    public void NextDay() throws NotAliveException
    {
        NewDay();  // New day :3
        if (GetInt(Stat.HP) <= 0)
        {
            App.GameOver();
            return;
        }
        Log("-- Day "+player.turn+" --", LogType.INFO);
        // Yeah.
        Adjust(Stat.FOOD, -2);
        if (GetInt(Stat.FOOD) >= 0) {
            Adjust(Stat.HP, 1);
            ClampHP();
        }
        else {
            float loss = Get(Stat.FOOD) / 5;
            Adjust(Stat.HP, loss);
            Log("Starving, lost "+(-loss)+" HP.", LogType.ATTACKED);
            if (!IsAlive())
                return;
        }
        // Analyze some chosen hours of activity. Generate events and stuff for each?
        EvaluateActions();
        /// Force the player to play through the generated events before they proceed.
//        HandleGeneratedEvents();

        // Attacks of the evergreen?
        int everGreenTurn = turn % 16;
        switch(everGreenTurn)
        {
            default: break;
            case 0: case 6:case 10:case 13: case 15: // The pattern repeats every 16 turns.
                Adjust(Stat.ATTACKS_OF_THE_EVERGREEN, 1);
                break;
        }
    }

    public void ClampHP()
    {
        float hp = Get(Stat.HP);
        if (hp > Get(Stat.MAX_HP))
            hp = Get(Stat.MAX_HP);
        else if (hp < 0)
            hp = 0;
        Set(Stat.HP, hp);
    }

    // Present dialogue-box for handled events.
    public void HandleGeneratedEvents(FragmentManager fragMan)
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
    public boolean AllMandatoryEventsHandled()
    {
        if (Get(Stat.ENCOUNTERS) > 0)
            return false;
        if (Get(Stat.ATTACKS_OF_THE_EVERGREEN) > 0)
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
//        System.out.println("hoursPerAction: "+hoursPerAction);
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
                Log(da.text + ": Found " + Stringify(units) + " units of food.", LogType.INFO);
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
//        System.out.println("Sum chances: "+sumChances);
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
                case Food: numFoodStashes += 1; foodFound += 1 + r.nextFloat() * 2; break;
                case Materials: numMatStashes += 1; matFound = 1 + r.nextFloat() * 2; break;
                case AbandonedShelter: numAbShelters += 1; break;
                case RandomPlayerShelter: numRPS += 1; break;
                case EnemyStronghold: numEnStrong += 1; break;
                default: s += "\n Not implemented: "+f.toString(); break;
            }
        }
        /// Check config for preferred display verbosity of the search results?
        s += numEncounters == 1? "\n- encounter a group of monsters from the Evergreen" : numEncounters > 1? "\n- encounter "+numEncounters+" groups of monsters" : "";
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

    public void GainEXP(int expGained)
    {
        // Check queued skills.
        int xp = expGained;
        while (xp > 0 && skillTrainingQueue.size() > 0) {
            Skill next = skillTrainingQueue.get(0);
            next.ordinal();
            Skill toSkillUp = skills.get(next.ordinal());
            int needed = toSkillUp.EXPToNext();
            int toGain = xp;
            if (needed < xp)
                toGain = needed;
            xp -= toGain;
            int levelReached = toSkillUp.GainExp(toGain);
            if (levelReached >= 0) {
                Log("Skill " + toSkillUp.text + " reached level " + levelReached + "!", LogType.EXP);
                skillTrainingQueue.remove(0);
            }
        }
        // If queue empty, place in unallocated points.
        Adjust(Stat.UNALLOCATED_EXP, xp);
    }

    public void PrepareForCombat() {
        // Load data into the Combatable variables from the more persistant ones saved here.
        Combatable c = (Combatable) this;
        c.attack = Attack();
        c.defense = Defense();
        c.hp = GetInt(Stat.HP);
        c.maxHP = GetInt(Stat.MAX_HP);
        c.attackDamage = new Dice(6, 1, 0);
    }
}
