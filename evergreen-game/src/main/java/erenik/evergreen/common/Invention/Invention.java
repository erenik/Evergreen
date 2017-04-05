package erenik.evergreen.common.Invention;

import java.util.Random;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import erenik.util.Dice;

import static erenik.evergreen.common.Invention.InventionStat.Blueprint;
import static erenik.evergreen.common.Invention.InventionType.RangedWeapon;


/**
 * Created by Emil on 2016-11-01.
 */
public class Invention implements Serializable {
    /// Level of quality/ranking of the invented item. This will grant some bonuses. From 0 to 5ish?
    public String name = "NoName";
    public InventionType type;
    int inventionSubType = -1; // Used for weapons only, to remember which type was generated? Used for the iterative/upgrade invention feature as well.
    int additionalEffect = -1; // Used for weapons only.
    /// Returns the item ID for crafted items, should be the same on client and server-side, so can be used for equipping/using items easily.
    public long GetID(){ return itemID; };
    private long itemID = -1; // Created incrementally for new items. Should be entirely unique!
    static private long itemIDenumerator = 0; // Used on server-side mainly.
    int[] stats = new int[InventionStat.values().length];

    /// Temporary stats, saved into stats-array later.
    int qualityLevel = Get(InventionStat.QualityLevel), // Quality level will mainly vary..
            defenseBonus = 0, parryBonus = 0, bonusAttacksPerTurn = 0;
    int additionalEffectDice = 0;
    // For ranged gear.
    int rangedAttackBonus = 0, rangedAtkDmgBonus = 0;
    int harvestBonus = 0, scavBonus = 0, recoBonus = 0, constBonus = 0, inventBonus = 0, scoutBonus = 0;

    protected Invention(InventionType type) {
        SetDefaults();
        // Just copy type.
        this.type = type;
        Set(InventionStat.Type, type.ordinal()); // Set this.
    }
    protected Invention(Invention inv) {
        type = inv.type;
        name = inv.name;
        // Copy over all stats?
        for (int i = 0; i < InventionStat.values().length; ++i)
            stats[i] = inv.stats[i];
    }

    public static Invention CreateBlueprint(InventionType type){
        return NewBlueprint(type);
    }

    // Make this object into a blueprint (flag 1 variable).
    public Invention Blueprint(){
        Set(Blueprint, 1);
        return this;
    }

    public static Invention NewBlueprint(InventionType inventionType) {
        switch (inventionType){
            case RangedWeapon:
            case Weapon:
                return new Weapon().Blueprint();
            default:
                return new Invention(inventionType).Blueprint();
        }
    }
    public static Invention Craft(Invention invention){
        switch (invention.type){
            case RangedWeapon:
            case Weapon:
                return new Weapon().Blueprint();
            default:
                return new Invention(invention.type).Blueprint();
        }
    }
    public static Invention MakeBlueprint(Invention invention) {
        return Craft(invention).Blueprint();
    }
    public Invention CraftInventionFromBlueprint() {
        Invention inv = Invention.Craft(this);
        inv.itemID = ++itemIDenumerator;
        // Copy over stats?
        inv.name = name;
        for (int i = 0; i < InventionStat.values().length; ++i)
            inv.stats[i] = this.stats[i];
        return inv;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//        System.out.println("Invention writeObject");
        writeObjectToStream(out);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
  //      System.out.println("Invention readObject");
        readObjectFromStream(in);
    }
    protected void writeObjectToStream(java.io.ObjectOutputStream out) throws IOException{
        out.writeObject(name);
        out.writeObject(stats);
        out.writeLong(itemID);
    }
    protected void readObjectFromStream(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = (String) in.readObject();
        stats = (int[]) in.readObject();
        itemID = in.readLong();
        type = InventionType.values()[Get(InventionStat.Type)]; // Assign type after reading from stream.
    }


    private void readObjectNoData() throws ObjectStreamException {

    }

    public boolean IsCraftable()
    {
        return type.IsCraftable();
    }
    void SetDefaults() {
        for (int i = 0; i < InventionStat.values().length; ++i) {
            stats[i] = InventionStat.values()[i].defaultValue;
        }
    }
    public static Invention Random(int qualityLevel) {
        int roll = Dice.RollD3(1);
        switch (roll){
            case 0: return RandomWeapon(qualityLevel);
            case 1: return RandomArmor(qualityLevel);
            case 2: return RandomTool(qualityLevel);
            default:
                return RandomWeapon(qualityLevel);
        }
    }

    public static Invention RandomWeapon(int qualityLevel) {
        Invention weap = new Invention(InventionType.Weapon);
        weap = Invention.Craft(weap);
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
    public void Set(InventionStat stat, int value) {
        stats[stat.ordinal()] = value;
    }

    public int RandomizeSubType() {
        Random r = new Random(System.nanoTime());
        switch(this.type) {
            case Weapon:
                inventionSubType = WeaponType.Random();; // Keep it within.
                break;
            case RangedWeapon:
                inventionSubType = WeaponType.RandomRanged();
                break;
            case Tool:
                inventionSubType = r.nextInt(ToolType.values().length+1) % ToolType.values().length; // Keep it yeaahhhh.
                break;
            case Armor:
                inventionSubType = r.nextInt(ArmorType.values().length+1) % ArmorType.values().length;
                break;
            default:
                System.out.println("Bad type, cannot determine sub-type. Type: "+this.type.name());
                System.exit(4);
        }
        Set(InventionStat.SubType, inventionSubType);
        System.out.println("Sub-type set to: "+inventionSubType+" type: "+this.type);
        UpdateDetails();
        return inventionSubType;
    }
    /// Randomizes details based on current stats.
    public void RandomizeDetails() {
        // Fetch all details from the array? Just quality level?
        qualityLevel = Get(InventionStat.QualityLevel);
        inventionSubType = Get(InventionStat.SubType);
        type = InventionType.values()[Get(InventionStat.Type)];
        if (inventionSubType == -1)
            RandomizeSubType();
        /// Updates details based on quality, type, subtype, etc.
        UpdateDetails();
    }

    // Adjuster.
    public void Adjust(InventionStat stat, int val)
    {
        stats[stat.ordinal()] += val;
    }


    /// Updates all stats, based on provided quality level, type, subtype and possibly sub-sub-type.
    public void UpdateDetails() {
        System.out.println("Updating deatils.");
        switch(this.type) {
            case RangedWeapon:
            case Weapon: {
                // Randomize weapon type? Stats vary with weapon type instead?
                System.out.println("Shouldn't be here, subclass should handle this, type: "+this.type);
                new Exception().printStackTrace();
                System.exit(3);
                return; // All should already be finalized for the weapon now.
            }
            case Armor:
                ArmorType armorType = ArmorType.values()[inventionSubType];
                switch (armorType){
                    case Head: name = "Helmet"; break;
                    case Body: name = "Body Armor"; break;
                    case Hands: name = "Gloves"; break;
                    case Legs: name = "Breeches"; break;
                    case Feet: name = "Greaves"; break;
                }
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
                        name = name+" +1";
                        defenseBonus += 4;
                        break;
                    default:
                        name = name+" +2";
                        defenseBonus += 5;
                        break;
                }
                break;
            case Tool:
                DetermineToolDetails();
                break;
            /*
            case ShelterAddition:
                // Randomize type?
                Set(InventionStat.GreenhouseProductivity, 2 + qualityLevel);
                name = "Greenhouse";
                break;
                */
            // Vehicle upgrades not used for now.
//            case VehicleUpgrade: Set(InventionStat.Catalyst, Dice.RollD3(1) + qualityLevel);name = "Catalyst";break;
            default:
                System.out.println("Lacking code in Invention.RandomizeDetails");
                System.exit(5);
                break;
        }
        /// Add quality bonuses as numbers to the name to make it obvious.
        if (qualityLevel > 0 && type != InventionType.Weapon &&
                type != InventionType.Armor && type != RangedWeapon) {
            name = name + " +" + qualityLevel;
        }
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
    private void DetermineToolDetails() {
        ToolType tt = ToolType.values()[inventionSubType];
        switch (tt) {
            case HarvesterKit:
                harvestBonus += 2 + qualityLevel;
                name = "Harvester kit";
                break;
            case ScavengingKit:
                scavBonus += 2 + qualityLevel;
                name = "Scavenging kit";
                break;
            case FirstAidKit:
                recoBonus += 2 + qualityLevel;
                name = "First aid kit";
                break;
            case ConstructionToolsKit:
                constBonus += 2 + qualityLevel;
                name = "Construction tools kit";
                break;
            case InventorsKit:
                inventBonus += 2 + qualityLevel;
                name = "Inventor's kit";
                break;
            case ScoutsKit:
                scoutBonus += 2 + qualityLevel;
                name = "Scout's kit";
                break;
            default:
                System.out.println("BAd");
                System.exit(14);
        }
    }

    public int AttackBonus() {
        return Get(InventionStat.AttackBonus);
    }
}
