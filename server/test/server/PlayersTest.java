/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import erenik.seriousgames.evergreen.player.Player;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Emil
 */
public class PlayersTest {
    
    public PlayersTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        System.out.println("\nTesting Players");
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of Save method, of class Players.
     */
    @Test
    public void testSave() {
        System.out.println("Save");
        Player player = new Player();
        boolean result = Players.Save(player);
        assertEquals(result, true);
    }

    /**
     * Test of Load method, of class Players.
     */
    @Test
    public void testLoad() 
    {
        System.out.println("Load");
        Player player = new Player();
        player.SetName("TestLoadName");
        boolean saved = Players.Save(player);
        assertEquals(saved, true);
        Player loadedPlayer = Players.Load(player.Name(), player.password);
        assertEquals(loadedPlayer != null, true);
    }
}
