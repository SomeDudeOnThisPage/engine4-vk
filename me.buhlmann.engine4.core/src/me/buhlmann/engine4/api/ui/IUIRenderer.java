package me.buhlmann.engine4.api.ui;

import me.buhlmann.engine4.api.renderer.IRenderer;

public interface IUIRenderer
{
    void render(final IRenderer renderer, final IRenderer.Input input);
}
