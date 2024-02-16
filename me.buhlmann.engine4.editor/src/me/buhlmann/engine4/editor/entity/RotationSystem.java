package me.buhlmann.engine4.editor.entity;

import me.buhlmann.engine4.api.entity.AbstractEntityCollection;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityComponent;
import me.buhlmann.engine4.api.entity.IEntitySystem;
import me.buhlmann.engine4.entity.component.TransformComponent;

public class RotationSystem extends AbstractEntityCollection implements IEntitySystem
{
    @Override
    @SuppressWarnings("unchecked")
    public <T extends IEntityComponent> Class<T>[] components()
    {
        return new Class[]
        {
            TransformComponent.class
        };
    }

    @Override
    public void update(float dt)
    {
        for (final IEntity entity : this.getEntities())
        {
            entity.getComponent(TransformComponent.class).getRotation()
                .rotateX(0.01f)
                .rotateY(0.00f)
                .rotateZ(0.00f);
        }
    }
}
