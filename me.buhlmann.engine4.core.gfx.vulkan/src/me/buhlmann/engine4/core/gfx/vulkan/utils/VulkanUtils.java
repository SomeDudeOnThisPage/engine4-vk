package me.buhlmann.engine4.core.gfx.vulkan.utils;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.*;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanBuffer;
import me.buhlmann.engine4.platform.window.GLFWWindow;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class VulkanUtils
{
    public static void copy(VulkanBuffer vkBuffer, Matrix4f matrix, int offset)
    {
        final ByteBuffer buffer = MemoryUtil.memByteBuffer(vkBuffer.map(), (int) vkBuffer.getRequestedSize());
        matrix.get(offset, buffer);
        vkBuffer.unmap();
    }

    public static void copy(VulkanBuffer vkBuffer, Vector4f vector, int offset)
    {
        final ByteBuffer buffer = MemoryUtil.memByteBuffer(vkBuffer.map(), (int) vkBuffer.getRequestedSize());
        vector.get(offset, buffer);
        vkBuffer.unmap();
    }

    public static void copy(VulkanBuffer vkBuffer, Integer integer, int offset)
    {
        final IntBuffer buffer = MemoryUtil.memByteBuffer(vkBuffer.map(), (int) vkBuffer.getRequestedSize()).asIntBuffer();
        buffer.put(offset / 4, integer);
        vkBuffer.unmap();
    }

    public static void copy(VulkanBuffer vkBuffer, Float ft, int offset)
    {
        final FloatBuffer buffer = MemoryUtil.memByteBuffer(vkBuffer.map(), (int) vkBuffer.getRequestedSize()).asFloatBuffer();
        buffer.put(offset / 4, ft);
        vkBuffer.unmap();
    }

    public static void check(int err, String message)
    {
        if (err != VK11.VK_SUCCESS)
        {
            Engine4.getLogger().error("[VULKAN] " + message + " : " + err);
            throw new RuntimeException(message + " : " + err);
        }
    }

    public static int getMemoryTypeFromProperties(VulkanPhysicalDevice physDevice, int typeBits, int reqsMask)
    {
        int result = -1;
        VkMemoryType.Buffer memoryTypes = physDevice.getMemoryProperties().memoryTypes();
        for (int i = 0; i < VK11.VK_MAX_MEMORY_TYPES; i++)
        {
            if ((typeBits & 1) == 1 && (memoryTypes.get(i).propertyFlags() & reqsMask) == reqsMask)
            {
                result = i;
                break;
            }

            typeBits >>= 1;
        }

        if (result < 0)
        {
            throw new RuntimeException("Failed to find memoryType");
        }

        return result;
    }

    public static VulkanDeviceContext newDeviceContext()
    {
        final VulkanInstance vkInstance = new VulkanInstance(true);
        final VulkanPhysicalDevice vkPhysicalDevice = VulkanPhysicalDevice.createPhysicalDevice(vkInstance, null);
        final VulkanLogicalDevice vkLogicalDevice = new VulkanLogicalDevice(vkPhysicalDevice);
        return new VulkanDeviceContext(vkInstance, vkPhysicalDevice, vkLogicalDevice);
    }

    public static VulkanRenderContext newRenderContext(final GLFWWindow window)
    {
        final VulkanInstance vkInstance = new VulkanInstance(true);
        final VulkanPhysicalDevice vkPhysicalDevice = VulkanPhysicalDevice.createPhysicalDevice(vkInstance, null);
        final VulkanLogicalDevice vkLogicalDevice = new VulkanLogicalDevice(vkPhysicalDevice);

        final VulkanSurface vkSurface = new VulkanSurface(vkInstance, vkPhysicalDevice, window);
        final VulkanSwapChain vkSwapChain = new VulkanSwapChain(
            vkLogicalDevice,
            vkPhysicalDevice,
            vkSurface,
            3, // lazy...
            true // lazy...
        );

        final VulkanRenderContext vkContext = new VulkanRenderContext(
            vkInstance,
            vkPhysicalDevice,
            vkLogicalDevice,
            vkSurface,
            vkSwapChain
        );

        vkContext.setGraphicsQueue(new VulkanQueue.VulkanGraphicsQueue(vkContext, 0));
        vkContext.setPresentQueue(new VulkanQueue.VulkanPresentQueue(vkContext, 0));

        return vkContext;
    }

    public static VulkanSwapChain.VulkanSurfaceFormat newSurfaceFormat(final VulkanRenderContext vkContext)
    {
        final VulkanPhysicalDevice vkDevice = vkContext.getPhysicalDevice();
        final VulkanSurface vkSurface = vkContext.getSurface();

        int imageFormat;
        int colorSpace;
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final IntBuffer ip = stack.mallocInt(1);
            VulkanUtils.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(vkDevice.getNative(), vkSurface.getPointer(), ip, null),
                "Failed to get the number surface formats");

            final int numFormats = ip.get(0);
            if (numFormats <= 0)
            {
                throw new RuntimeException("No surface formats retrieved");
            }

            VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.callocStack(numFormats, stack);
            VulkanUtils.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(vkDevice.getNative(), vkSurface.getPointer(), ip, surfaceFormats), "Failed to get surface formats");

            imageFormat = VK11.VK_FORMAT_B8G8R8A8_SRGB;
            colorSpace = surfaceFormats.get(0).colorSpace();
            for (int i = 0; i < numFormats; i++)
            {
                final VkSurfaceFormatKHR surfaceFormatKHR = surfaceFormats.get(i);
                if (surfaceFormatKHR.format() == VK11.VK_FORMAT_B8G8R8A8_SRGB && surfaceFormatKHR.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                {
                    imageFormat = surfaceFormatKHR.format();
                    colorSpace = surfaceFormatKHR.colorSpace();
                    break;
                }
            }
        }
        return new VulkanSwapChain.VulkanSurfaceFormat(imageFormat, colorSpace);
    }
}
