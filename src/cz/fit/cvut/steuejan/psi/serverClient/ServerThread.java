package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerThread extends Thread
{
    ServerThread(Socket clientSocket) throws IOException
    {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        this.out = new DataOutputStream(this.clientSocket.getOutputStream());
    }

    @Override
    public void run()
    {
        try
        {
            authenticate();
        }
        catch(IOException e) { e.printStackTrace(); }
    }

    private void authenticate() throws IOException
    {
        robot = new Robot();
        robot.name = String.valueOf(getInput(in, 12).second());
        System.out.print("Robot's name: ");
        System.out.println(robot.name);
    }

    private Pair<String, Boolean> getInput(BufferedReader in, int maxLen) throws IOException
    {
        int c, len = 0;
        StringBuilder response = new StringBuilder();

        //do unless c is \a
        while((c = in.read()) != Message.A)
        {
            len++;
            response.append((char) c);
        }

        //now \b should be read, if not, then throw syntax error
        if(in.read() == Message.B) return ++len > maxLen ? new Pair<>(Message.SERVER_SYNTAX_ERROR, false) : new Pair<>(response.toString(), true);
        else return new Pair<>(Message.SERVER_SYNTAX_ERROR, false);
    }

    private Socket clientSocket;
    private DataOutputStream out;
    private BufferedReader in;
    private Robot robot;
    private final int KEY_SERVER = 54621;
    private final int KEY_CLIENT = 45328;

}
