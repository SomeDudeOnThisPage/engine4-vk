package me.buhlmann.engine4.entity.component;

import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityComponent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.CallbackI;

@SuppressWarnings("unused")
public final class TransformComponent implements IEntityComponent
{
    private final Vector3f position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    public void setPosition(final Vector3f position)
    {
        this.position.set(position);
    }

    public Vector3f getPosition()
    {
        return this.position;
    }

    public void setRotation(final Quaternionf rotation)
    {
        this.rotation.set(rotation);
    }

    public Quaternionf getRotation()
    {
        return this.rotation;
    }

    public void setScale(final Vector3f scale)
    {
        this.scale.set(scale);
    }

    public Vector3f getScale()
    {
        return this.scale;
    }

    public Matrix4f getTransformMatrix()
    {
        return new Matrix4f()
            .identity()
            .rotate(this.rotation)
            .translate(this.position)
            .scale(this.scale);
    }

    @Override
    public void onComponentAttached(IEntity entity)
    {

    }

    @Override
    public void onComponentDetached(IEntity entity)
    {

    }

    public TransformComponent(final Vector3f position, final Quaternionf rotation, final Vector3f scale)
    {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }
}
