package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.IOException;
import java.net.*;

class Server
{
    void start(int port)
    {
        try{ serverSocket = new ServerSocket(port); }
        catch(IOException exc) { System.out.println("Unable to open the socket: " + exc); }
        System.out.println("Server successfully started.");
    }

    void run()
    {
        try
        {
            while(nTests++ < NUMBER_OF_TESTS)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println();
                System.out.println("Running test " + nTests);
                System.out.println("New client connected.");
                new Thread(new ServerThread(clientSocket)).start();
            }
        }
        catch(IOException exc) {exc.getStackTrace();}
    }

    private ServerSocket serverSocket;
    private int nTests = 0;
    private static final int NUMBER_OF_TESTS = 32;

}
