package evergreen.common.Invention;

import java.util.Random;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import evergreen.util.Dice;
import evergreen.util.Printer;

import static evergreen.common.Invention.InventionStat.AttackBonus;
import static evergreen.common.Invention.InventionStat.Blueprint;
import static evergreen.common.Invention.InventionStat.CraftingBonus;
import static evergreen.common.Invention.InventionStat.DefenseBonus;
import static evergreen.common.Invention.InventionStat.Equipped;
import static evergreen.common.Invention.InventionStat.ForagingBonus;
import static evergreen.common.Invention.InventionStat.InventingBonus;
import static evergreen.common.Invention.InventionStat.ParryBonus;
import static evergreen.common.Invention.InventionStat.ScavengingBonus;
import static evergreen.common.Invention.InventionStat.ScoutingBonus;
import static evergreen.common.Invention.InventionStat.SubType;


/**
 * Created by Emil on 2016-11-01.
 */
public class Invention implements Serializable {
    private static final long serialVersionUID = 1L;

    /// Level of quality/ranking of the invented item. This will grant some bonuses. From 0 to 5ish?
    public String name = "NoName";
    public InventionType type;
   // int inventionSubType = -1; // Used for weapons only, to remember which type was generated? Used for the iterative/upgrade invention feature as well.
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
//    int rangedAttackBonus = 0, rangedAtkDmgBonus = 0;
//    int harvestBonus = 0, scavBonus = 0, recoBonus = 0, constBonus = 0, inventBonus = 0, scoutBonus = 0;

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
  //          case RangedWeapon:
            case Weapon:
                return new Weapon().Blueprint();
            default:
                return new Invention(inventionType).Blueprint();
        }
    }
    public static Invention Craft(Invention invention){
        switch (invention.type){
//            case RangedWeapon:
            case Weapon:
                return new Weapon();
            default:
                return new Invention(invention.type);
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
//        Printer.out("Invention writeObject");
        writeObjectToStream(out);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
  //      Printer.out("Invention readObject");
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

    public int Get(InventionStat stat) {
        if (stat.ordinal() >= stats.length) {
            Printer.out("Array index out of bounds for stat! Bad/old version?");
            return -1;
        }
        return stats[stat.ordinal()];
    }
    public void Set(InventionStat stat, int value) {
        stats[stat.ordinal()] = value;
    }

    public int RandomizeSubType() {
        Random r = new Random(System.nanoTime());
        int inventionSubType = -1;
        switch(this.type) {
            case Weapon:
                inventionSubType = WeaponType.Random();; // Keep it within.
                break;
    //        case RangedWeapon:
      //          inventionSubType = WeaponType.RandomRanged();
        //        break;
            case Tool:
                inventionSubType = r.nextInt(ToolType.values().length+1) % ToolType.values().length; // Keep it yeaahhhh.
                break;
            case Armor:
                inventionSubType = r.nextInt(ArmorType.values().length+1) % ArmorType.values().length;
                break;
            default:
                Printer.out("Bad type, cannot determine sub-type. Type: "+this.type.name());
                System.exit(4);
        }
        Set(InventionStat.SubType, inventionSubType);
      //  Printer.out("Sub-type set to: "+inventionSubType+" type: "+this.type);
        UpdateDetails();
        return inventionSubType;
    }
    /// Randomizes details based on current stats.
    public void RandomizeDetails() {
        // Fetch all details from the array? Just quality level?
        qualityLevel = Get(InventionStat.QualityLevel);
        int inventionSubType = Get(InventionStat.SubType);
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
       // Printer.out("Updating deatils.");
        if (Get(SubType) < 0)
            RandomizeSubType();
        Set(InventionStat.DefenseBonus, 0);
        switch(this.type) {
//            case RangedWeapon:
            case Weapon: {
                // Randomize weapon type? Stats vary with weapon type instead?
                Printer.out("Shouldn't be here, subclass should handle this, type: "+this.type);
                new Exception().printStackTrace();
                System.exit(3);
                return; // All should already be finalized for the weapon now.
            }
            case Armor:
                ArmorType armorType = ArmorType.values()[Get(SubType)];
                switch (armorType){
                    case FightingGear:
                        name = "Fighter's armor";
                        Set(DefenseBonus, 3 + qualityLevel);
                        Set(AttackBonus, 0 + qualityLevel); // Yeah.
                        Set(ParryBonus, 1 + qualityLevel / 3);
                        break;
                    case InventingGear:
                        name = "Inventor's garb";
                        Set(InventingBonus, 2 + qualityLevel);
                        Set(CraftingBonus, 1 + qualityLevel / 2); // Side-bonus.
                        Set(DefenseBonus, 2 + qualityLevel / 2);
                        break;
                    case CraftingGear:
                        name = "Crafter's garb";
                        Set(CraftingBonus, 2 + qualityLevel);
                        Set(InventingBonus, 1 + qualityLevel / 2); // Side-bonus
                        Set(DefenseBonus, 2 + qualityLevel / 2);
                        break;
                    case ForagingGear:
                        name = "Forager's garb";
                        Set(ForagingBonus, 2 + qualityLevel);
                        Set(ScavengingBonus, 1 + qualityLevel);
                        Set(DefenseBonus, 2 + qualityLevel / 2);
                        break;
                    case ScoutingGear:
                        name = "Scout's garb";
                        Set(ScoutingBonus, 2 + qualityLevel);
                        Set(DefenseBonus, 2 + qualityLevel / 2);
                        Set(InventionStat.StealthBonus, 1 + qualityLevel / 2); // Side-bonus
                        break;
                    case StealthGear:
                        name = "Stealth gear";
                        Set(InventionStat.StealthBonus, 2 + qualityLevel); // Primary bonus
                        Set(ScoutingBonus, 1 + qualityLevel / 2); // Side-bonus
                        Set(DefenseBonus, 2 + qualityLevel / 2);
                        break;
/*                    case Head: name = "Helmet"; break;
                    case Body: name = "Body Armor"; break;
                    case Hands: name = "Gloves"; break;
                    case Legs: name = "Breeches"; break;
                    case Feet: name = "Greaves"; break;
  */                  default:
                        Printer.out("Undefined ArmorType");
                        new Exception().printStackTrace();
                        System.exit(5);
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
                Printer.out("Lacking code in Invention.RandomizeDetails");
                System.exit(5);
                break;
        }
        switch(qualityLevel) {
            case 0:
                name = "Crude " + name;
                break;
            case 1:
                name = "Rough " + name;
                break;
            case 2:
                break;
            case 3: // Polished?
                name = name+" +1";
                break;
            case 4: // Polished?
                name = name+" +2";
                break;
            default:
                name = name+" +3";
                break;
        }
        // Ranged stats
//        Set(InventionStat.RangedAttackBonus, rangedAttackBonus);
  //      Set(InventionStat.RangedDamageBonus, rangedAtkDmgBonus);
        // Tools stats
    }
    private void DetermineToolDetails() {
        if (Get(SubType) == -1){
            RandomizeSubType();
        }
        ToolType tt = ToolType.values()[Get(SubType)];
        switch (tt) {
            case HarvesterKit:
                Set(InventionStat.ForagingBonus, 2 + qualityLevel);
                name = "Harvesting tools";
                break;
            case ScavengingKit:
                Set(InventionStat.ScavengingBonus, 2 + qualityLevel);
                name = "Scavenging tools";
                break;
            case FirstAidKit:
                Set(InventionStat.RecoveryBonus, 2 + qualityLevel);
                name = "First aid kit";
                break;
            case ConstructionToolsKit:
                Set(InventionStat.ConstructionBonus, 2 + qualityLevel);
                name = "Construction tools kit";
                break;
            case CraftersKit:
                Set(InventionStat.CraftingBonus, 2 + qualityLevel);
                name = "Craftman's tools";
                break;
            case InventorsKit:
                Set(InventionStat.InventingBonus, 2 + qualityLevel);
                name = "Inventor's kit";
                break;
            case ScoutsKit:
                Set(InventionStat.ScoutingBonus, 2 + qualityLevel);
                name = "Scouting set";
                break;
            default:
                Printer.out("BAd");
                System.exit(14);
        }
    }

    public int AttackBonus() {
        return Get(InventionStat.AttackBonus);
    }

    public boolean IsEquipped() {
        return Get(Equipped) >= 0;
    }
}
