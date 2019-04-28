package cz.fit.cvut.steuejan.psi.serverClient;

public class Response
{
    public Response(boolean syntaxError, boolean messageFound)
    {
        this.syntaxError = syntaxError;
        this.messageFound = messageFound;
    }

    boolean syntaxError;
    boolean messageFound;
}
