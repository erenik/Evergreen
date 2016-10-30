package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    EMISSIONS(0);
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
    int turnSurvived = 0;
    /// Temporary/chaning starving modifier, changes each turn. Default 1, lower when starving.
    float t_starvingModifier = 1;
    float t_materialModifier = 1; // Same as starving, lowers when negative (debt) on action requirements of materials.

    /// List of events to evaluate/process/play-mini-games. Old events are removed from the list.
    List<Event> events = new ArrayList<Event>();
    /// Log of messages for this specific player.
    List<Log> log = new ArrayList<Log>();

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
    }
    /// Adjusts stats, generates events based on chosen actions to be played, logged
    void NextDay() throws NotAliveException
    {
        if (GetInt(Stat.HP) <= 0)
            throw new NotAliveException();
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
        // Generate events?

        // Attacks of the evergreen?

        if (IsAlive())
            NewDay();
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
    float relativeDuration = 1.f;
    void EvaluateActions()
    {
        t_starvingModifier = GetInt(Stat.HP) >= 0? 1.0f : 1 / (1 + Math.abs(Get(Stat.HP)) * 0.5f);
        relativeDuration = (1.f / (dailyActions.size()));
        System.out.println("relDur: "+relativeDuration);
        for (int i = 0; i < dailyActions.size(); ++i)
        {
            DAction da = dailyActions.get(i);
            EvaluateAction(da);
        }
    }
    void EvaluateAction(DAction da)
    {
        float units = 1;
        switch (da)
        {
            case FOOD:
            {
                units = r.nextInt(5) + 2;
                units *= t_starvingModifier;
                units *= relativeDuration;
                Adjust(Stat.FOOD, units);
                Log("Found " + Stringify(units) + " units of food.", LogType.INFO);
                events.add(new Event(EventType.PICKING_BERRIES, 10.f));
                break;
            }
            case MATERIALS:
                units = r.nextInt(5) + 2;
                units *= t_starvingModifier;
                units *= relativeDuration;
                Log("Found " + Stringify(units) + " units of materials.", LogType.INFO);
                Adjust(Stat.MATERIALS, units);
                break;
            case SCOUT:
                Scout();
                break;
            case RECOVER:
                units = 2;
                units *= t_starvingModifier;
                units *= relativeDuration;
                Adjust(Stat.HP, units);
                Log("Recovered "+Stringify(units)+" HP.", LogType.INFO);
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
        // Randomize.
        float chance = r.nextFloat() * 5;
        System.out.println("So Random!!!");
        // Find encounter

    }
    void BuildDefenses()
    {
        Adjust(Stat.MATERIALS, -2); // Consume!
        float progress = 5 / Get(Stat.SHELTER_DEFENSE);
        progress *= t_starvingModifier;
        CalcMaterialModifier();
        progress *= t_materialModifier;
        progress *= relativeDuration;
        Adjust(Stat.SHELTER_DEFENSE_PROGRESS, progress);
        float requiredToNext = Get(Stat.SHELTER_DEFENSE) * 10;
        Log("Building shelter defense progress increased by " + Stringify(progress) + " units, Now at " + Stringify(Get(Stat.SHELTER_DEFENSE_PROGRESS)), LogType.INFO);
        if (Get(Stat.SHELTER_DEFENSE_PROGRESS) >= requiredToNext)
        {
            Adjust(Stat.SHELTER_DEFENSE_PROGRESS, -requiredToNext);
            Adjust(Stat.SHELTER_DEFENSE, 1);
            Log("Shelter defense reached level "+GetInt(Stat.SHELTER_DEFENSE)+"!", LogType.PROGRESS);
        }
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
