package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

public class VulkanCommandPool implements IVulkanPointer, IVulkanDisposable
{
    private final long pointer;
    private final VulkanDeviceContext vkContext;

    @Override
    public long getPointer()
    {
        return this.pointer;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyCommandPool(this.vkContext.getLogicalDevice().getNative(), this.pointer, null);
    }

    public VulkanCommandPool(VulkanDeviceContext vkContext, int queueFamilyIndex)
    {
        this.vkContext = vkContext;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK11.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(queueFamilyIndex);

            final LongBuffer pointer = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateCommandPool(this.vkContext.getLogicalDevice().getNative(), cmdPoolInfo, null, pointer), "Failed to create command pool");
            this.pointer = pointer.get(0);

            Engine4.getLogger().trace(String.format("[VULKAN] created command pool %dl", this.pointer));
        }
    }
}
