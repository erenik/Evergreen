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

// Daily Action
enum DAction
{
    FOOD("Gather berries"),
    MATERIALS("Gather materials"),
    SCOUT("Scout the area"),
    RECOVER("Recover"),
    BUILD_DEF("Build defenses"),

    /* <item>Build defenses</item>
    <item>Augment transport</item>
    <item>Look for player</item>
    <item>Expedition</item>
    <item>Invent</item>
    <item>Craft</item>
    <item>Steal</item>
    <item>Attack a player</item>
*/
    NONE("None")
    ;
    DAction(String txt)
    {
        this.text = txt;
    }
    String text;
};


enum Stat
{
    HP(10), MAX_HP(10),
    FOOD(5),
    MATERIALS(3),
    BASE_ATTACK(10), ATTACK_BONUS(0),
    BASE_DEFENSE(10), DEFENSE_BONUS(0), SHELTER_DEFENSE(1),
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
    INFO,
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
            case ATTACKED: return R.color.attacked;
            case EVENT: return R.color.event;
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
    int dailyAction = -1;
    int skill = -1;
    int activeAction = -1;
    /// Increment every passing day. Stop once dying.
    int turnSurvived = 0;

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
        e.putInt(Constants.DAILY_ACTION, dailyAction);
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
        dailyAction = sp.getInt(Constants.DAILY_ACTION, -1);
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
            float loss = GetInt(Stat.FOOD);
            Adjust(Stat.HP, loss);
            Log("Starving, lost 1 HP.", LogType.ATTACKED);
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

    void EvaluateActions()
    {
        DAction da = DAction.NONE;
        try {
            da = DAction.values()[dailyAction];
        } catch (Exception e)
        {
            System.out.println(e.toString());
        }
        switch (da)
        {
            case FOOD:
            {
                float units = r.nextInt(5) + 2;
                Adjust(Stat.FOOD, units);
                Log("Found " + units + " units of food.", LogType.INFO);
                events.add(new Event(EventType.PICKING_BERRIES, 10.f));
                break;
            }
            case MATERIALS:
                float units = r.nextInt(5) + 2;
                Log("Found " + units + " units of materials.", LogType.INFO);
                Adjust(Stat.MATERIALS, units);
                break;
            case SCOUT:
                Scout();
                break;
            case RECOVER:
                float rec = 2;
                Adjust(Stat.HP, rec);
                Log("Recovered "+rec+" HP.", LogType.INFO);
                break;
            case BUILD_DEF:
                float progress = 0.5f / Get(Stat.SHELTER_DEFENSE);
                Adjust(Stat.SHELTER_DEFENSE, progress);
                Log("Building shelter defense progressed by "+progress+" units, now at "+Get(Stat.SHELTER_DEFENSE), LogType.INFO);
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
}
