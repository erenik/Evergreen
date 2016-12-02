/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

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
public class TCPServerTest {
    
    public TCPServerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
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
     * Test of main method, of class TCPServer.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        String[] args = null;
        TCPServer.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of Host method, of class TCPServer.
     */
    @Test
    public void testHost() throws Exception {
        System.out.println("Host");
        TCPServer instance = new TCPServer();
        instance.Host();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of AcceptClients method, of class TCPServer.
     */
    @Test
    public void testAcceptClients() throws Exception {
        System.out.println("AcceptClients");
        TCPServer instance = new TCPServer();
        instance.AcceptClients();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of ReadIncomingData method, of class TCPServer.
     */
    @Test
    public void testReadIncomingData() throws Exception {
        System.out.println("ReadIncomingData");
        TCPServer instance = new TCPServer();
        instance.ReadIncomingData();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
