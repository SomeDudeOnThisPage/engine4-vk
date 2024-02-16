package me.buhlmann.engine4.api.gfx.material;

import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.gfx.texture.ITexture;
import org.joml.Vector3f;

import java.util.List;

public interface IMaterial extends IAsset
{
    record Uniform<T>(String location, T data) {}
    record Sampler(int binding, IAssetReference<ITexture> sampler) {}
}
