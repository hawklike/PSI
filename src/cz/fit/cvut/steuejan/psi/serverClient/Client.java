package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.IOException;
import java.net.Socket;

public class Client
{

    public void start(String ipAddress, int port) throws IOException
    {
        Socket s = new Socket(ipAddress, port);
    }
}
