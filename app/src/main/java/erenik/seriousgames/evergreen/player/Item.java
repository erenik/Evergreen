package erenik.seriousgames.evergreen.player;

import erenik.seriousgames.evergreen.util.Dice;

/**
 * Created by Emil on 2016-11-01.
 */
public enum Item
{
    Weapon,
    Armor,
    RangedWeapon;

    // Weapon stats.
    int attackBonus = 0, defenseBonus = 0, bonusAttacksPerTurn = 0, damageBonus = 0;
    Item() {
    }
    static Item RandomWeapon(int bonus)
    {
        Item weap = Item.Weapon;
        weap.attackBonus = Dice.RollD3(2);
        weap.damageBonus = Dice.RollD6(1);
        return weap;
    }

}
