package me.buhlmann.engine4.entity;

import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityCollection;

public abstract class EntityComponentSystemOperation
{
    public enum Type
    {
        ADD,
        REMOVE,
        MAP
    }

    public static final class OnEntity extends EntityComponentSystemOperation
    {
        public IEntity entity;

        public OnEntity(IEntity entity, Type type)
        {
            super(type);
            this.entity = entity;
        }
    }

    public static final class OnCollection extends EntityComponentSystemOperation
    {
        public IEntityCollection collection;

        public OnCollection(IEntityCollection collection, Type type)
        {
            super(type);
            this.collection = collection;
        }
    }

    public Type type;

    public EntityComponentSystemOperation(Type type)
    {
        this.type = type;
    }
}
