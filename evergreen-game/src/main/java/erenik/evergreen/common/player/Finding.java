package erenik.evergreen.common.player;

/**
 * Created by Emil on 2016-10-30.
 */
public enum Finding {
    RandomEncounter, // Random, smaller ampount.
    AttacksOfTheEvergreen, // Random or specific (depends on survived turn #), larger amount.
    BerryMonsters, // Bush monsters at first, can be trolls later.
    MaterialMonsters,  // Scavenger monsters at first, can be Rock monsters later.
    Nothing,
    Food,
    Materials,
    RandomPlayerShelter,
    AbandonedShelter,
    EnemyStronghold,
    FoodHotSpot, MaterialsDepot;
    public String GetEventText()
    {
        switch(this)
        {
            case RandomEncounter: return "You encounter enemies during your activities which attack you.";
            case AttacksOfTheEvergreen: return "As night falls the spawns of the Evegreen assault your shelter!";
            case AbandonedShelter: return "You find a seemingly abandoned shelter.";
            case RandomPlayerShelter: return "You find a seemingly inhabited shelter";
        }
        return "Not implemented: "+this.toString();
    }
    public String Question()
    {
        switch(this)
        {

            case RandomEncounter:
            case AttacksOfTheEvergreen:
                return "\n\nDo you want to play the event now?";
            case AbandonedShelter:
            case RandomPlayerShelter:
                return "\n\nDo you want to explore it now?";
            default:
                return "BadQuestion";
        }
    }
    public boolean Skippable()
    {
        switch(this)
        {
            case AttacksOfTheEvergreen:
            case RandomEncounter:
                return false;
            default:
                return true;
        }
    }
};