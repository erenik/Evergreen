/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import erenik.seriousgames.evergreen.Invention.Invention;
import erenik.seriousgames.evergreen.Invention.InventionType;
import java.util.ArrayList;
import java.util.List;
import erenik.seriousgames.evergreen.player.Player;
import erenik.seriousgames.evergreen.player.Stat;
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
   
    List<Player> players = new ArrayList<Player>();

    void RegisterDefaultPlayers() 
    {
        System.out.println("Register default players: ");
        Player p = new Player();
        p.SetName("Yoyo hp ");
        p.inventory.add(Invention.RandomWeapon(0));
        p.inventions.add(Invention.RandomWeapon(1));
        writePlayerToFile(p, "playerSave.sav");
        p.SetName("Bobo hp ");
        p.Set(Stat.HP, 43);
        p.Set(Stat.ABANDONED_SHELTER, 3);
        p.inventory.add(Invention.RandomWeapon(0));
        p.inventions.add(Invention.RandomWeapon(2));
        System.out.println("Before: "+p.Name());
        p.PrintAll();
        p = readPlayerFromFile("playerSave.sav");
        System.out.println("\nAfter: "+p.Name());
        p.PrintAll();
        players.add(p);

        System.out.println();
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

        ObjectInputStream objectIn = null;
        Player player = null;
        try {
            FileInputStream fileIn = new FileInputStream(filename);
            objectIn = new ObjectInputStream(fileIn);
            player = (Player) objectIn.readObject();
        } catch (FileNotFoundException e) {
            // Do nothing
            return null;
        } catch (IOException e) {
            e.printStackTrace();
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