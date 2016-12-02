/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import erenik.seriousgames.evergreen.auth.Auth;
import erenik.seriousgames.evergreen.player.Player;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Emil
 */
public class TCPServer
{
    /**
     * @param args the command line arguments
     */
    int portN = 4000;
    int millisecondsTimeout = 50;
    List<Socket> sockets = new ArrayList<Socket>(); 

    ServerSocket servSock;
    public static void main(String[] args) throws Exception
    {
        
        String s = "Hello world!";
       // EGPacket.UnitTest();
//        Auth.UnitTest();
        /// yeyeye.
        
        Players players = new Players();
        players.RegisterDefaultPlayers();
        TCPServer serv = new TCPServer();
        serv.StartServer();
    }
    
    void StartServer() throws IOException 
    {
        Host();
        /// Host server.
        while (true)
        {
            AcceptClients();
            if (sockets.size() > 0)
                ReadIncomingData();
        }
    }
    
    void Host() throws IOException
    {
        servSock = new ServerSocket(portN);
        servSock.setSoTimeout(millisecondsTimeout);
/*    try{} catch (java.io.InterruptedIOException)
        {
        }
*/
        System.out.println("Launching tcp listener server on port: "+portN);
    }
    static int count = 0;
    void AcceptClients() throws IOException
    {
        /// check old ones, if any disconnected
        for (int i = 0; i < sockets.size(); ++i)
        {
            Socket sock = sockets.get(i);
            if (sock.isClosed() || !sock.isConnected())
            {
                sockets.remove(sock);
                System.out.println("Client disconnected or socket closed");
                System.out.println("Num clients: "+sockets.size());
            }
        }
        while (true)
        {
            try {
                Socket socket = servSock.accept();
                sockets.add(socket);
            }
            catch (java.io.InterruptedIOException ioe)
            {
                break; // No new client, just break the loop.
            }
            System.out.println("Incoming client");
            System.out.println("Num clients: "+sockets.size());
        }
    }
    static final int BUF_LEN = 4000;
    static char[] charBuffer = new char[BUF_LEN];
    void ReadIncomingData() throws IOException 
    {
    //    System.out.print("Incoming data");
        boolean incData = false;
        for (int i = 0; i < sockets.size(); ++i)
        {
            Socket sock = sockets.get(i);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      //      System.out.println("Making new buffered readerrr");
            if (!in.ready())
                continue;
            int bytesRead = in.read(charBuffer, 0, BUF_LEN);
            String inText = String.copyValueOf(charBuffer, 0, bytesRead);
            EGPacket pack = EGPacket.packetFromString(inText);
            if (pack == null)
            {
                Reply(sock, EGPacket.error(EGErrorType.BadRequest).build()); // Reply with error String.
                sock.close(); // Close the socket.
                sockets.remove(sock); // Remove it from the list.
                continue;
            }
            // Check requests and evaluate them.
            if (pack.type == EGPacketType.Request)
                EvaluateRequest(sock, pack);
            // Parse packets received?
            System.out.println("recv: "+inText);
            /// Reply?
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            out.println("Reply");
            out.flush();
        }
       // if (incData == false)
         //   System.out.println("?");
    }
    static void Reply(Socket sock, String packetContents)
    {
        PrintWriter out = null;
        try {
            out = new PrintWriter(sock.getOutputStream(), true);
            out.println(packetContents);
            out.flush();
        } catch (IOException ex) {
            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    private void EvaluateRequest(Socket sock, EGPacket pack) {
        switch(pack.reqt)
        {
            case Save: 
                Player player = new Player();
                boolean ok = player.fromByteArr(pack.body);
                if (!ok)
                {   
                    Reply(sock, EGPacket.parseError().build());
                    return;
                }
                boolean ok = Players.Save(player);
                if (ok){
                    Reply(sock, EGPacket.ok().build());
                    return;
                }
                // Check cause of failure. Bad authentication? Name already exists?
                Reply(sock, EGPacket.error(EGErrorType.BadPassword).build());
                break;
            case Load: 
                break;
        }
    }
}
