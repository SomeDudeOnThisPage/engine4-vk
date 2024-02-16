package me.buhlmann.engine4.editor.entity;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.entity.*;
import me.buhlmann.engine4.entity.component.TransformComponent;
import me.buhlmann.engine4.platform.Keyboard;
import org.joml.Vector3f;

public class CameraMovementScript implements IEntityScript
{
    @Override
    public void update(final float dt, final IEntity entity)
    {
        final TransformComponent transform = entity.getComponent(TransformComponent.class);
        final Keyboard keyboard = Engine4.getKeyboard();

        if (keyboard.isDown(Keyboard.Key.W))
        {
            transform.setPosition(transform.getPosition().add(0.0f, 0.0f, dt * 10.0f));
        }

        if (keyboard.isDown(Keyboard.Key.S))
        {
            transform.setPosition(transform.getPosition().add(0.0f, 0.0f, -dt * 10.0f));
        }

        if (keyboard.isDown(Keyboard.Key.A))
        {
            transform.setPosition(transform.getPosition().add(dt * 10.0f, 0.0f, 0.0f));
        }

        if (keyboard.isDown(Keyboard.Key.D))
        {
            transform.setPosition(transform.getPosition().add(-dt * 10.0f, 0.0f, 0.0f));
        }

        if (keyboard.isDown(Keyboard.Key.SPACE))
        {
            transform.setPosition(transform.getPosition().add(0.0f, -dt * 10.0f, 0.0f));
        }

        if (keyboard.isDown(Keyboard.Key.LCONTROL))
        {
            transform.setPosition(transform.getPosition().add(0.0f, dt * 10.0f, 0.0f));
        }

        if (keyboard.isDown(Keyboard.Key.Q))
        {
            transform.setRotation(transform.getRotation().rotateAxis(dt * 3.0f, new Vector3f(0.0f, 1.0f, 0.0f)));
        }

        if (keyboard.isDown(Keyboard.Key.E))
        {
            transform.setRotation(transform.getRotation().rotateAxis(-dt * 3.0f, new Vector3f(0.0f, 1.0f, 0.0f)));
        }
    }
}
