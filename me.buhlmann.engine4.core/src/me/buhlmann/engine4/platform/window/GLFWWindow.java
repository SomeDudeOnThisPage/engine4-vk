package me.buhlmann.engine4.platform.window;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.exception.EngineInitializationException;
import me.buhlmann.engine4.api.exception.GLFWException;
import me.buhlmann.engine4.event.WindowResizeEvent;
import org.joml.Vector2i;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.system.MemoryUtil.NULL;

public final class GLFWWindow
{
    private final long handle;

    private WindowState state;
    private final Vector2i size;

    public float getAspectRatio()
    {
        return (float) this.size.x / (float) this.size.y;
    }

    public long getHandle()
    {
        return this.handle;
    }

    public void poll()
    {
        GLFW.glfwPollEvents();
    }

    public void swapBuffers()
    {
        // GLFW.glfwSwapBuffers(this.handle);
    }

    public boolean shouldClose()
    {
        return GLFW.glfwWindowShouldClose(this.handle);
    }

    public void close()
    {
        GLFW.glfwSetWindowShouldClose(this.handle, true);
    }

    public Vector2i getSize()
    {
        return this.size;
    }

    public void terminate()
    {
        GLFW.glfwTerminate();
    }

    public GLFWWindow(Vector2i size) throws GLFWException
    {
        GLFW.glfwSetErrorCallback((final int code, final long description) -> {
            throw new GLFWException(code, description);
        });

        if (!GLFW.glfwInit()) {
            throw new GLFWException(0, 0L);
        }

        this.size = size;
        this.state = WindowState.WINDOWED;

        final GLFWVidMode vm = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        this.size.set(vm != null ? vm.width() : 800, vm != null ? vm.height() : 600);

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        this.handle = GLFW.glfwCreateWindow(this.size.x, this.size.y, "Test", NULL, NULL);

        GLFW.glfwSetWindowSizeCallback(this.handle, (handle, x, y) -> {
            this.size.set(x, y);
            Engine4.getEventBus().publish(new WindowResizeEvent(new Vector2i(x, y)));
        });

        GLFW.glfwSetFramebufferSizeCallback(this.handle, (handle, x, y) -> {
            // Engine4.getEventBus().publish(new DisplayEvent(new Vector2i(x, y)));
        });
    }
}

