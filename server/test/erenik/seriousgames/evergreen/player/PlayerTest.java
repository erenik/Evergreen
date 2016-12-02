/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.seriousgames.evergreen.player;

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
public class PlayerTest {
    
    public PlayerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        System.out.println("Player test");
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    Player instance;
    @Before
    public void setUp() {   
        instance = new Player();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of toByteArr method, of class Player.
     */
    @Test
    public void testToByteArr() 
    {
        System.out.println("Testing: toByteArr");
        instance = new Player();
 //       instance.PrintAll(); // Print all details.
        byte[] byteArr = instance.toByteArr();
//        System.out.println("byteArr: "+byteArr.toString());
    }

    /**
     * Test of fromByteArr method, of class Player.
     */
    @Test
    public void testFromByteArr() 
    {
        System.out.println("Testing: fromByteArr");
        byte[] bytes = instance.toByteArr();
        Player player2 = new Player();
        boolean result = player2.fromByteArr(bytes);
//        System.out.println("Result: "+result);
  //      instance.PrintAll();
        byte[] bytes2 = player2.toByteArr();
        assertArrayEquals(bytes, bytes2);
    }    
}
