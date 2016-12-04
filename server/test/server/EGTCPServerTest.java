/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import erenik.seriousgames.evergreen.player.Player;
import java.net.Socket;
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
public class EGTCPServerTest {
    
    public EGTCPServerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        System.out.println("\nTesting EGTCPServer");
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    EGTCPServer server;
    
    @Before
    public void setUp() 
    {
        System.out.println("setUp");
        EGPacketReceiver.StartSingleton();
        server = new EGTCPServer();
        server.start(); // Start thread.
    }
    
    @After
    public void tearDown() 
    {
        EGPacketReceiver.StopSingleton();
        server.stopHosting = true;
    }

    /**
     * Test of run method, of class TCPServer.
     */
    @Test
    public void testIt() throws InterruptedException 
    {
        System.out.println("testing it.");
//        server.portN;
        // Send some messages to the server on given port.
        Player player = new Player();
        player.SetName("Verycool");
        player.password = "So good";
        EGRequest egr = EGRequest.Load(player);
        egr.Send("localhost", server.portN);

        Thread.sleep(1000);
        
        EGPacket reply = egr.reply;
        System.out.println("Reply: "+reply);
        reply.toString();
        
    }

}
