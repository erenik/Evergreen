package erenik.evergreen;

/**
 * Created by Emil on 2016-12-18.
 */

/** Game that this player belongs to.
 *  0 - Local game. Backed up on server for practical purposes.
 *  1 - Global game. All players can interact with you.
 *  2 - Local Multiplayer game. Enter IP or other details to create ad-hoc connection.
 *  3-99 - Reserved.
 *  100-2000. Public game IDs. These games may have a password to join them.
 */

public class GameID {
    public static int LocalGame = 0;
    public static int GlobalGame_24Hours = 1;
    public static int GlobalGame_10Seconds = 2;
    public static int GlobalGame_60Seconds = 3;
    public static int GlobalGame_10Minutes = 4;
    public static int GlobalGame_60Minutes = 5;
    public static int LocalMultiplayer = 10;
    /*
    LocalGame(0),
    GlobalGame(1), // Default/main game, 1 update every 24 hours.
    LocalMultiplayer(2),
    GlobalGame1Minute(3),
    GlobalGame60Minutes(4),
    GlobalGame10Seconds(5),
    ReservedGameID, // up to 99
    PublicGameID, // from 100, up to 2k
    */
    ;
//    GameID(){id = -2;}
    GameID(int numID, String name)
    {
        this.id = numID;
        switch(numID)
        {
            case 0: typeString = "LocalGame"; break;
            case 1: typeString = "GlobalGame"; break;
            default:
                if (numID < 100)
                    typeString = "ReservedID";
                else if (numID < 2000)
                    typeString = "PublicGameID";
                else
                    typeString = "BadGameID";
                break;
        }

        this.name = name;
    }
    String typeString; // Type of game.
    String name; // Name of the game.
    int id; // Unique #.
}
