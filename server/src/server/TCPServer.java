/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

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
    public static void main(String[] args) throws IOException
    {
        TCPServer serv = new TCPServer();
        serv.Host();
        /// Host server.
        while (true)
        {
            serv.AcceptClients();
            if (serv.sockets.size() > 0)
                serv.ReadIncomingData();
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
            if (!inText.startsWith("Evergreen"))
            {
                /// Reply?
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                out.println("BadPacket");
                out.flush();
                System.out.println("Bad data received, sending BadPacket and closing socket");
                // Close the socket.
                sock.close();
                continue;
            }
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
}
