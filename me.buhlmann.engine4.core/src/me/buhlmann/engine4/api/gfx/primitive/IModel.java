package me.buhlmann.engine4.api.gfx.primitive;

import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.asset2.IAsset2;
import me.buhlmann.engine4.api.gfx.material.IMaterial;

import java.util.List;

public interface IModel extends IAsset2
{
    IMesh[] getMeshes();

    void setMaterial(final int index, IMaterial material);
    IMaterial[] getMaterials();
}
