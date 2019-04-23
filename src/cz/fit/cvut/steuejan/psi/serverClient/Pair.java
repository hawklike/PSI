package cz.fit.cvut.steuejan.psi.serverClient;

public class Pair<L, R>
{
    public Pair(L left, R right)
    {
        this.left = left;
        this.right = right;
    }

    private L left;
    private R right;

    public L first() { return left; }
    public R second() { return right; }
}
