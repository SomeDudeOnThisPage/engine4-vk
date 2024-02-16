package me.buhlmann.engine4.api.renderer;

import me.buhlmann.engine4.api.entity.IEntity;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public interface ICamera extends IEntity
{
    void setProjection(float fov, Vector2i viewport, Vector2f planes);
    Vector3f getPosition();
    Matrix4f getTransform();
    Matrix4f getProjectionMatrix();
}
