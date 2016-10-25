package erenik.seriousgames.evergreen;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Emil on 2016-10-25.
 */
public class Player
{
    // Main stats.
//    float hp, food, materials, base_attack, base_defense, emissions;
    enum Stat
    {
        HP(10),
        FOOD(5),
        MATERIALS(3),
        BASE_ATTACK(10),
        BASE_DEFENSE(10),
        EMISSIONS(0);
        Stat(float defaultValue)
        {
            this.defaultValue = defaultValue;
        }
        float defaultValue;
    };
    String name;
    float[] statArr = new float[Stat.values().length];
    final float DEFAULT_HP = 10, DEFAULT_FOOD = 5, DEFAULT_MATERIALS = 3, DEFAULT_BASE_ATTACK = 10, DEFAULT_BASE_DEFENSE = 10, DEFAULT_EMISSIONS = 0;

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
        for (int i = 0; i < Stat.values().length; ++i)
        {
            System.out.println("Saving "+Stat.values()[i].toString());
            e.putFloat(Stat.values()[i].toString(), statArr[i]);
        }
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
        for (int i = 0; i < Stat.values().length; ++i)
        {
            System.out.println("Saving "+Stat.values()[i].toString());
            statArr[i] = sp.getFloat(Stat.values()[i].toString(), Stat.values()[i].defaultValue);
        }
        return true;
    }
}
