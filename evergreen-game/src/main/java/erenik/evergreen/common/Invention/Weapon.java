package erenik.evergreen.common.Invention;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import erenik.util.Dice;

/**
 * Created by Emil on 2017-02-26.
 */

public class Weapon extends Invention  implements Serializable {
    private static final long serialVersionUID = 1L;


    @Override
    public String toString() {
        return name+" "+Get(InventionStat.AttackDamageDice)+"D"+Get(InventionStat.AttackDamageDiceType)+"+"+Get(InventionStat.AttackDamageBonus)+
                " Acc+"+Get(InventionStat.AttackBonus)+" NumAttacks: "+1+Get(InventionStat.BonusAttacks);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Testing out Weapon Save/Load.");
        String path = "weaponTest.save";

        Weapon weapon = new Weapon(); // Create a random weapon.
        weapon.RandomizeSubType();
        weapon.SaveLoadFile(path);

        Invention trySaveThis = weapon;
        String path2 = "weaponAsInventionTest";
        System.out.println("Weapon as inv: "+trySaveThis);
        File file = new File(path2);
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(trySaveThis);
        oos.close();

        // Try load it?
        File file2 = new File(path);
        FileInputStream fis = new FileInputStream(file2);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object obj = ois.readObject();
        if (obj instanceof Weapon){
            System.out.println("Is weapon");
            Weapon loadedWeapon = (Weapon) obj;
            System.out.println("Weapon: "+loadedWeapon);
        }
    }
    void SaveLoadFile(String path) throws Exception{
        System.out.println("Weapon: "+this);
        File file = new File(path);
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(this);
        oos.close();

        // Try load it?
        File file2 = new File(path);
        FileInputStream fis = new FileInputStream(file2);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object obj = ois.readObject();
        if (obj instanceof Weapon){
            System.out.println("Is weapon");
            Weapon loadedWeapon = (Weapon) obj;
            System.out.println("Weapon: "+loadedWeapon);
        }
    }



    public Weapon() {
        super(InventionType.Weapon);
        // Randomize sub-type?
        RandomizeSubType();
    }
    void SetMain(String name, int numDice, int diceType, int dmgBonus, int atkBonus, int numAttacks){
        this.name = name;
        // Save stats accordingly.
        Set(InventionStat.AttackDamageDiceType, diceType);
        Set(InventionStat.AttackDamageDice, numDice);
        Set(InventionStat.AttackBonus, atkBonus);
        Set(InventionStat.AttackDamageBonus, dmgBonus);
        Set(InventionStat.BonusAttacks, numAttacks - 1);
    }
    void SetDefensive(int defBonus, int parryBonus){
        Set(InventionStat.DefenseBonus, defBonus);
        Set(InventionStat.ParryBonus, parryBonus);
    }

    int atkDmgDice = 0, atkDmgDiceType = 0, atkDmgBonus = 0; // Temporary vars.
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



    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//        System.out.println("Weapon writeObject");
        writeObjectToStream(out);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
  //      System.out.println("Weapon readObject");
        readObjectFromStream(in);
    }
    private void readObjectNoData() throws ObjectStreamException {

    }

    public void UpdateDetails(){
        int qualityLevel = Get(InventionStat.QualityLevel);
        WeaponType wt = WeaponType.values()[Get(InventionStat.SubType)];
        int additionalEffectDice = 0;
        switch (wt) {
            // Knives n daggers. // 1D3+1, 3 att, 1 attack/round
            case Knife: SetMain("Knife", 1, 3, 1, 3, 2);  break;
            case Dagger: SetMain("Dagger", 1, 3, 2, 4, 2); break;
            default:
                // Sword-type weapons // 1D3+1, 3 att, 1 attack/round
            case Sword: SetMain("Gladius", 1, 6, 4, 5, 1);
                SetDefensive(0, 1 + qualityLevel / 2); break;
            case Longsword: SetMain("Longsword", 1, 8, 6, 2, 1);
                SetDefensive(0, 2 + qualityLevel / 5);
                break;
            /// Club-type weapons.
            case Club: SetMain("Club", 2, 3, 2, 3, 1);
                break;
            case GreatClub: SetMain("Heavy club", 2, 5, 4, 1, 1);
                break;
            case Sledgehammer: SetMain("Sledgehammer", 2, 6, 7, -1, 1);
                break;
            // Axe-type weapons
            case Axe: SetMain("Axe", 1, 8, 4, 6, 1);
                break;
            case GreatAxe: SetMain("Great Axe", 1, 12, 6, 5, 1);
                break;

            // Ranged weapons/
            case Boomerang: SetRanged("Boomerang", 1, 6, 1, 1, 2); break;
            case ShortBow: SetRanged("Short Bow", 1, 3, (int)(1 + qualityLevel * 0.5f), 1 + qualityLevel, 3); break;
            case LongBow: SetRanged("Long Bow", 2, 4, (int)(1 + qualityLevel * 1.f), 0 + qualityLevel, 2); break;
            case Crossbow: SetRanged("Crossbow", 3, 6, (int)(1 + qualityLevel * 2.5f), -1 + qualityLevel, 1); break;
        }
        switch (qualityLevel) {
            case 0: name = "Crude " + name;
                Adjust(InventionStat.AttackBonus, -2);
                Adjust(InventionStat.AttackDamageBonus, -2);
                break;
            case 1:
                name = "Rough " + name;
                Adjust(InventionStat.AttackBonus, -2);
            case 2: // QL 2 is like 0 - normal?
                break;
            case 3: // Polished?
                name = name+" +1";
                Adjust(InventionStat.AttackBonus, 1);
                Adjust(InventionStat.AttackDamageBonus, 1);
                break;
            case 4:
                name = name+" +2";
                Adjust(InventionStat.AttackBonus, 2);
                Adjust(InventionStat.AttackDamageBonus, 2);
                additionalEffectDice = 1;
                break;
            case 5:
                name = name+" +3";
                Adjust(InventionStat.AttackBonus, 3);
                Adjust(InventionStat.AttackDamageBonus, 3);
                additionalEffectDice = 1;
                Adjust(InventionStat.BonusAttacks, 1);
            case 6:
            default:
                name = name+" +4";
                Adjust(InventionStat.AttackBonus, 4);
                Adjust(InventionStat.AttackDamageBonus, 4);
                additionalEffectDice = 1;
                Adjust(InventionStat.BonusAttacks, 2);
                break;
        }
        if (atkDmgBonus < 0) // Minimum +0 bonus, no negatives.
            atkDmgBonus = 1;
        // Randomize additional effect if needed.
        if (additionalEffectDice > 0)
            Set(InventionStat.AdditionalEffect, Dice.RollD6(1));
        UpdateWeaponAdditionalEffect();
    }

    private void SetRanged(String name, int numDice, int diceType, int dmgBonus, int rangedAcc, int attacksPerRoundWhenNotEngaged) {
        this.name = name;
        Set(InventionStat.AttackDamageDiceType, diceType);
        Set(InventionStat.AttackDamageDice, numDice);
        Set(InventionStat.RangedAttackBonus, rangedAcc);
        Set(InventionStat.RangedDamageBonus, dmgBonus);
        Set(InventionStat.BonusAttacks, attacksPerRoundWhenNotEngaged - 1);
    }

    public void UpdateWeaponAdditionalEffect() {
        switch (Get(InventionStat.AdditionalEffect)) {
            case -1: // No effect!
                break;
            default:
  //              System.out.println("Very cool");
//                new Exception().printStackTrace();
            case 0:
                Adjust(InventionStat.DefenseBonus, 3);
                Adjust(InventionStat.ParryBonus, 1);
                name = "Defender's " + name;
                break;
            case 1:
                Adjust(InventionStat.AttackBonus, 4);
                Adjust(InventionStat.AttackDamageBonus, 2);
                name = "Warrior's " + name;
                break;
            case 2:
                Adjust(InventionStat.AttackDamageBonus, 5);
                Adjust(InventionStat.AttackBonus, 2);
                name = "Berserker's " + name;
                break;
            case 3:
                Adjust(InventionStat.ParryBonus, 2);
                Adjust(InventionStat.DefenseBonus, 1);
                Adjust(InventionStat.AttackBonus, 1);
                name = "Duelist's " + name;
                break;
            case 4:
                Adjust(InventionStat.BonusAttacks, 1);
                Adjust(InventionStat.AttackBonus, -1);
                Adjust(InventionStat.AttackDamageBonus, -2);
                name = "Swift" + name;
                break;
        }
    }
}
