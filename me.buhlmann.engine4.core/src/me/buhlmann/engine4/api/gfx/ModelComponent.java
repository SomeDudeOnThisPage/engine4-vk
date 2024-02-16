package me.buhlmann.engine4.api.gfx;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityComponent;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.primitive.IMesh;
import me.buhlmann.engine4.api.gfx.primitive.IModel;

public class ModelComponent implements IEntityComponent
{
    private IModel model;

    public void setModel(final IModel model)
    {
        this.model = model;
    }

    public IModel getModel()
    {
        return this.model;
    }

    public ModelComponent setMesh(final int index, final IMesh mesh)
    {
        this.getModel().getMeshes()[index] = mesh;
        return this;
    }

    public ModelComponent setMaterial(final int index, final IMaterial material)
    {
        this.getModel().setMaterial(index, material);
        return this;
    }

    @Override
    public void onComponentAttached(IEntity entity)
    {

    }

    @Override
    public void onComponentDetached(IEntity entity)
    {

    }

    public ModelComponent(final String model)
    {
        this.model = Engine4.getAssetManager().get(IModel.class, model);
    }
}
