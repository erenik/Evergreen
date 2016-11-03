package erenik.seriousgames.evergreen.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import erenik.seriousgames.evergreen.util.Dice;


/**
 * Created by Emil on 2016-11-01.
 */
public class Invention
{
    /// Level of quality/ranking of the invented item. This will grant some bonuses. From 0 to 5ish?
    public String name;
    InventionType type;
    int[] stats = new int[InventionStat.values().length];

    Invention(InventionType type)
    {
        // Just copy type.
        this.type = type;
        SetDefaults();
    }
    Invention(Invention inv)
    {
        type = inv.type;
        name = inv.name;
        // Copy over all stats?
        SetDefaults();
    }
    void SetDefaults()
    {
        for (int i = 0; i < InventionStat.values().length; ++i)
        {
            stats[i] = InventionStat.values()[i].defaultValue;
        }
    }
    static Invention RandomWeapon(int bonus)
    {
        Invention weap = new Invention(InventionType.Weapon);
        weap.RandomizeDetails();
        return weap;
    }
    int Get(InventionStat stat)
    {
        return stats[stat.ordinal()];
    }
    void Set(InventionStat stat, int value)
    {
        stats[stat.ordinal()] = value;
    }
    void RandomizeDetails()
    {
        int atkDmgDiceType = 6, atkDmgDice = 1, attackBonus = 0, atkDmgBonus = 0, qualityLevel = Get(InventionStat.QualityLevel),
                defenseBonus = 0, parryBonus = 0, bonusAttacksPerTurn = 0;
        // For ranged gear.
        int rangedAttackBonus = 0, rangedAtkDmgBonus = 0;
        int harvestBonus = 0, scavBonus = 0, recoBonus = 0, constBonus = 0, inventBonus = 0, scoutBonus = 0;
        switch(this.type)
        {
            case Weapon: {
                attackBonus = Dice.RollD3(2);
                atkDmgBonus = Dice.RollD6(1);
                // Random name
                if (attackBonus > 6)
                    name = atkDmgBonus >= 4 ? "Great axe" : "Great sword";
                else if (attackBonus > 3)
                    name = atkDmgBonus >= 4 ? "Axe" : "Longsword";
                else            // Low attack bonus, high vs. low damage bonus.
                    name = atkDmgBonus >= 4 ? "Club" : "Dagger";
                // Additional stuffs.
                int dice = Dice.RollD6(2), attackBonusDice = 0;
                for (int i = 0; i < qualityLevel + 1; ++i) {
                    switch (dice) {
                        case 2:
                        case 3:
                            defenseBonus += Dice.RollD3(1);
                            break;
                        case 4:
                        case 5:
                        case 6:
                            attackBonus += Dice.RollD3(1);
                            attackBonusDice += 1;
                            break;
                        case 7:
                        case 8:
                            atkDmgBonus += Dice.RollD3(1);
                            break;
                        case 9:
                        case 10:
                            parryBonus += 1;
                            break;
                        case 11:
                            bonusAttacksPerTurn = 1;
                            attackBonus -= 1;
                            atkDmgBonus -= 2;
                            break;
                        case 12:
                            bonusAttacksPerTurn = 2;
                            attackBonus -= 2;
                            atkDmgBonus -= 4;
                            break;
                    }
                }
                if (parryBonus > 0) name = name + " of Parrying";
                if (bonusAttacksPerTurn >= 2) name = "Swift " + name;
                else if (bonusAttacksPerTurn >= 1) name = "Light " + name;
                if (attackBonusDice > 2) name = "Sharp " + name;
                if (qualityLevel > 0) {
                    attackBonus += (qualityLevel + 1) / 2;
                    atkDmgBonus += qualityLevel / 2;
                }
                break;
            }
            case Armor:
                defenseBonus += Dice.RollD3(1);
                name = "Body Armor";
                break;
            case RangedWeapon:
                atkDmgDiceType = 3;
                atkDmgDice = 2;// 2D3?
                rangedAttackBonus += Dice.RollD3(2) + qualityLevel;
                rangedAtkDmgBonus += Dice.RollD3(1) + qualityLevel;
                name = rangedAttackBonus > 5? "Longbow" : "Bow";
                break;
            case Tool:
                int diceRoll = Dice.RollD6(1);
                switch(diceRoll)
                {
                    default:
                    case 0:
                        harvestBonus += Dice.RollD3(1) + qualityLevel;
                        name = "Harvester kit";
                        break;
                }
                break;
            case ShelterAddition:
                // Randomize type?
                Set(InventionStat.GreenhouseProductivity, Dice.RollD3(1) + qualityLevel);
                name = "Greenhouse";
                break;
            case VehicleUpgrade:
                Set(InventionStat.Catalyst, Dice.RollD3(1) + qualityLevel);
                name = "Catalyst";
                break;
            default:
                System.out.println("Lacking code in Invention.RandomizeDetails");
                System.exit(5);
                break;
        }
        /// Add quality bonuses to the name to make it obvious.
        if (qualityLevel > 0) {
            name = name + " +" + qualityLevel;
        }
        // Save stats accordingly.
        Set(InventionStat.AttackDamageDiceType, atkDmgDiceType);
        Set(InventionStat.AttackDamageDice, atkDmgDice);
        Set(InventionStat.AttackBonus, attackBonus);
        Set(InventionStat.AttackDamageBonus, atkDmgBonus);
        Set(InventionStat.DefenseBonus, defenseBonus);
        Set(InventionStat.BonusAttacks, bonusAttacksPerTurn);
        Set(InventionStat.ParryBonus, parryBonus);
        // Ranged stats
        Set(InventionStat.RangedAttackBonus, rangedAttackBonus);
        Set(InventionStat.RangedDamageBonus, rangedAtkDmgBonus);
        // Tools stats
        Set(InventionStat.HarvestBonus, harvestBonus);
    }
}
