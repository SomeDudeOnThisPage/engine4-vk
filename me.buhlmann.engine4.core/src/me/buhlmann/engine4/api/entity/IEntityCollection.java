package me.buhlmann.engine4.api.entity;

import java.util.Collection;

public interface IEntityCollection
{
    default void onEntityAdded(IEntity entity) { }
    default void onEntityRemoved(IEntity entity) { }

    <T extends IEntityComponent> Class<T>[] components();

    void addEntity(IEntity entity);
    void removeEntity(IEntity entity);
    Collection<IEntity> getEntities();
}
