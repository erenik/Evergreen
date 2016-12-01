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
public class EGPacketTest {
    
    public EGPacketTest() {
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
     * Test of build method, of class EGPacket.
     */
    @Test
    public void testBuild() {
        System.out.println("build");
//        EGPacket instance = new EGPacket();
  //      String expResult = null;
    //    String result = instance.build();
      //  assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of packetFromString method, of class EGPacket.
     */
    @Test
    public void testPacketFromString() 
    {    
        EGRequestType reqType = EGRequestType.Save;
        EGPacket pack = new EGPacket();
        pack.type = EGPacketType.Request;
        pack.reqt = reqType;
        pack.body = "{\"name\":\"Emil\"}";
        String built = pack.build();
        System.out.println("Built: \n"+built);
        
        EGPacket pack2 = EGPacket.packetFromString(built);
        System.out.println("Parsed: "+pack2
                +"\nPT: "+pack2.type+" REQ: "+pack2.reqt+" RES: "+pack2.rest+" body: "+pack2.body
        );
        assertEquals(pack.reqt, pack2.reqt);
        assertEquals(pack.rest, pack2.rest);
        assertEquals(pack.body, pack2.body);
        
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }    
}
