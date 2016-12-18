/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.evergreen.server;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.packet.EGErrorType;
import erenik.evergreen.common.packet.EGPacket;
import erenik.evergreen.common.packet.EGPacketType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public int packetsReceived = 0;
    public int GetPort(){return portN;};
    int millisecondsTimeout = 50;
    List<Socket> sockets = new ArrayList<Socket>(); 

    ServerSocket servSock;
    public static void main(String[] args) throws Exception
    {        
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
        log.add("Stopping");
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
        log.add("Socket closed");
    }
    
    void Host() throws IOException
    {
        servSock = new ServerSocket(portN);
        servSock.setSoTimeout(millisecondsTimeout);
        System.out.println("Launching tcp listener server on port: "+portN);
        log.add("Hosting on port "+portN);
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
    static final int BUF_LEN = 40000;
    static byte[] readBuffer = new byte[BUF_LEN];
    void ReadIncomingData() throws IOException 
    {
    //    System.out.print("Incoming data");
        boolean incData = false;
        for (int i = 0; i < sockets.size(); ++i)
        {
            Socket sock = sockets.get(i);

            InputStream is = sock.getInputStream();
            int availableBytes = is.available();
            if (availableBytes < 0)
                continue;
            int bytesRead = is.read(readBuffer);
            System.out.println("BytesRead: "+bytesRead);
            System.out.println("Packet received?");
            EGPacket pack = EGPacket.packetFromBytes(readBuffer);
            System.out.println("Packet received: "+pack);
            if (pack == null)
            {
                System.out.println("Packet null: ");
                Reply(sock, EGPacket.error(EGErrorType.BadRequest).build()); // Reply with error String.
                sock.close(); // Close the socket.
                sockets.remove(sock); // Remove it from the list.
                continue;
            }
            ++packetsReceived;
            System.out.println("Packet type: "+pack.Type().text);
            // Check requests and evaluate them.
            if (pack.Type() == EGPacketType.Request)
                EvaluateRequest(sock, pack);
            else
            {
                Reply(sock, EGPacket.error(EGErrorType.BadRequest).build());
            }
        }
       // if (incData == false)
         //   System.out.println("?");
    }
    static void Reply(Socket sock, byte[] packetContents)
    {
        try {
            OutputStream os = sock.getOutputStream();
            os.write(packetContents, 0, packetContents.length);
        } catch (IOException ex) {
            Logger.getLogger(EGTCPServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    private void EvaluateRequest(Socket sock, EGPacket pack) 
    {
        log.add("Received packet: "+pack);
        switch(pack.ReqType())
        {
            case Save: // Player as POJO in body 
            {
                System.out.println("Evaluate request: SAVE");
                Player player = new Player();
                boolean ok = player.fromByteArr(pack.GetBody());
                if (!ok)
                {   
                    Reply(sock, EGPacket.parseError().build());
                    return;
                }
                boolean saved = Players.Save(player);
                if (saved){
                    log.add("Saved, replying OK");
                    Reply(sock, EGPacket.ok().build());
                    return;
                }
                log.add("BadPassword, replying error");
                // Check cause of failure. Bad authentication? Name already exists?
                Reply(sock, EGPacket.error(EGErrorType.BadPassword).build());
                break;
            }
            case Load: // Player as POJO in body, at least the name and password.
            {
                System.out.println("Evaluate request: LOAD");
                Player player = new Player();
                try {
                    System.out.println("EGTCPServer.EvaluateRequest: Body first 10 bytes:");
                    byte[] body = pack.GetBody();
                    for (int i = 0; i < 10; ++i)
                    {
                        System.out.print(" "+(int)body[i]);
                    }
                    for (int i = body.length - 10; i < body.length; ++i)
                    {
                        System.out.print(" "+(int)body[i]);
                    }
                    System.out.println();
                    System.out.println("Bytes in body: "+body.length);
                    /// Use bytes AFTER the header FFS....
                    boolean ok = player.fromByteArr(body);
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
    
    List<String> log = new ArrayList<>();
    public void PrintLog()
    {
        System.out.println("\nServer log:\n---------------");
        for (int i = 0; i < log.size(); ++i)
        {
            System.out.println(log.get(i));
        }
    }

    public void StopHosting() {
        stopHosting = true;
    }
}
