package me.buhlmann.engine4.api;

public interface IEvent extends IFlags<IEvent.Flags, IEvent>
{
    enum Flags
    {
        DEFERRED,
        CANCELLED
    }

    String getName();
    boolean isDeferred();
    boolean isCancelled();
    void cancel();
}
