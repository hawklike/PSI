package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client
{
    public Client(Socket socket)
    {
        this.clientSocket = socket;
    }

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
}
