package evergreen;

/**
 * Created by Emil on 2016-12-18.
 */

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;

/** Game that this player belongs to.
 *  0 - Local game. Backed up on server for practical purposes.
 *  1 - Global game. All players can interact with you.
 *  2 - Local Multiplayer game. Enter IP or other details to create ad-hoc connection.
 *  3-99 - Reserved.
 *  100-2000. Public game IDs. These games may have a password to join them.
 */

public class GameID implements Serializable{
    private static final long serialVersionUID = 1L;

    public static int BadID = -1;
    public static int LocalGame = 0;
    public static int GlobalGame = 1; // Refers to any of the above, depends on test mode.
    public static int MAX_TYPES = 2; // May refer to any global game mode, or all. Depends on implementation/server. Should default to main mode (e.g. 1 min during test, 24h for release).
    /*
    public static int GlobalGame_24Hours = 1;
    public static int GlobalGame_10Seconds = 2;
    public static int GlobalGame_60Seconds = 3;
    public static int GlobalGame_10Minutes = 4;
    public static int GlobalGame_60Minutes = 5;
    public static int LocalMultiplayer = 10;

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
    GameID(int numID, String name) {
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
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(name);
        out.writeObject(typeString);
        out.writeInt(id);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
        name = (String) in.readObject();
        typeString = (String) in.readObject();
        id = in.readInt();
    }

    String typeString; // Type of game.
    String name; // Name of the game.
    int id; // Unique #.
}
