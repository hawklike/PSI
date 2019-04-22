package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.IOException;
import java.net.*;

public class Server
{
    void start(int port)
    {
        try{serverSocket = new ServerSocket(port);}
        catch(IOException exc) {System.out.println("Unable to open the socket: " + exc);}
        System.out.println("Server successfully started.");
    }


    public void run()
    {
        try
        {
            while(true)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected.");
                new ServerThread(clientSocket).start();
            }
        }
        catch(IOException exc) {exc.getStackTrace();}
    }

    private ServerSocket serverSocket;

}
