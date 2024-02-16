package me.buhlmann.engine4.event;

import me.buhlmann.engine4.api.IEvent;
import org.joml.Vector2i;

public class WindowResizeEvent extends Event
{
    private final Vector2i size;

    public Vector2i getSize()
    {
        return this.size;
    }
    
    public WindowResizeEvent(final Vector2i size)
    {
        super("onWindowResize", IEvent.Flags.DEFERRED);
        this.size = size;
    }
}
