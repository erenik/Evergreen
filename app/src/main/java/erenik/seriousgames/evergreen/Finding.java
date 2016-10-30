package erenik.seriousgames.evergreen;

/**
 * Created by Emil on 2016-10-30.
 */
public enum Finding {
    Encounter, Nothing, Food, Materials, RandomPlayerShelter,
    AbandonedShelter, EnemyStronghold, AttacksOfTheEvergreen,
    ;
    public String GetEventText()
    {
        switch(this)
        {
            case Encounter: return "You encounter enemies during your activities which attack you.";
            case AttacksOfTheEvergreen: return "As night falls the spawns of the Evegreen assault your shelter!";
        }
        return "Not implemented: "+this.toString();
    }
};