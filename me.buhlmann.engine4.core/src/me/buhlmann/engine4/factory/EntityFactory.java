package me.buhlmann.engine4.factory;

import me.buhlmann.engine4.api.entity.Entity;
import me.buhlmann.engine4.api.scene.IScene;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityCollection;
import me.buhlmann.engine4.api.entity.IEntityComponent;

public class EntityFactory
{
    @SuppressWarnings("UnusedReturnValue")
    public static IEntity createInScene(final IScene scene, final String key, final IEntityComponent... components)
    {
        final IEntity entity = new Entity(key);
        for (IEntityComponent component : components)
        {
            entity.addComponent(component);
        }

        scene.getECS().add(entity);
        return entity;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <T extends IEntity> T createInScene(final T entity, final IScene scene, final IEntityComponent... components)
    {
        for (IEntityComponent component : components)
        {
            entity.addComponent(component);
        }

        scene.getECS().add(entity);
        return entity;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <T extends IEntityCollection> T createInScene(final IScene scene, final T collection)
    {
        scene.getECS().add(collection);
        return collection;
    }
}
