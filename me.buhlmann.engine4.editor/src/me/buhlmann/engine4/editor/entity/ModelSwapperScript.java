package me.buhlmann.engine4.editor.entity;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityScript;
import me.buhlmann.engine4.api.gfx.ModelComponent;
import me.buhlmann.engine4.api.gfx.primitive.IMesh;
import me.buhlmann.engine4.platform.Keyboard;

public class ModelSwapperScript implements IEntityScript
{
    @Override
    public void update(float dt, IEntity entity)
    {
        if (Engine4.getKeyboard().isDown(Keyboard.Key.Q))
        {
            entity.getComponent(ModelComponent.class).setMesh(0, Engine4.getAssetManager().get(IMesh.class, "model.triangle#Plane"));
        }
        else
        {
            entity.getComponent(ModelComponent.class).setMesh(0, Engine4.getAssetManager().get(IMesh.class, "model.cube#Cube"));
        }
    }
}
