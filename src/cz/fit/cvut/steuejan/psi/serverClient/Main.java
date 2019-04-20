package cz.fit.cvut.steuejan.psi.serverClient;

import java.net.Socket;
import java.io.*;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        Server server = new Server();
        server.start(port);

        while(true)
        {
            Socket clientSocket = server.getSocket().accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            new Thread(handler).start();
        }
    }

    private final static int port = 3099;
    private static int serverAddress;
    private Server server;
    private Client client;
}
