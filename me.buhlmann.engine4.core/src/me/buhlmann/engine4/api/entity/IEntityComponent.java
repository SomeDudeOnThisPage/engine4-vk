package me.buhlmann.engine4.api.entity;

public interface IEntityComponent
{
    default void onComponentAttached(IEntity entity) {};
    default void onComponentDetached(IEntity entity) {};
}
