package me.buhlmann.engine4.entity.component;

import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityComponent;
import org.joml.Matrix4f;
import org.joml.Vector2i;

public class ProjectionComponent implements IEntityComponent
{
    private final Matrix4f projection;

    public Matrix4f getProjection()
    {
        return this.projection;
    }

    public void setProjection(float fov, Vector2i aspect, float near, float far)
    {
        this.projection.identity().perspective(fov, aspect.x / (float) aspect.y, near, far, true);
    }

    @Override
    public void onComponentAttached(IEntity entity)
    {

    }

    @Override
    public void onComponentDetached(IEntity entity)
    {

    }

    public ProjectionComponent()
    {
        this.projection = new Matrix4f();
    }
}
