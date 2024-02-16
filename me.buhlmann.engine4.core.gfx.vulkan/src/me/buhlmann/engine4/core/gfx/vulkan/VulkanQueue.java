package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanQueue implements IVulkanNative<VkQueue>, IVulkanDisposable
{
    public static class VulkanPresentQueue extends VulkanQueue
    {
        public VulkanPresentQueue(VulkanRenderContext vkContext, int index)
        {
            super(vkContext, VulkanQueue.VulkanPresentQueue.getPresentQueueFamilyIndex(vkContext), index);
        }

        private static int getPresentQueueFamilyIndex(VulkanRenderContext vkContext)
        {
            int index = -1;
            try (final MemoryStack stack = MemoryStack.stackPush())
            {
                final VkQueueFamilyProperties.Buffer properties = vkContext.getPhysicalDevice().getQueueFamilyProperties();
                final IntBuffer pointer = stack.mallocInt(1);

                for (int i = 0; i < properties.capacity(); i++)
                {
                    KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(vkContext.getPhysicalDevice().getNative(), i, vkContext.getSurface().getPointer(), pointer);

                    boolean supportsPresentation = pointer.get(0) == VK11.VK_TRUE;
                    if (supportsPresentation)
                    {
                        index = i;
                        break;
                    }
                }
            }

            if (index < 0)
            {
                throw new RuntimeException("Failed to get Presentation Queue family index");
            }

            return index;
        }
    }

    public static class VulkanGraphicsQueue extends VulkanQueue
    {
        public VulkanGraphicsQueue(final VulkanDeviceContext context, int index)
        {
            super(context, VulkanGraphicsQueue.getGraphicsQueueFamilyIndex(context), index);
        }

        private static int getGraphicsQueueFamilyIndex(VulkanDeviceContext context)
        {
            int index = -1;
            final VulkanPhysicalDevice physicalDevice = context.getPhysicalDevice();
            final VkQueueFamilyProperties.Buffer queuePropsBuff = physicalDevice.getQueueFamilyProperties();
            int numQueuesFamilies = queuePropsBuff.capacity();

            for (int i = 0; i < numQueuesFamilies; i++)
            {
                final VkQueueFamilyProperties props = queuePropsBuff.get(i);
                boolean graphicsQueue = (props.queueFlags() & VK11.VK_QUEUE_GRAPHICS_BIT) != 0;
                if (graphicsQueue)
                {
                    index = i;
                    break;
                }
            }

            if (index < 0)
            {
                throw new RuntimeException("Failed to get graphics Queue family index");
            }

            return index;
        }
    }

    private final int index;
    private final VkQueue vkQueue;

    @Override
    public void dispose()
    {

    }

    @Override
    public VkQueue getNative()
    {
        return this.vkQueue;
    }

    public int getIndex()
    {
        return this.index;
    }

    public void idle()
    {
        VK11.vkQueueWaitIdle(this.vkQueue);
    }

    public void submit(PointerBuffer commandBuffers, LongBuffer waitSemaphores, IntBuffer dstStageMasks, LongBuffer signalSemaphores, VulkanFence fence)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(commandBuffers)
                .pSignalSemaphores(signalSemaphores);

            if (waitSemaphores != null)
            {
                submitInfo.waitSemaphoreCount(waitSemaphores.capacity())
                    .pWaitSemaphores(waitSemaphores)
                    .pWaitDstStageMask(dstStageMasks);
            }
            else
            {
                submitInfo.waitSemaphoreCount(0);
            }

            long fenceHandle = fence != null ? fence.getPointer() : VK11.VK_NULL_HANDLE;
            VulkanUtils.check(VK11.vkQueueSubmit(vkQueue, submitInfo, fenceHandle),
                "Failed to submit command to queue");
        }
    }

    public VulkanQueue(final VulkanDeviceContext context, int family, int queue)
    {
        this.index = family;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final PointerBuffer pQueue = stack.mallocPointer(1);
            VK11.vkGetDeviceQueue(context.getLogicalDevice().getNative(), family, queue, pQueue);
            this.vkQueue = new VkQueue(pQueue.get(0), context.getLogicalDevice().getNative());
        }
    }
}
