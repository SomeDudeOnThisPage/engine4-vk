package me.buhlmann.engine4.entity;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.entity.*;
import me.buhlmann.engine4.api.utils.ICollectionMap;
import me.buhlmann.engine4.utils.SetMap;

import java.util.*;

public class EntityComponentSystem implements IEntityComponentSystem
{
    private final Map<String, IEntity> entities;
    private final ICollectionMap<IEntity, IEntityScript> scripts;

    private final Map<Class<? extends IEntityCollection>, IEntityCollection> collections;
    private final List<IEntitySystem> systems;

    private final Queue<EntityComponentSystemOperation> operations;

    private void map(IEntity entity, IEntityCollection collection)
    {
        final Class<? extends IEntityComponent>[] components = collection.components();
        if (components.length > 0)
        {
            boolean hasRequired = true;
            for (Class<? extends IEntityComponent> listen : components)
            {
                if (!entity.hasComponent(listen))
                {
                    hasRequired = false;
                    break;
                }
            }

            if (hasRequired)
            {
                Engine4.getLogger().trace("[ECS] added entity '" + entity.getIdentifier() + "' to collection '"  + collection + "'");
                collection.addEntity(entity);
                collection.onEntityAdded(entity);
            }
            else
            {
                if (collection.getEntities().contains(entity))
                {
                    collection.removeEntity(entity);
                    collection.onEntityRemoved(entity);
                }
            }
        }
    }

    private void addCollection(IEntityCollection collection)
    {
        this.collections.put(collection.getClass(), collection);

        if (collection instanceof IEntitySystem)
        {
            this.systems.add((IEntitySystem) collection);
        }

        // map all entities to this collection
        for (IEntity entity : this.entities.values())
        {
            this.map(entity, collection);
        }

        Engine4.getEventBus().subscribe(collection);
    }

    private void removeCollection(IEntityCollection collection)
    {
        this.collections.remove(collection.getClass());

        if (collection instanceof IEntitySystem)
        {
            this.systems.remove(collection);
        }

        Engine4.getEventBus().unsubscribe(collection);
    }

    private void handleCollectionOperation(EntityComponentSystemOperation.OnCollection operation)
    {
        switch (operation.type)
        {
            case ADD -> this.addCollection(operation.collection);
            case REMOVE -> this.removeCollection(operation.collection);
            case MAP -> throw new UnsupportedOperationException("Map operation not supported for collections - use with entities instead.");
        }
    }

    private void handleEntityOperation(EntityComponentSystemOperation.OnEntity operation)
    {
        final IEntity entity = operation.entity;
        switch (operation.type)
        {
            case ADD ->
            {
                this.entities.put(entity.getIdentifier(), entity);
                Engine4.getEventBus().subscribe(entity);
                Engine4.getLogger().trace("[ECS] added entity '" + entity.getIdentifier() + "'");

                for (final IEntityComponent component : entity.getComponents())
                {
                    if (component instanceof IEntityScript script)
                    {
                        this.scripts.put(entity, script);
                    }
                }
            }
            case REMOVE ->
            {
                this.entities.remove(entity.getIdentifier());
                for (IEntityCollection collection : this.collections.values())
                {
                    collection.removeEntity(entity);
                }

                for (final IEntityComponent component : entity.getComponents())
                {
                    if (component instanceof IEntityScript script)
                    {
                        this.scripts.remove(entity, script);
                    }
                }

                Engine4.getEventBus().unsubscribe(entity);
                Engine4.getLogger().trace("[ECS] removed entity '" + entity.getIdentifier() + "'");
            }
            case MAP ->
            {
                for (IEntityCollection collection : this.collections.values())
                {
                    this.map(entity, collection);
                }
                Engine4.getLogger().trace("[ECS] mapped entity '" + entity.getIdentifier() + "'");
            }
        }
    }

    @Override
    public IEntity get(final String key)
    {
        if (!this.exists(key))
        {
            Engine4.getLogger().error("attempted to retrieve nonexistent entity " + key);
            return null;
        }

        return this.entities.get(key);
    }

    @Override
    public <T extends IEntityCollection> void add(T collection)
    {
        this.operations.add(new EntityComponentSystemOperation.OnCollection(collection, EntityComponentSystemOperation.Type.ADD));
    }

    @Override
    public <T extends IEntityCollection> void remove(T collection)
    {
        this.operations.add(new EntityComponentSystemOperation.OnCollection(collection, EntityComponentSystemOperation.Type.REMOVE));
    }

    @Override
    public void add(IEntity entity)
    {
        this.operations.add(new EntityComponentSystemOperation.OnEntity(entity, EntityComponentSystemOperation.Type.ADD));
        this.operations.add(new EntityComponentSystemOperation.OnEntity(entity, EntityComponentSystemOperation.Type.MAP));
    }

    @Override
    public void remove(IEntity entity)
    {
        this.operations.add(new EntityComponentSystemOperation.OnEntity(entity, EntityComponentSystemOperation.Type.REMOVE));
    }

    @Override
    public boolean exists(String id)
    {
        return this.entities.containsKey(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IEntityCollection> T get(Class<T> collection)
    {
        return (T) this.collections.get(collection);
    }

    @Override
    public void update(float dt)
    {
        while (!this.operations.isEmpty())
        {
            final EntityComponentSystemOperation operation = this.operations.poll();
            Engine4.getLogger().trace("[ECS] performing operation '" + operation.getClass().getSimpleName() + "'");
            if (EntityComponentSystemOperation.OnEntity.class == operation.getClass())
            {
                this.handleEntityOperation((EntityComponentSystemOperation.OnEntity) operation);
            }
            else if (EntityComponentSystemOperation.OnCollection.class == operation.getClass())
            {
                this.handleCollectionOperation((EntityComponentSystemOperation.OnCollection) operation);
            }
        }

        for (IEntitySystem system : this.systems)
        {
            system.update(dt);
        }

        for (final IEntity entity : this.scripts.keySet())
        {
            for (IEntityScript script : this.scripts.getAll(entity))
            {
                script.update(dt, entity);
            }
        }
    }

    public EntityComponentSystem()
    {
        this.entities = new HashMap<>();
        this.collections = new HashMap<>();
        this.systems = new ArrayList<>();
        this.scripts = new SetMap<>();
        this.operations = new LinkedList<>();
    }
}
