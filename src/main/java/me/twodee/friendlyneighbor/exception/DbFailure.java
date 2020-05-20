package me.twodee.friendlyneighbor.exception;

public class DbFailure extends Throwable
{
    public DbFailure()
    {
        super("Something went wrong");
    }

    public DbFailure(String err)
    {
        super(err);
    }

    public DbFailure(Throwable e)
    {
        super(e);
    }
}
