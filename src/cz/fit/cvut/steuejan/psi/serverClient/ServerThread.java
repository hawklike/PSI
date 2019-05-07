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
        catch(LogicErrorException e)
        {
            try { sendOutput(Message.SERVER_LOGIC_ERROR); }
            catch(IOException ex) { ex.printStackTrace(); }
            System.out.println("Logic error occurred.");
        }
        close();
    }

    private void navigate() throws IOException, LogicErrorException
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

    private boolean findMessage() throws IOException, LogicErrorException
    {
        Response response = pickUpMessage();
        if(response.syntaxError || response.messageFound)
            return !response.syntaxError;

        while(true)
        {
            response = goAxisX(Orientation.RIGHT, BOTTOM_RIGHT);
            if(response.syntaxError || response.messageFound) return !response.syntaxError;

            response = goAxisY(Orientation.DOWN, new Position(robot.position.posX, robot.position.posY-1));
            if(response.syntaxError || response.messageFound) return !response.syntaxError;

            response = goAxisX(Orientation.LEFT, TOP_LEFT);
            if(response.syntaxError || response.messageFound) return !response.syntaxError;

            response = goAxisY(Orientation.DOWN, new Position(robot.position.posX, robot.position.posY-1));
            if(response.syntaxError || response.messageFound) return !response.syntaxError;
        }
    }

    private Response pickUpMessage() throws IOException, LogicErrorException
    {
        sendOutput(Message.SERVER_PICK_UP);
        var input = getInput(in, 100);

        if(input.first().equals(Message.CLIENT_RECHARGING) && input.second())
        {
            if(!recharging()) throw new LogicErrorException();
            else input = getInput(in, 100);
        }

        boolean messageFound = !input.first().isEmpty();
        if(messageFound && input.second())
        {
            sendOutput(Message.SERVER_LOGOUT);
            return new Response(false, true);
        }
        else return new Response(!input.second(), false);
    }

    private Response goAxisX(Orientation direction, Position endPoint) throws IOException, LogicErrorException
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

    private Response goAxisY(Orientation direction, Position endPoint) throws IOException, LogicErrorException
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

    private boolean goToTopLeft() throws IOException, LogicErrorException
    {
        if(!getToRightPosX(TOP_LEFT.posX)) return false;
        if(!getToRightPosY(TOP_LEFT.posY)) return false;
        System.out.println(robot.toString());
        return true;
    }

    private boolean getToRightPosY(int posY) throws IOException, LogicErrorException
    {
        if(!rotate(robot.position.posY < posY ? Orientation.UP : Orientation.DOWN)) return false;
        while(robot.position.posY != posY)
            if(!move()) return false;
        return true;
    }

    private boolean getToRightPosX(int posX) throws IOException, LogicErrorException
    {
        if(!rotate(robot.position.posX < posX ? Orientation.RIGHT : Orientation.LEFT)) return false;
        while(robot.position.posX != posX)
            if(!move()) return false;
        return true;
    }

    private boolean rotate(Orientation targetDir) throws IOException, LogicErrorException
    {
        System.out.println("Target direction: " + targetDir);
        int turn = Orientation.convertToInt(robot.orientation) - Orientation.convertToInt(targetDir);
        if(turn == 0) return true;
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

    private boolean setupRobot() throws IOException, LogicErrorException
    {
        Position prevPosition, actualPosition;
        robot.position = new Position(0,0);

        if(move()) prevPosition = robot.position;
        else return false;

        do
        {
            if(move()) actualPosition = robot.position;
            else return false;
        }
        while(prevPosition.equals(actualPosition));

        robot.orientation = setupOrientation(prevPosition, actualPosition);
        System.out.println(robot.toString());
        return true;
    }

    private Orientation setupOrientation(Position prevPosition, Position actualPosition)
    {
        if(prevPosition.posX == actualPosition.posX)
            return actualPosition.posY > prevPosition.posY ? Orientation.UP : Orientation.DOWN;
        else
            return actualPosition.posX > prevPosition.posX ? Orientation.RIGHT : Orientation.LEFT;
    }

    private boolean move() throws IOException, LogicErrorException
    {
        sendOutput(Message.SERVER_MOVE);
        return setPosition();
    }

    private boolean setPosition() throws IOException, LogicErrorException
    {
        var input = getInput(in, 12);

        if(input.first().equals(Message.CLIENT_RECHARGING) && input.second())
        {
            if(!recharging()) throw new LogicErrorException();
            else input = getInput(in, 12);
        }

        if(input.second() && testClientOk(input.first()))
        {
            int posX = getPosition(input.first(), Message.POSX);
            int posY = getPosition(input.first(), Message.POSY);
            robot.position = new Position(posX, posY);
            System.out.println(robot.toString());
            return true;
        }
        else return false;
    }

    private static int getPosition(String str, int pos)
    {
        String[] elems = str.split(" ");
        return convertToNumber(elems[pos]);
    }

    private boolean testClientOk(String str)
    {
        return Pattern.matches("OK -?[0-9]{1,3} -?[0-9]{1,3}", str);
    }

    private boolean recharging() throws IOException
    {
        clientSocket.setSoTimeout(TIMEOUT_RECHARGING);
        var input = getInput(in, 12);
        if(!input.second()) return false;
        else return input.first().equals(Message.CLIENT_FULL_POWER);
    }

    private boolean authenticate() throws IOException, LogicErrorException
    {
        int hash = 0;
        robot = new Robot();
        var state = Authentication.CLIENT_USERNAME;
        var expectedState = Authentication.CLIENT_USERNAME;
        Pair<String, Boolean> input = null;

        while(true)
        {
            if(state == Authentication.CLIENT_USERNAME || state == Authentication.CLIENT_CONFIRMATION)
            {
                input = getInput(in, 12);
                if(!input.second()) state = Authentication.CLIENT_SYNTAX_ERROR;
                else if(input.first().equals(Message.CLIENT_RECHARGING)) state = Authentication.CLIENT_RECHARGING;
            }

            switch(state)
            {
                case CLIENT_USERNAME:
                    robot.name = input.first();
                    hash = getHash(robot.name);
                    int clientCodeUsername = (hash + KEY_SERVER) % 65536;
                    sendOutput(String.valueOf(clientCodeUsername));
                    expectedState = state = Authentication.CLIENT_CONFIRMATION;
                    break;

                case CLIENT_CONFIRMATION:
                    String clientCodeStr = input.first();
                    int clientCodeConfirm = convertToNumber(clientCodeStr);
                    if(clientCodeConfirm > 65535 || clientCodeConfirm < 0 || clientCodeStr.length() > 5)
                    {
                        state = Authentication.CLIENT_SYNTAX_ERROR;
                        continue;
                    }
                    int serverCode = (hash + KEY_CLIENT) % 65536;
                    if(serverCode == clientCodeConfirm) expectedState = state = Authentication.CLIENT_OK;
                    else state = Authentication.CLIENT_FAILED;
                    break;

                case CLIENT_RECHARGING:
                    if(!recharging()) throw new LogicErrorException();
                    state = expectedState;
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

    private static int getHash(String name)
    {
        int hash = 0;
        for(char letter : name.toCharArray())
            hash += (int) letter;

        return (hash * 1000) % 65536;
    }

    private static int convertToNumber(String text)
    {
        int number;
        try { number = Integer.parseInt(text); }
        catch(NumberFormatException exc) { return -1; }
        return number;
    }

    private static String removeLastChar(String str)
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
            //maximum number of any chars, sequence \a\b should follow
            if(len == maxLen && c != Message.B) return new Pair<>(Message.SERVER_SYNTAX_ERROR, false);

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
    private static final int TIMEOUT_RECHARGING = 5000;
}
