package cz.fit.cvut.steuejan.psi.serverClient;

public class Main
{
    private static final int PORT = 3999;

    public static void main(String[] args)
    {
        Server server = new Server();
        server.start(PORT);
        server.run();
    }
}
