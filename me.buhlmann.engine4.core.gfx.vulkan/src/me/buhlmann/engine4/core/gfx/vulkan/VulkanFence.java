package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

public class VulkanFence implements IVulkanPointer, IVulkanDisposable
{
    private final long id;
    private final VulkanLogicalDevice device;

    @Override
    public void dispose()
    {
        VK11.vkDestroyFence(this.device.getNative(), this.id, null);
    }

    public void hold()
    {
        VK11.vkWaitForFences(this.device.getNative(), this.id, true, Long.MAX_VALUE);
    }

    public void reset()
    {
        VK11.vkResetFences(this.device.getNative(), this.id);
    }

    @Override
    public long getPointer()
    {
        return this.id;
    }

    public VulkanFence(VulkanLogicalDevice device, boolean signaled)
    {
        this.device = device;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(signaled ? VK11.VK_FENCE_CREATE_SIGNALED_BIT : 0);

            final LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateFence(device.getNative(), fenceCreateInfo, null, lp),
                "Failed to create semaphore");
            this.id = lp.get(0);
        }
    }
}
