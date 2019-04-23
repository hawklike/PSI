package cz.fit.cvut.steuejan.psi.serverClient;

class Message
{
    static final int A = 7;
    static final int B = 8;
    static final String SERVER_MOVE = "102 MOVE";
    static final String SERVER_TURN_LEFT = "103 TURN LEFT";
    static final String SERVER_TURN_RIGHT = "104 TURN RIGHT";
    static final String SERVER_PICK_UP = "105 GET MESSAGE";
    static final String SERVER_LOGOUT = "106 LOGOUT";
    static final String SERVER_OK = "200 OK";
    static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED";
    static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR";
    static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR";
}
