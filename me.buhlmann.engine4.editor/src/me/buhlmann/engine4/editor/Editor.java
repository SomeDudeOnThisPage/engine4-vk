package me.buhlmann.engine4.editor;

import me.buhlmann.engine4.api.IEngine;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred.VulkanDeferredRenderer3D;
import me.buhlmann.engine4.editor.scenes.EditorScene;
import me.buhlmann.engine4.editor.scenes.LoadingScene;
import me.buhlmann.engine4.platform.window.GLFWWindow;
import org.joml.Vector2i;

public class Editor
{
    public static void main(final String[] args)
    {
        final IEngine.EngineCreationInfo info = new IEngine.EngineCreationInfo(
            VulkanDeferredRenderer3D.class,
            LoadingScene.class,
            new GLFWWindow(new Vector2i(800, 600)),
            args
        );
        final IEngine engine = IEngine.create(info);
        engine.start();
    }
}
