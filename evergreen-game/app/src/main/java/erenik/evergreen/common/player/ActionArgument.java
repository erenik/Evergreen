package erenik.evergreen.common.player;

/**
 * Created by Emil on 2016-11-06.
 */
public enum ActionArgument
{
    Transport, // Which transport to augment.
    TransportAugment, // Which augment to aim to improve.
    PlayerName, // Player name to look for.
    Player, // Player steal from, etc. From drop-down list of known names.
    Stronghold, //
    InventionCategory, // String name of the InventionType?
    InventionToCraft, // String name of the Invention?
    SkillToStudy, // String name of Skill to study?
    TextSearchType,
    ResourceType,
    ResourceQuantity,
    Item, // Item to give, or the ID of it..?
    Blueprint,
    Text, // Free-text.
    TreatyType, // For treaties
    ReceivedMessage, // Received messages -> Any type.
    ; // Contains, Exactly, StartsWith
    /// Value passed by user when queueing.
    public String value = "";
    // Settings.
    // for PlayerName, is can be 0 for EXACT matches, 1 for StartsWith and 2 for Contains. To be added later?
    int settings;
}
