package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.platform.window.GLFWWindow;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;

import java.nio.LongBuffer;

public class VulkanSurface implements IVulkanPointer, IVulkanDisposable
{
    private final long surface;
    private final VulkanInstance vkInstance;
    private final VulkanPhysicalDevice vkPhysicalDevice;

    @Override
    public long getPointer()
    {
        return this.surface;
    }

    @Override
    public void dispose()
    {
        KHRSurface.vkDestroySurfaceKHR(this.vkPhysicalDevice.getNative().getInstance(), this.surface, null);
    }

    public VulkanSurface(final VulkanInstance vkInstance, final VulkanPhysicalDevice vkPhysicalDevice, final GLFWWindow window)
    {
        this.vkInstance = vkInstance;
        this.vkPhysicalDevice = vkPhysicalDevice;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            LongBuffer pSurface = stack.mallocLong(1);
            GLFWVulkan.glfwCreateWindowSurface(this.vkInstance.getNative(), window.getHandle(), null, pSurface);
            this.surface = pSurface.get(0);
        }
    }
}
