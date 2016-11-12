package erenik.seriousgames.evergreen.player;

/**
 * Created by Emil on 2016-10-30.
 */
public enum Finding {
    Encounter, Nothing, Food, Materials, RandomPlayerShelter,
    AbandonedShelter, EnemyStronghold, AttacksOfTheEvergreen,
    FoodHotSpot, MaterialsDepot;
    public String GetEventText()
    {
        switch(this)
        {
            case Encounter: return "You Encounter enemies during your activities which attack you.";
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

            case Encounter:
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
            case Encounter:
                return false;
            default:
                return true;
        }
    }
};