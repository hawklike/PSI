package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable
{
    public ClientHandler(Socket clientSocket) throws IOException
    {
        this.socket = clientSocket;
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run()
    {

    }

    private Socket socket;
    private DataOutputStream out;
    private BufferedReader in;

}
