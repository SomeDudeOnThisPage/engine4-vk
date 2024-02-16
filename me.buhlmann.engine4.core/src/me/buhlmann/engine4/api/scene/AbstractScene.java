package me.buhlmann.engine4.api.scene;

import me.buhlmann.engine4.api.entity.IEntityComponentSystem;
import me.buhlmann.engine4.entity.EntityComponentSystem;

public abstract class AbstractScene implements IScene
{
    private final IEntityComponentSystem ecs;

    @Override
    public IEntityComponentSystem getECS()
    {
        return this.ecs;
    }

    protected AbstractScene()
    {
        this.ecs = new EntityComponentSystem();
    }
}
