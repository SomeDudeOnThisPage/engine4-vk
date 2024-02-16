package me.buhlmann.engine4.api.entity;

import java.util.Collection;
import java.util.UUID;

public interface IEntity
{
    UUID getUUID();
    String getIdentifier();

    <T extends IEntityComponent> IEntity addComponent(T component);
    <T extends IEntityComponent> IEntity removeComponent(Class<T> component);

    <T extends IEntityComponent> boolean hasComponent(Class<T> component);
    <T extends IEntityComponent> T getComponent(Class<T> component);

    Collection<? extends IEntityComponent> getComponents();
}
