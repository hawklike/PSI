package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread
{
    public ServerThread(Socket clientSocket) throws IOException
    {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        this.out = new DataOutputStream(this.clientSocket.getOutputStream());
    }

    @Override
    public void run()
    {
        authenticate();
    }

    void authenticate()
    {

    }

    private Socket clientSocket;
    private DataOutputStream out;
    private BufferedReader in;
    private final int KEY_SERVER = 54621;
    private final int KEY_CLIENT = 45328;

}
