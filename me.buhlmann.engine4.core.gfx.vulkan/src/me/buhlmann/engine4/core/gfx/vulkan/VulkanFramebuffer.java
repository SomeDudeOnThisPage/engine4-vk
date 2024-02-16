package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

public class VulkanFramebuffer implements IVulkanPointer, IVulkanDisposable
{
    private final long id;
    private final VulkanLogicalDevice device;

    @Override
    public long getPointer()
    {
        return this.id;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyFramebuffer(this.device.getNative(), this.id, null);
    }

    public VulkanFramebuffer(VulkanLogicalDevice device, int width, int height, LongBuffer pAttachments, long renderPass)
    {
        this.device = device;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .pAttachments(pAttachments)
                .width(width)
                .height(height)
                .layers(1)
                .renderPass(renderPass);

            LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateFramebuffer(device.getNative(), fci, null, lp),
                "Failed to create FrameBuffer");
            this.id = lp.get(0);
        }
    }
}
