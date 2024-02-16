package me.buhlmann.engine4.api.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractEntityCollection implements IEntityCollection
{
    private final Set<IEntity> entities;

    @Override
    public void addEntity(IEntity entity)
    {
        this.entities.add(entity);
    }

    @Override
    public void removeEntity(IEntity entity)
    {
        this.entities.remove(entity);
    }

    @Override
    public Collection<IEntity> getEntities()
    {
        return this.entities;
    }

    public AbstractEntityCollection()
    {
        this.entities = new HashSet<>();
    }
}
