package cz.fit.cvut.steuejan.psi.serverClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Pattern;

public class ServerThread implements Runnable
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
        try { if(authenticate()) navigate(); }
        catch(IOException e) { e.getStackTrace(); }
        close();
    }

    private void navigate() throws IOException
    {
        if(!setupRobot())
        {
            sendOutput(Message.SERVER_SYNTAX_ERROR);
            return;
        }
        if(!goToTopLeft())
        {
            sendOutput(Message.SERVER_SYNTAX_ERROR);
            return;
        }

        if(!findMessage())
            sendOutput(Message.SERVER_SYNTAX_ERROR);
    }

    private boolean findMessage() throws IOException
    {
        Response response = pickUpMessage();
        if(response.syntaxError || response.messageFound)
            return !response.syntaxError;

        while(true)
        {
            response = goAxisX(Orientation.RIGHT, BOTTOM_RIGHT);
            if(response.syntaxError || response.messageFound)
                return !response.syntaxError;
            response = goAxisY(Orientation.DOWN, new Position(robot.position.posX, robot.position.posY-1));
            if(response.syntaxError || response.messageFound)
                return !response.syntaxError;
            response = goAxisX(Orientation.LEFT, TOP_LEFT);
            if(response.syntaxError || response.messageFound)
                return !response.syntaxError;
            response = goAxisY(Orientation.DOWN, new Position(robot.position.posX, robot.position.posY-1));
            if(response.syntaxError || response.messageFound)
                return !response.syntaxError;
        }
    }

    private Response pickUpMessage() throws IOException
    {
        sendOutput(Message.SERVER_PICK_UP);
        var input = getInput(in, 100);
        boolean messageFound = !input.first().isEmpty();
        if(messageFound && input.second())
        {
            sendOutput(Message.SERVER_LOGOUT);
            return new Response(false, true);
        }
        else return new Response(!input.second(), false);
    }

    private Response goAxisX(Orientation direction, Position endPoint) throws IOException
    {
        if(!rotate(direction)) return new Response(true, false);
        while(robot.position.posX != endPoint.posX)
        {
            if(!move()) return new Response(true, false);
            Response response = pickUpMessage();
            if(response.syntaxError || response.messageFound) return response;
        }
        return new Response(false, false);
    }

    private Response goAxisY(Orientation direction, Position endPoint) throws IOException
    {
        if(!rotate(direction)) return new Response(true, false);
        while(robot.position.posY != endPoint.posY)
        {
            if(!move()) return new Response(true, false);
            Response response = pickUpMessage();
            if(response.syntaxError || response.messageFound) return response;
        }
        return new Response(false, false);
    }

    private boolean goToTopLeft() throws IOException
    {
        if(!getToRightPosX(TOP_LEFT.posX)) return false;
        if(!getToRightPosY(TOP_LEFT.posY)) return false;
        System.out.println(robot.toString());
        return true;
    }

    private boolean getToRightPosY(int posY) throws IOException
    {
        if(!rotate(robot.position.posY < posY ? Orientation.UP : Orientation.DOWN)) return false;
        while(robot.position.posY != posY)
            if(!move()) return false;
        return true;
    }

    private boolean getToRightPosX(int posX) throws IOException
    {
        if(!rotate(robot.position.posX < posX ? Orientation.RIGHT : Orientation.LEFT)) return false;
        while(robot.position.posX != posX)
            if(!move()) return false;
        return true;
    }

    private boolean rotate(Orientation targetDir) throws IOException
    {
        int turn = Orientation.convertToInt(robot.orientation) - Orientation.convertToInt(targetDir);
        if(turn == 7) sendOutput(Message.SERVER_TURN_RIGHT);
        else if(turn == -7) sendOutput(Message.SERVER_TURN_LEFT);
        else if(turn % 3 == 0)
        {
            sendOutput(Message.SERVER_TURN_RIGHT);
            if(!setPosition()) return false;
            sendOutput(Message.SERVER_TURN_RIGHT);
        }
        else if(Integer.signum(turn) == -1) sendOutput(Message.SERVER_TURN_RIGHT);
        else if(Integer.signum(turn) == 1) sendOutput(Message.SERVER_TURN_LEFT);
        robot.orientation = targetDir;
        return setPosition();
    }

    private boolean setupRobot() throws IOException
    {
        int prevPosX, prevPosY, actualPosX, actualPosY;
        robot.position = new Position(0,0);
        if(move())
        {
            prevPosX = robot.position.posX;
            prevPosY = robot.position.posY;
        }
        else return false;

        if(move())
        {
            actualPosX = robot.position.posX;
            actualPosY = robot.position.posY;
        }
        else return false;

        robot.orientation = setupOrientation(prevPosX, prevPosY, actualPosX, actualPosY);
        return true;
    }

    private Orientation setupOrientation(int prevPosX, int prevPosY, int actualPosX, int actualPosY)
    {
        if(prevPosX == actualPosX)
            return actualPosY > prevPosY ? Orientation.UP : Orientation.DOWN;
        else
            return actualPosX > prevPosX ? Orientation.RIGHT : Orientation.LEFT;
    }

    private boolean move() throws IOException
    {
        sendOutput(Message.SERVER_MOVE);
        return setPosition();
    }

    private boolean setPosition() throws IOException
    {
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
        return Pattern.matches("OK -?[0-9]{1,3} -?[0-9]{1,3}", str);
    }

    private boolean authenticate() throws IOException
    {
        int hash = 0;
        robot = new Robot();
        var state = Authentication.CLIENT_USERNAME;
        clientSocket.setSoTimeout(TIMEOUT);

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
                    System.out.println("Authentication wasn't successful");
                    return false;

                case CLIENT_FAILED:
                    sendOutput(Message.SERVER_LOGIN_FAILED);
                    System.out.println("Authentication wasn't successful");
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
            clientSocket.setSoTimeout(TIMEOUT);

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
        catch (Exception e) { System.out.println("Unable to close a connection: " + e); }
    }

    private Socket clientSocket;
    private DataOutputStream out;
    private BufferedReader in;
    private Robot robot;
    private static final Position TOP_LEFT = new Position(-2,2);
    private static final Position BOTTOM_RIGHT = new Position(2, -2);
    private static final int KEY_SERVER = 54621;
    private static final int KEY_CLIENT = 45328;
    private static final int TIMEOUT = 1000;

}
