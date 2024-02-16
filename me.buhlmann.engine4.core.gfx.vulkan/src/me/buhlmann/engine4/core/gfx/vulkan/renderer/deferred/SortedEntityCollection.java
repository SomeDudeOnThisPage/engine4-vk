package me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityCollection;
import me.buhlmann.engine4.api.entity.IEntityComponent;
import me.buhlmann.engine4.api.gfx.primitive.IModel;
import me.buhlmann.engine4.api.utils.ICollectionMap;
import me.buhlmann.engine4.api.gfx.ModelComponent;
import me.buhlmann.engine4.entity.component.TransformComponent;
import me.buhlmann.engine4.utils.SetMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class SortedEntityCollection implements IEntityCollection // <I> (index)
{
    private final ICollectionMap</* I */ String, IEntity> entities;

    public SortedEntityCollection()
    {
        this.entities = new SetMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IEntityComponent> Class<T>[] components()
    {
        return new Class[]
        {
            TransformComponent.class,
            ModelComponent.class
        };
    }

    @Override
    public void addEntity(final IEntity entity)
    {
        Engine4.getLogger().info(entity.getIdentifier());
        final ModelComponent component = entity.getComponent(ModelComponent.class);
        final IAssetReference<IModel> model = component.getModelReference();

        this.entities.put(model.getKey(), entity);
    }

    @Override
    public void removeEntity(final IEntity entity)
    {
        Engine4.getLogger().info(entity.getIdentifier() + " remove");
        final ModelComponent component = entity.getComponent(ModelComponent.class);
        final IAssetReference<IModel> model = component.getModelReference();

        this.entities.remove(model.getKey(), entity);
    }

    public Collection<IEntity> getEntitiesByIndex(final /* I */ String index)
    {
        return this.entities.getAll(index);
    }

    public Set<String> getKeySet()
    {
        return this.entities.keySet();
    }

    @Override
    public Collection<IEntity> getEntities()
    {
        Engine4.getLogger().warning("attempted to access entities");
        return Collections.emptySet();
    }
}
