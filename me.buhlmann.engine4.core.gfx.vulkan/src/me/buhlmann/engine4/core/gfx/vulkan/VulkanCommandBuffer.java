package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public class VulkanCommandBuffer implements IVulkanNative<VkCommandBuffer>, IVulkanDisposable
{
    public record VulkanInheritanceInfo(long vkRenderPass, long vkFrameBuffer, int subPass) {}

    private final VulkanDeviceContext vkContext;

    private final VkCommandBuffer vkCommandBuffer;
    private final VulkanCommandPool vkCommandPool;
    private final boolean oneTimeSubmit;
    private final boolean primary;

    @Override
    public VkCommandBuffer getNative()
    {
        return this.vkCommandBuffer;
    }

    @Override
    public void dispose()
    {
        VK11.vkFreeCommandBuffers(this.vkContext.getLogicalDevice().getNative(), this.vkCommandPool.getPointer(), this.vkCommandBuffer);
    }

    public void begin(VulkanCommandBuffer.VulkanInheritanceInfo inheritanceInfo)
    {
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (this.oneTimeSubmit)
            {
                cmdBufInfo.flags(VK11.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            }

            if (!this.primary)
            {
                if (inheritanceInfo == null)
                {
                    throw new RuntimeException("Secondary buffers must declare inheritance info");
                }

                final VkCommandBufferInheritanceInfo vkInheritanceInfo = VkCommandBufferInheritanceInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
                    .renderPass(inheritanceInfo.vkRenderPass)
                    .subpass(inheritanceInfo.subPass)
                    .framebuffer(inheritanceInfo.vkFrameBuffer);

                cmdBufInfo.pInheritanceInfo(vkInheritanceInfo);
                cmdBufInfo.flags(VK11.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);
            }

            VulkanUtils.check(VK11.vkBeginCommandBuffer(this.vkCommandBuffer, cmdBufInfo), "Failed to begin command buffer");
        }
    }

    public void end()
    {
        VulkanUtils.check(VK11.vkEndCommandBuffer(this.vkCommandBuffer), "Failed to end command buffer");
    }

    public void reset()
    {
        VK11.vkResetCommandBuffer(this.vkCommandBuffer, VK11.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
    }

    public VulkanCommandBuffer(VulkanRenderContext vkContext, boolean primary, boolean oneTimeSubmit)
    {
        this.vkContext = vkContext;
        this.vkCommandPool = vkContext.getCommandPool();
        this.primary = primary;
        this.oneTimeSubmit = oneTimeSubmit;
        final VkDevice vkDevice = this.vkContext.getLogicalDevice().getNative();

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(this.vkCommandPool.getPointer())
                .level(primary ? VK11.VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK11.VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                .commandBufferCount(1);

            final PointerBuffer pointer = stack.mallocPointer(1);
            VulkanUtils.check(VK11.vkAllocateCommandBuffers(vkDevice, cmdBufAllocateInfo, pointer), "Failed to allocate render command buffer");
            this.vkCommandBuffer = new VkCommandBuffer(pointer.get(0), vkDevice);

            Engine4.getLogger().trace(String.format("[VULKAN] created command buffer %dl", pointer.get(0)));
        }
    }
}
