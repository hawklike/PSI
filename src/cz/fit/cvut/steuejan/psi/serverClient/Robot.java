package cz.fit.cvut.steuejan.psi.serverClient;

class Robot
{
    Position position;
    String name;
    Orientation orientation;

    @Override
    public String toString()
    {
        return "Robot {" +
                "position=" + position.posX + " " + position.posY +
                ", name='" + name + '\'' +
                ", orientation=" + orientation +
                '}';
    }
}