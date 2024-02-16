package me.buhlmann.engine4.api.entity;

import com.sun.istack.NotNull;

public interface IEntityComponentSystem
{
    void update(@NotNull float dt);

    <T extends IEntityCollection> T get(@NotNull Class<T> collection);

    IEntity get(final String key);

    boolean exists(@NotNull String id);

    void add(IEntity entity);
    void remove(IEntity entity);

    <T extends IEntityCollection> void add(T collection);
    <T extends IEntityCollection> void remove(T collection);
}
