package cz.fit.cvut.steuejan.psi.serverClient;

public enum Orientation
{
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public static int convertToInt(Orientation dir)
    {
        switch(dir)
        {
            case UP:
                return 1;
            case RIGHT:
                return 2;
            case DOWN:
                return 4;
            case LEFT:
                return 8;
        }
        return 0;
    }
}