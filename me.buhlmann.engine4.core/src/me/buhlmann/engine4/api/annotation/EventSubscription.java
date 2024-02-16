package me.buhlmann.engine4.api.annotation;

import me.buhlmann.engine4.api.IEvent;

@FunctionalInterface
public interface EventSubscription<T extends IEvent>
{
    void onEvent(T event);
}

