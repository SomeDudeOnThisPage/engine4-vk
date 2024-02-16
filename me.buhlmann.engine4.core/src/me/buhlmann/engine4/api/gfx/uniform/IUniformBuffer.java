package me.buhlmann.engine4.api.gfx.uniform;

import me.buhlmann.engine4.api.IAsset;

public interface IUniformBuffer extends IAsset
{
    <T> void set(final String name, final T data);
}
