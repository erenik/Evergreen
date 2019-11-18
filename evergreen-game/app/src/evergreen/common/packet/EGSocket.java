/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package evergreen.common.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 *
 * @author Emil
 */
public class EGSocket {

    EGSocket(String ip, int port) throws IOException {
        sock = new Socket();
        SocketAddress sockAddr = new InetSocketAddress(ip, port);
        sock.connect(sockAddr, 3000); // 3 second time-out?
    }
    
    
    private Socket sock = null;
    private PrintWriter printWriter = null;

    OutputStream getOutputStream() throws IOException 
    {
        return sock.getOutputStream();
    }

    boolean isClosed() {
        return sock.isClosed();
    }

    InputStream getInputStream() throws IOException {
        return sock.getInputStream();
    }

    PrintWriter getPrintWriter() throws IOException {
        if (printWriter == null)
            printWriter = new PrintWriter(getOutputStream(), true);
        return printWriter;
    }
    
    
}
