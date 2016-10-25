package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

enum Stat
{
    HP(10), MAX_HP(10),
    FOOD(5),
    MATERIALS(3),
    BASE_ATTACK(10), ATTACK_BONUS(0),
    BASE_DEFENSE(10), DEFENSE_BONUS(0), SHELTER_DEFENSE(1),
    EMISSIONS(0);
    Stat(float defaultValue)
    {
        this.defaultValue = defaultValue;
    }
    float defaultValue;
};

/**
 * Created by Emil on 2016-10-25.
 */
public class Player
{
    // Main stats.
//    float hp, food, materials, base_attack, base_defense, emissions;
    String name;
    float[] statArr = new float[Stat.values().length];
    /// Used in main menu, and also saved.
    int dailyAction = -1;
    int skill = -1;
    int activeAction = -1;

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
}
