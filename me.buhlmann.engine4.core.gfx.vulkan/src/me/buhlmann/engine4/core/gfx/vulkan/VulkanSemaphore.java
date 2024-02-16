package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

public class VulkanSemaphore implements IVulkanPointer, IVulkanDisposable
{
    private final VulkanLogicalDevice device;
    private final long id;

    @Override
    public long getPointer()
    {
        return this.id;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroySemaphore(this.device.getNative(), this.id, null);
    }

    public VulkanSemaphore(VulkanLogicalDevice device)
    {
        this.device = device;
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            final LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateSemaphore(device.getNative(), semaphoreCreateInfo, null, lp),
                "Failed to create semaphore");
            this.id = lp.get(0);
        }
    }
}
