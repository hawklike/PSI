package cz.fit.cvut.steuejan.psi.serverClient;

public class Position
{
    public Position(int posX, int posY)
    {
        this.posX = posX;
        this.posY = posY;
    }

    public int posX;
    public int posY;

    @Override
    public boolean equals(Object o)
    {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return posX == position.posX && posY == position.posY;
    }
}
