package erenik.evergreen.common.Invention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public enum InventionType
{
    Any(0), // Used as a wild-card.
    Weapon(3),
    Armor(2),
    RangedWeapon(1),
    ShelterAddition(2),
    VehicleUpgrade(2),
    Tool(2);
    InventionType(int defaultChanceToInvent)
    {
        chance = defaultChanceToInvent;
    }
    // When randoming type to invent (unspecified)
    public int chance = 0;

    public String text()
    {
        switch(this)
        {
            case Any: return "Any";
            case Armor:
            case Weapon:
                return name();
            case RangedWeapon: return "Ranged Weapon";
            case ShelterAddition: return "Shelter Addition";
            case VehicleUpgrade: return "Vehicle Upgrade";
            case Tool: return "Tool";
            default:
                return "BadString";
        }
    }
    public static InventionType RandomType()
    {
        Map<InventionType, Integer> chances = new HashMap<InventionType, Integer>();
        // Assume random for now.
        for (int i = 0; i < InventionType.values().length; ++i)
        {
            chances.put(InventionType.values()[i], InventionType.values()[i].chance);
        }
        int sumChances = 0; for (int i = 0; i < chances.size(); ++i) sumChances+= (Integer) chances.values().toArray()[i];
        InventionType type = null;
        List<Invention> inventions = new ArrayList<>();
        Random r = new Random(System.nanoTime());
        float r2 = r.nextFloat() * sumChances;
        System.out.println("Next int: "+r2);
        for (int j = 0; j < chances.size(); ++j)
        {
            r2 -=  ((Integer)chances.values().toArray()[j]).floatValue();
            if (r2 < 0) {
                // This type.
                type = (InventionType) chances.keySet().toArray()[j];
                break;
            }
        }
        System.out.println("Randomed type: "+type.text());
        return type;
    };

    public static List<String> GetStrings()
    {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < values().length; ++i)
            l.add(values()[i].text());
        return l;
    }
    public boolean IsCraftable()
    {
        switch(this)
        {
            case VehicleUpgrade:
                return false;
            default:
                return true;
        }
    }

    public static InventionType GetFromString(String typeStr)
    {
        typeStr = typeStr.trim();
        for (int i = 0; i < values().length; ++i)
        {
            if (values()[i].text().equals(typeStr))
                return values()[i];
        }
        return null;
    }
}
