package me.buhlmann.engine4.api;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.renderer.IRenderer;
import me.buhlmann.engine4.api.scene.IScene;
import me.buhlmann.engine4.platform.window.GLFWWindow;

public interface IEngine
{
    record EngineCreationInfo(Class<? extends IRenderer> renderer, Class<? extends IScene> scene, GLFWWindow window, String[] args)
    {

    }

    static IEngine create(final EngineCreationInfo info)
    {
        // final GLFWWindow window = new GLFWWindow(new Vector2i(800, 600));
        // info.renderer().initialize(window);

        final IEngine engine = new Engine4(info);
        // Engine4.setInitialScene(info.scene());
        return engine;
    }

    void start();

}
