package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.IOException;
import java.net.*;

public class Server
{
    public void start(int port)
    {
        try { serverSocket = new ServerSocket(port); }
        catch(IOException exc) { System.out.println("Unable to open the socket: " + exc); }
    }

    public ServerSocket getSocket() { return serverSocket; }
    private ServerSocket serverSocket;
}
