/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.server;

import erenik.evergreen.common.Invention.Invention;

import erenik.util.EList;
import erenik.util.EList;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.player.Stat;
import erenik.util.Printer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Emil
 */
public class Players {
   
    EList<Player> players = new EList<Player>();
    
    static String playersDir = "players/";

    /// o-o
    public static final int NO_ERROR = 0,
            DOES_NOT_EXIST = 1,
            WRONG_PASSWORD = 2,
            IO_ERROR = 3;
    public static int lastError = 0;
    // Save to its own file?
    static boolean Save(Player player) 
    {
        // Make directory if not existing already.
        new File(playersDir).mkdirs();
        /// Load, check password matches, save it.
        Player tryLoadPlayer = Load(player.name, player.password);
        if (tryLoadPlayer == null && lastError == WRONG_PASSWORD){
            return false;
        }
        boolean ok = writePlayerToFile(player, playersDir+player.Name());
        if (ok)
            return true;
        lastError = IO_ERROR;
        return false;
    }
    static Player Load(String playerName, String password)
    {
        Player player = readPlayerFromFile(playersDir+playerName);
        if (player == null)
        {
            lastError = DOES_NOT_EXIST;
            return null;
        }
        if (password.equals(player.password)){
            return player;
        }
        lastError = WRONG_PASSWORD;
        return null;
    }

    void RegisterDefaultPlayers() 
    {
        /*
        Printer.out("Register default players: ");
        Player p = new Player();
        p.SetName("Yoyo hp ");
        p.cd.inventory.add(Invention.RandomWeapon(0));
        p.cd.inventions.add(Invention.RandomWeapon(1));
        writePlayerToFile(p, "playerSave.sav");
        p.SetName("Bobo hp ");
        p.Set(Stat.HP, 43);
        p.Set(Stat.ABANDONED_SHELTER, 3);
        p.cd.inventory.add(Invention.RandomWeapon(0));
        p.cd.inventions.add(Invention.RandomWeapon(2));
        Printer.out("Before: "+p.Name());
        p.PrintAll();
        p = readPlayerFromFile("playerSave.sav");
        Printer.out("\nAfter: "+p.Name());
        p.PrintAll();
        players.add(p);
*/
        Printer.out();
    }
    
    public static boolean writePlayerToFile(Player object, String filename) 
    {
        ObjectOutputStream objectOut = null;
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(object);
            fileOut.getFD().sync();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException e) {
                    return false;
                    // do nowt
                }
            }
        }
        return true;
    }
    public static Player readPlayerFromFile(String filename) {
        Printer.out("Loading player from file: "+filename);
        ObjectInputStream objectIn = null;
        Player player = null;
        try {
            FileInputStream fileIn = new FileInputStream(filename);
            objectIn = new ObjectInputStream(fileIn);
            player = (Player) objectIn.readObject();
            objectIn.close();
        } catch (FileNotFoundException e) {
            // Do nothing
            return null;
        } catch (IOException e) {
            Printer.out("IOException: "+e.toString());
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e) {
                    // do nowt
                }
            }
        }
        return player;
    }
}