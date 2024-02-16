package me.buhlmann.engine4.core.gfx.vulkan.renderer;

import me.buhlmann.engine4.api.IDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanPointer;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

public class VulkanBuffer implements IVulkanPointer, IDisposable, IVulkanDisposable
{
    private final long vkBuffer;
    private final long vkMemory;
    private long vkMap;

    private final long size;
    private final long requested;

    private final VulkanDeviceContext vkContext;
    private final PointerBuffer pointers;

    @Override
    public void dispose()
    {
        MemoryUtil.memFree(this.pointers);
        VK11.vkDestroyBuffer(this.vkContext.getLogicalDevice().getNative(), this.vkBuffer, null);
        VK11.vkFreeMemory(this.vkContext.getLogicalDevice().getNative(), this.vkMemory, null);
    }

    @Override
    public long getPointer()
    {
        return this.vkBuffer;
    }

    public long getRequestedSize()
    {
        return this.requested;
    }

    public long map()
    {
        if (this.vkMap == MemoryUtil.NULL)
        {
            VulkanUtils.check(VK11.vkMapMemory(this.vkContext.getLogicalDevice().getNative(), this.vkMemory, 0, this.size, 0, this.pointers), "Failed to map Buffer");
            this.vkMap = this.pointers.get(0);
        }

        return this.vkMap;
    }

    public void unmap()
    {
        if (this.vkMap != MemoryUtil.NULL)
        {
            VK11.vkUnmapMemory(this.vkContext.getLogicalDevice().getNative(), this.vkMemory);
            this.vkMap = MemoryUtil.NULL;
        }
    }

    public VulkanBuffer(VulkanDeviceContext vkContext, long size, int usage, int mask)
    {
        this.vkContext = vkContext;
        this.requested = size;
        this.vkMap = MemoryUtil.NULL;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(usage)
                .sharingMode(VK11.VK_SHARING_MODE_EXCLUSIVE);

            final LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateBuffer(vkContext.getLogicalDevice().getNative(), bufferCreateInfo, null, lp), "Failed to create buffer");
            this.vkBuffer = lp.get(0);

            final VkMemoryRequirements requirements = VkMemoryRequirements.mallocStack(stack);
            VK11.vkGetBufferMemoryRequirements(vkContext.getLogicalDevice().getNative(), this.vkBuffer, requirements);

            final VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(requirements.size())
                .memoryTypeIndex(VulkanUtils.getMemoryTypeFromProperties(vkContext.getPhysicalDevice(), requirements.memoryTypeBits(), mask));

            VulkanUtils.check(VK11.vkAllocateMemory(vkContext.getLogicalDevice().getNative(), memAlloc, null, lp), "Failed to allocate memory");

            this.size = memAlloc.allocationSize();
            this.vkMemory = lp.get(0);
            this.pointers = MemoryUtil.memAllocPointer(1);

            VulkanUtils.check(VK11.vkBindBufferMemory(vkContext.getLogicalDevice().getNative(), this.vkBuffer, this.vkMemory, 0), "Failed to bind buffer memory");
        }
    }
}
