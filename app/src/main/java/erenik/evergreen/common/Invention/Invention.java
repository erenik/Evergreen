package erenik.evergreen.common.Invention;

import java.util.Random;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import erenik.evergreen.util.Dice;


/**
 * Created by Emil on 2016-11-01.
 */
public class Invention implements Serializable
{
    /// Level of quality/ranking of the invented item. This will grant some bonuses. From 0 to 5ish?
    public String name = "NoName";
    public InventionType type;
    int inventionSubType = -1; // Used for weapons only, to remember which type was generated? Used for the iterative/upgrade invention feature as well.
    int additionalEffect = -1; // Used for weapons only.
    int[] stats = new int[InventionStat.values().length];

    /// Temporary stats, saved into stats-array later.
    int atkDmgDiceType = 6, atkDmgDice = 1, attackBonus = 0, atkDmgBonus = 0, qualityLevel = Get(InventionStat.QualityLevel),
            defenseBonus = 0, parryBonus = 0, bonusAttacksPerTurn = 0;
    int additionalEffectDice = 0;
    // For ranged gear.
    int rangedAttackBonus = 0, rangedAtkDmgBonus = 0;
    int harvestBonus = 0, scavBonus = 0, recoBonus = 0, constBonus = 0, inventBonus = 0, scoutBonus = 0;

    public Invention(InventionType type)
    {
        SetDefaults();
        // Just copy type.
        this.type = type;
        Set(InventionStat.Type, type.ordinal()); // Set this.
    }
    public Invention(Invention inv)
    {
        type = inv.type;
        name = inv.name;
        // Copy over all stats?
        for (int i = 0; i < InventionStat.values().length; ++i)
            stats[i] = inv.stats[i];
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.writeObject(name);
        out.writeObject(stats);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        name = (String) in.readObject();
        stats = (int[]) in.readObject();
        type = InventionType.values()[Get(InventionStat.Type)]; // Assign type after reading from stream.
    }
    private void readObjectNoData() throws ObjectStreamException
    {

    }

    public boolean IsCraftable()
    {
        return type.IsCraftable();
    }
    void SetDefaults()
    {
        for (int i = 0; i < InventionStat.values().length; ++i)
        {
            stats[i] = InventionStat.values()[i].defaultValue;
        }
    }
    public static Invention Random(int qualityLevel) {
        int roll = Dice.RollD3(1);
        switch (roll){
            case 0: return RandomWeapon(qualityLevel);
            case 1: return RandomArmor(qualityLevel);
            case 2: return RandomTool(qualityLevel);
        }
        return null;
    }

    public static Invention RandomWeapon(int qualityLevel) {
        Invention weap = new Invention(InventionType.Weapon);
        weap.Set(InventionStat.QualityLevel, qualityLevel);
        weap.RandomizeDetails();
        return weap;
    }
    public static Invention RandomArmor(int qualityLevel) {
        Invention armor = new Invention(InventionType.Armor);
        armor.Set(InventionStat.QualityLevel, qualityLevel);
        armor.RandomizeDetails();
        return armor;
    }
    public static Invention RandomTool(int qualityLevel) {
        Invention tool = new Invention(InventionType.Tool);
        tool.Set(InventionStat.QualityLevel, qualityLevel);
        tool.RandomizeDetails();
        return tool;
    }

    public int Get(InventionStat stat)
    {
        return stats[stat.ordinal()];
    }
    public void Set(InventionStat stat, int value)
    {
        stats[stat.ordinal()] = value;
    }

    public int RandomizeSubType()
    {
        switch(this.type)
        {
            case Weapon:
                Random r = new Random(System.nanoTime());
                int rr = r.nextInt(WeaponType.values().length+1) % WeaponType.values().length; // Keep it within.
                Set(InventionStat.SubType, rr);
                break;
        }
        return Get(InventionStat.SubType);
    }
    /// Randomizes details based on current stats.
    public void RandomizeDetails() {
        // Fetch all details from the array? Just quality level?
        qualityLevel = Get(InventionStat.QualityLevel);
        inventionSubType = Get(InventionStat.SubType);
        type = InventionType.values()[Get(InventionStat.Type)];
        switch(this.type)
        {
            case Weapon: {
                // Randomize weapon type? Stats vary with weapon type instead?
                UpdateWeaponStats();
                if (additionalEffect > 0)
                    Set(InventionStat.AdditionalEffect, Dice.RollD6(1));
                UpdateWeaponAdditionalEffect();
                // Additional stuffs.
                int lastRoll = -1;
                break;
            }
            case Armor:
                name = "Body Armor";
                switch(qualityLevel) {
                    case 0:
                        name = "Crude " + name;
                        defenseBonus += 1;
                        break;
                    case 1:
                        name = "Rough " + name;
                        defenseBonus += 2;
                        break;
                    case 2:
                        defenseBonus += 3;
                        break;
                    case 3: // Polished?
                        name = "Refined " + name;
                        defenseBonus += 4;
                        break;
                }
                break;
            case RangedWeapon:
                // Add different ranged weapons later?
                atkDmgDiceType = 3;
                atkDmgDice = 2;// 2D3?
                name = "Bow";
                rangedAttackBonus += 1 + qualityLevel;
                rangedAtkDmgBonus += 1 + qualityLevel * 1.5;
                break;
            case Tool:
                int diceRoll = Dice.RollD6(1) - 1;
                switch(diceRoll) {
                    default:
                    case 0:
                        harvestBonus += 2 + qualityLevel;
                        name = "Harvester kit";
                        break;
                    case 1:
                        scavBonus += 2 + qualityLevel;
                        name = "Scavenging kit";
                        break;
                    case 2:
                        recoBonus += 2 + qualityLevel;
                        name = "First aid kit";
                        break;
                    case 3:
                        constBonus += 2 + qualityLevel;
                        name = "Construction tools kit";
                        break;
                    case 4:
                        inventBonus += 2 + qualityLevel;
                        name = "Inventor's kit";
                        break;
                    case 5:
                        scoutBonus += 2 + qualityLevel;
                        name = "Scout's kit";
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
        if (qualityLevel > 0 && type != InventionType.Weapon) {
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
        Set(InventionStat.ScavengingBonus, scavBonus); // Material searching
        Set(InventionStat.RecoveryBonus, recoBonus);
        Set(InventionStat.ConstructionBonus, constBonus);
        Set(InventionStat.InventingBonus, inventBonus);
        Set(InventionStat.ScoutingBonus, scoutBonus);

    }

    public void UpdateWeaponStats()
    {
        WeaponType wt = WeaponType.values()[Get(InventionStat.SubType)];
        switch (wt) {
            // Knives n daggers.
            case Knife:
                name = "Knife";
                attackBonus += 3;
                atkDmgBonus += 1;
                atkDmgDiceType = 3;
                bonusAttacksPerTurn += 1;
                break;
            case Dagger:
                name = "Dagger";
                attackBonus += 4;
                atkDmgBonus += 2;
                atkDmgDiceType = 3;
                bonusAttacksPerTurn += 1;
                break;
            default:
            // Sword-type weapons
            case Sword:
                name = "Gladius";
                attackBonus += 5;
                atkDmgBonus += 4;
                parryBonus += 1 + qualityLevel / 2;
                break;
            case Longsword:
                name = "Longsword";
                attackBonus += 4;
                atkDmgBonus += 6;
                parryBonus += 2 + qualityLevel / 5;
                break;
            /// Club-type weapons.
            case Club:
                name = "Club";
                attackBonus += 3;
                atkDmgBonus += 2;
                atkDmgDiceType = 10;
                break;
            case GreatClub:
                name = "Heavy club";
                attackBonus += 2;
                atkDmgBonus += 4;
                atkDmgDiceType = 10;
                break;
            case Sledgehammer:
                name = "Sledgehammer";
                attackBonus += 1;
                atkDmgBonus += 7;
                atkDmgDiceType = 10;
                break;
            // Axe-type weapons
            case Axe:
                name = "Axe";
                attackBonus += 6;
                atkDmgBonus += 4;
                atkDmgDiceType = 8;
                break;
            case GreatAxe:
                name = "Great Axe";
                attackBonus += 5;
                atkDmgBonus += 6;
                atkDmgDiceType = 8;
                break;
        }
        switch (qualityLevel) {
            case 0:
                name = "Crude " + name;
                attackBonus -= 2;
                atkDmgBonus -= 2;
                break;
            case 1:
                name = "Rough " + name;
                attackBonus -= 2;
            case 2:
                break;
            case 3: // Polished?
                name = "Refined " + name;
                attackBonus += 1;
                atkDmgBonus += 1;
                break;
            case 4:
                attackBonus += 2;
                atkDmgBonus += 2;
                additionalEffectDice = 1;
                break;
            case 5:
                attackBonus += 3;
                atkDmgBonus += 3;
                additionalEffectDice = 1;
                bonusAttacksPerTurn += 1;
            case 6:
            default:
                attackBonus += 4;
                atkDmgBonus += 4;
                additionalEffectDice = 1;
                bonusAttacksPerTurn += 2;
                break;
        }
        if (atkDmgBonus < 0) // Minimum +0 bonus, no negatives.
            atkDmgBonus = 1;
    }
    public void UpdateWeaponAdditionalEffect() {
        switch (Get(InventionStat.AdditionalEffect)) {
            case 0:
                defenseBonus += 3;
                parryBonus += 1;
                name = "Defender's " + name;
                break;
            case 1:
                attackBonus += 4;
                atkDmgBonus += 2;
                name = "Warrior's " + name;
                break;
            case 2:
                atkDmgBonus += 5;
                attackBonus += 2;
                name = "Berserker's " + name;
                break;
            case 3:
                parryBonus += 2;
                defenseBonus += 1;
                attackBonus += 1;
                name = "Duelist's " + name;
                break;
            case 4:
                bonusAttacksPerTurn = 1;
                attackBonus -= 1;
                atkDmgBonus -= 2;
                name = "Swift" + name;
                break;
            default:
                // Bad value, no additional effect.
                break;
        }
    }
    // Adjuster.
    public void Adjust(InventionStat stat, int val)
    {
        stats[stat.ordinal()] += val;
    }

    void FetchStatsFromArray() {
        atkDmgDice = Get(InventionStat.AttackDamageDice);
        atkDmgDiceType = Get(InventionStat.AttackDamageDiceType);
        atkDmgBonus = Get(InventionStat.AttackDamageBonus);
    }

    /// From one hit.
    public int MinimumDamage() {
        FetchStatsFromArray();
        return atkDmgDice * 1 + atkDmgBonus;
    }
    public int MaximumDamage() {
        FetchStatsFromArray();
        return atkDmgDice * atkDmgDiceType + atkDmgBonus;
    }

}
