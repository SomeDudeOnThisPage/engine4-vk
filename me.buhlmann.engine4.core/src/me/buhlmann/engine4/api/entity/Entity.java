package me.buhlmann.engine4.api.entity;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityComponent;

import java.util.*;

public class Entity implements IEntity
{
    private final UUID uuid;
    private final String id;
    private final Map<Class<? extends IEntityComponent>, IEntityComponent> components;

    @Override
    public UUID getUUID()
    {
        return this.uuid;
    }

    @Override
    public String getIdentifier()
    {
        return this.id;
    }

    @Override
    public <T extends IEntityComponent> IEntity addComponent(T component)
    {
        this.components.put(component.getClass(), component);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IEntityComponent> T getComponent(Class<T> component)
    {
        return (T) this.components.get(component);
    }

    @Override
    public Collection<? extends IEntityComponent> getComponents()
    {
        return this.components.values();
    }

    @Override
    public <T extends IEntityComponent> IEntity removeComponent(Class<T> component)
    {
        this.components.remove(component);
        return this;
    }

    @Override
    public <T extends IEntityComponent> boolean hasComponent(Class<T> component)
    {
        return this.components.containsKey(component);
    }

    public Entity(String id)
    {
        this.uuid = UUID.randomUUID();
        this.id = id;
        this.components = new HashMap<>();
        Engine4.getEventBus().subscribe(this);
    }
}
