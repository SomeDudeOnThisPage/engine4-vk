package me.buhlmann.engine4.editor.entity;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.annotation.EventListener;
import me.buhlmann.engine4.api.entity.Entity;
import me.buhlmann.engine4.api.renderer.ICamera;
import me.buhlmann.engine4.entity.component.ProjectionComponent;
import me.buhlmann.engine4.entity.component.TransformComponent;
import me.buhlmann.engine4.event.WindowResizeEvent;
import org.joml.*;

public class CameraEntity extends Entity implements ICamera
{
    private final TransformComponent transform;
    private final ProjectionComponent projection;

    public final TransformComponent getTransformComponent()
    {
        return this.transform;
    }

    public final ProjectionComponent getProjectionComponent()
    {
        return this.projection;
    }

    @EventListener(WindowResizeEvent.class)
    public void onWindowResize(final WindowResizeEvent event)
    {
        Engine4.getLogger().info("camera - " + event.getName() + " : " + event.getSize());
        this.setProjection(0.67f, event.getSize(), new Vector2f(0.01f, 100.0f));
    }

    @Override
    public void setProjection(final float fov, final Vector2i viewport, final Vector2f planes)
    {
        this.projection.setProjection(fov, viewport, planes.x(), planes.y());
    }

    public CameraEntity(String id)
    {
        super(id);
        this.transform = new TransformComponent(
            new Vector3f(0.0f, 0.0f, 0.0f),
            new Quaternionf().identity(),
            new Vector3f(1.0f)
        );

        this.projection = new ProjectionComponent();

        this.addComponent(this.transform);
        this.addComponent(this.projection);
    }

    @Override
    public Vector3f getPosition()
    {
        return this.getTransformComponent().getPosition();
    }

    @Override
    public Matrix4f getTransform()
    {
        return this.transform.getTransformMatrix();
    }

    @Override
    public Matrix4f getProjectionMatrix()
    {
        return this.projection.getProjection();
    }
}
