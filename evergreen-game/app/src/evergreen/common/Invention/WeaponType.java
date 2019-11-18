package evergreen.common.Invention;

import evergreen.util.EList;
import java.util.Random;

/**
 * Created by Emil on 2016-11-07.
 */
public enum WeaponType {
    Knife(false),
    Dagger(false),
    Sword(false),
    Longsword(false),
    Axe(false),
    GreatAxe(false),
    Club(false),
    GreatClub(false),
    Sledgehammer(false),
    // Ranged weapons/
    Boomerang(true),
    ShortBow(true),
    LongBow(true),
    Crossbow(true)
    ;
    WeaponType(boolean isRanged){
        this.isRanged = isRanged;
    }
    /// 0 for false, 1 for Yes
    boolean isRanged;

    // For the random-functions.
    static private Random weaponTypeRand = new Random(System.currentTimeMillis());

    /// Assumed to be non-ranged.
    public static int Random() {
        EList<WeaponType> nonRangedTypes = new EList<>();
        for (int i = 0; i < WeaponType.values().length; ++i){
            WeaponType t = WeaponType.values()[i];
            if (t.isRanged)
                nonRangedTypes.add(t);
        }
        return weaponTypeRand.nextInt(nonRangedTypes.size()+1) % nonRangedTypes.size();
    }

    public static int RandomRanged() {
        EList<WeaponType> rangedTypes = new EList<>();
        for (int i = 0; i < WeaponType.values().length; ++i){
            WeaponType t = WeaponType.values()[i];
            if (t.isRanged)
                rangedTypes.add(t);
        }
        return weaponTypeRand.nextInt(rangedTypes.size()+1) % rangedTypes.size();
    }
};
