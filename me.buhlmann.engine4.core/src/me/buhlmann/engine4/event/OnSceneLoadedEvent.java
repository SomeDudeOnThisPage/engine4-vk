package me.buhlmann.engine4.event;

import me.buhlmann.engine4.api.IEvent;

public final class OnSceneLoadedEvent extends Event
{
    private final String scene;

    public String getSceneID()
    {
        return this.scene;
    }

    public OnSceneLoadedEvent(String sceneID)
    {
        super("onSceneLoaded", IEvent.Flags.DEFERRED);
        this.scene = sceneID;
    }
}
