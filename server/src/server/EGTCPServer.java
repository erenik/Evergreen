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
public class EGTCPServer extends Thread
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
        EGTCPServer serv = new EGTCPServer();
        serv.StartServer();
    }
    boolean stopHosting = false;
    public void run()
    {
        try {
            StartServer();
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Stopping server.");
    }
    
    void StartServer() throws IOException 
    {
        Host();
        /// Host server.
        while (stopHosting == false)
        {
            AcceptClients();
            if (sockets.size() > 0)
                ReadIncomingData();
        }
        servSock.close();
    }
    
    void Host() throws IOException
    {
        servSock = new ServerSocket(portN);
        servSock.setSoTimeout(millisecondsTimeout);
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
                System.out.println("EGTCPServer.AcceptClients: Client disconnected or socket closed");
                System.out.println("EGTCPServer.AcceptClients: Num clients: "+sockets.size());
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
            System.out.println("BytesRead: "+bytesRead);
            for (int j = 0; j < bytesRead; ++j)
            {
                System.out.print(charBuffer[j]);
            }
            System.out.println();
            String inText = String.copyValueOf(charBuffer, 0, bytesRead);
            System.out.println("Packet received?");
            EGPacket pack = EGPacket.packetFromString(inText);
            System.out.println("Packet received: "+pack);
            if (pack == null)
            {
                System.out.println("Packet null: ");
                Reply(sock, EGPacket.error(EGErrorType.BadRequest).build()); // Reply with error String.
                sock.close(); // Close the socket.
                sockets.remove(sock); // Remove it from the list.
                continue;
            }
            System.out.println("Packet type: "+pack.type.text);
            // Check requests and evaluate them.
            if (pack.type == EGPacketType.Request)
                EvaluateRequest(sock, pack);
            else
            {
                Reply(sock, EGPacket.error(EGErrorType.BadRequest).build());
            }
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
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    private void EvaluateRequest(Socket sock, EGPacket pack) 
    {
        switch(pack.reqt)
        {
            case Save: // Player as POJO in body 
            {
                System.out.println("Evaluate request: SAVE");
                Player player = new Player();
                boolean ok = player.fromByteArr(pack.body);
                if (!ok)
                {   
                    Reply(sock, EGPacket.parseError().build());
                    return;
                }
                boolean saved = Players.Save(player);
                if (saved){
                    Reply(sock, EGPacket.ok().build());
                    return;
                }
                // Check cause of failure. Bad authentication? Name already exists?
                Reply(sock, EGPacket.error(EGErrorType.BadPassword).build());
                break;
            }
            case Load: // Player as POJO in body, at least the name and password.
            {
                System.out.println("Evaluate request: LOAD");
                Player player = new Player();
                try {
                    boolean ok = player.fromByteArr(pack.body);
                } catch (Exception e)
                {
                    System.out.println("reply parse error");
                    Reply(sock, EGPacket.error(EGErrorType.ParseError).build());
                    return;
                }
                System.out.println("fromByteArr");
                boolean saved = Players.Save(player);
                if (saved){
                    System.out.println("reply Save OK");
                    Reply(sock, EGPacket.ok().build());
                    return;
                }
                // Check cause of failure. Bad authentication? Name already exists?
                Reply(sock, EGPacket.error(EGErrorType.BadPassword).build());
                break;
            }
            default:
                System.out.println("Send bad request reply o-o");
                Reply(sock, EGPacket.error(EGErrorType.BadRequest).build());

        }
    }
}
