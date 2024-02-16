package me.buhlmann.engine4.api.gfx.material;

import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.renderer.IRenderer;

public interface IMaterialArchetype extends IAsset
{
    void bind(final IRenderer renderer);
}
