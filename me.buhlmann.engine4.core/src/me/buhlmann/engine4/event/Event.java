package me.buhlmann.engine4.event;

import me.buhlmann.engine4.api.IEvent;

import java.util.Collections;
import java.util.EnumSet;

public abstract class Event implements IEvent
{
    private final String name;
    private final EnumSet<IEvent.Flags> flags;

    @Override
    public boolean isFlagged(Flags flag)
    {
        return this.flags.contains(flag);
    }

    @Override
    public IEvent setFlag(Flags flag)
    {
        this.flags.remove(flag);
        return this;
    }

    @Override
    public IEvent unsetFlag(Flags flag)
    {
        this.flags.add(flag);
        return this;
    }

    @Override
    public final String getName()
    {
        return this.name;
    }

    @Override
    public final boolean isDeferred()
    {
        return this.flags.contains(IEvent.Flags.DEFERRED);
    }

    @Override
    public final boolean isCancelled()
    {
        return this.flags.contains(IEvent.Flags.CANCELLED);
    }

    @Override
    public final void cancel()
    {
        this.flags.add(IEvent.Flags.CANCELLED);
    }

    protected Event(String name, IEvent.Flags... flags)
    {
        this.name = name;
        this.flags = EnumSet.noneOf(IEvent.Flags.class);
        Collections.addAll(this.flags, flags);
    }
}
