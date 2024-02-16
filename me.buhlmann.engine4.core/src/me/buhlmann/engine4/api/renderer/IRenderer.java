package me.buhlmann.engine4.api.renderer;

import me.buhlmann.engine4.api.scene.IScene;
import me.buhlmann.engine4.platform.window.GLFWWindow;

public interface IRenderer
{
    record Input(GLFWWindow window, IScene scene, ICamera camera)
    {

    }

    void initialize(GLFWWindow window);

    void bind(final IScene scene);

    void render(final IRenderer.Input input);

    void finish();

    void terminate();
}
