package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Pattern;

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
            if(!authenticate())  close();
            else if(!navigate()) close();
            else close();
        }
        catch(IOException e)
        {
            System.out.println("Authentication failed!");
            e.printStackTrace();
        }
    }

    private boolean navigate() throws IOException
    {
        if(!setup())
        {
            sendOutput(Message.SERVER_SYNTAX_ERROR);
            return false;
        }

        return true;
    }

    private boolean setup() throws IOException
    {
        int prevPosX, prevPosY, actualPosX, actualPosY;
        if(setPosition())
        {
            prevPosX = robot.position.posX;
            prevPosY = robot.position.posY;
        }
        else return false;

        if(setPosition())
        {
            actualPosX = robot.position.posX;
            actualPosY = robot.position.posY;
        }
        else return false;

        robot.orientation = setOrientation(prevPosX, prevPosY, actualPosX, actualPosY);
        System.out.println(robot.toString());
        return true;
    }

    private Orientation setOrientation(int prevPosX, int prevPosY, int actualPosX, int actualPosY)
    {
        if(prevPosX == actualPosX)
            return actualPosY > prevPosY ? Orientation.UP : Orientation.DOWN;
        else
            return actualPosX > prevPosX ? Orientation.RIGHT : Orientation.LEFT;
    }

    private boolean setPosition() throws IOException
    {
        sendOutput(Message.SERVER_MOVE);
        var input = getInput(in, 12);
        if(input.second() && testClientOk(input.first()))
        {
            robot.position.posX = getPosition(input.first(), Message.POSX);
            robot.position.posY = getPosition(input.first(), Message.POSY);
            return true;
        }
        else return false;
    }

    private int getPosition(String str, int pos)
    {
        String[] elems = str.split(" ");
        return convertToNumber(elems[pos]);
    }

    private boolean testClientOk(String str)
    {
        return Pattern.matches("OK -?[0-9] -?[0-9]", str);
    }

    private boolean authenticate() throws IOException
    {
        int hash = 0;
        robot = new Robot();
        var state = Authentication.CLIENT_USERNAME;

        while(true)
        {
            switch(state)
            {
                case CLIENT_USERNAME:
                    var inputName = getInput(in, 12);
                    if(inputName.second())
                    {
                        robot.name = inputName.first();
                        if(robot.name.isEmpty())
                        {
                            state = Authentication.CLIENT_SYNTAX_ERROR;
                            continue;
                        }
                        hash = getHash(robot.name);
                        int clientCode = (hash + KEY_SERVER) % 65536;
                        sendOutput(String.valueOf(clientCode));
                        state = Authentication.CLIENT_CONFIRMATION;
                    }
                    else state = Authentication.CLIENT_SYNTAX_ERROR;
                    break;


                case CLIENT_CONFIRMATION:
                    var inputConfirmCode = getInput(in, 7);
                    if(inputConfirmCode.second())
                    {
                        String input = inputConfirmCode.first();
                        int clientCode = convertToNumber(input);
                        if(clientCode > 65535 || clientCode < 0)
                        {
                            state = Authentication.CLIENT_SYNTAX_ERROR;
                            continue;
                        }
                        int serverCode = (hash + KEY_CLIENT) % 65536;
                        if(serverCode == clientCode) state = Authentication.CLIENT_OK;
                        else state = Authentication.CLIENT_FAILED;

                    }
                    else state = Authentication.CLIENT_SYNTAX_ERROR;
                    break;

                case CLIENT_SYNTAX_ERROR:
                    sendOutput(Message.SERVER_SYNTAX_ERROR);
                    return false;

                case CLIENT_FAILED:
                    sendOutput(Message.SERVER_LOGIN_FAILED);
                    return false;

                case CLIENT_OK:
                    sendOutput(Message.SERVER_OK);
                    System.out.println("Authentication was successful");
                    return true;
            }
        }
    }

    private int getHash(String name)
    {
        int hash = 0;
        for(char letter : name.toCharArray())
            hash += (int) letter;

        return (hash * 1000) % 65536;
    }

    private int convertToNumber(String text)
    {
        int number;
        try { number = Integer.parseInt(text); }
        catch(NumberFormatException exc) { return -1; }
        return number;
    }

    private String removeLastChar(String str)
    {
        return str.substring(0, str.length()-1);
    }

    private Pair<String, Boolean> getInput(BufferedReader in, int maxLen) throws IOException
    {
        int c, len = 0;
        StringBuilder response = new StringBuilder();
        boolean lastA = false;

        while(true)
        {
            c = in.read();

            //long word or string doesn't contain \a\b
            if(++len > maxLen || c == -1) return new Pair<>(Message.SERVER_SYNTAX_ERROR, false);
            if(lastA && c == Message.B) break;
            else
            {
                lastA = c == Message.A;
                response.append((char) c);
            }
        }

        String input = removeLastChar(response.toString());
        System.out.println("Input message: " + input);
        return new Pair<>(input, true);
    }

    private void sendOutput(String text) throws IOException
    {
        out.writeBytes(text + (char) Message.A + (char) Message.B);
        out.flush();
        System.out.println("Sent this message: " + text);
    }

    private void close()
    {
        try
        {
            System.out.println("Closing connection");
            clientSocket.close();
            out.close();
            in.close();
        }
        catch (Exception e) { System.out.println("Unable to close the thread: " + e); }
    }

    private Socket clientSocket;
    private DataOutputStream out;
    private BufferedReader in;
    private Robot robot;
    private static final int KEY_SERVER = 54621;
    private static final int KEY_CLIENT = 45328;

}
