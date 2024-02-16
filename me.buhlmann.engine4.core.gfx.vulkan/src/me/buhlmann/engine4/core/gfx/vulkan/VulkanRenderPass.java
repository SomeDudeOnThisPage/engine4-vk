package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class VulkanRenderPass implements IVulkanPointer, IVulkanDisposable
{
    private final long id;

    private final VulkanDeviceContext vkContext;
    private final VulkanSwapChain vkSwapChain;

    @Override
    public long getPointer()
    {
        return this.id;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyRenderPass(this.vkContext.getLogicalDevice().getNative(), this.id, null);
    }

    public VulkanRenderPass(final VulkanDeviceContext vkContext, final VulkanSwapChain vkSwapChain)
    {
        this.vkContext = vkContext;
        this.vkSwapChain = vkSwapChain;

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(1, stack);

            // Color attachment
            attachments.get(0)
                .format(this.vkSwapChain.getSurfaceFormat().format())
                .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            final VkAttachmentReference.Buffer colors = VkAttachmentReference.callocStack(1, stack)
                .attachment(0)
                .layout(VK11.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            final VkSubpassDescription.Buffer subPass = VkSubpassDescription.callocStack(1, stack)
                .pipelineBindPoint(VK11.VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(colors.remaining())
                .pColorAttachments(colors);

            final VkSubpassDependency.Buffer subPassDependencies = VkSubpassDependency.callocStack(1, stack);
            subPassDependencies.get(0)
                .srcSubpass(VK11.VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstAccessMask(VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            final VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(subPass)
                .pDependencies(subPassDependencies);

            final LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateRenderPass(this.vkContext.getLogicalDevice().getNative(), renderPassInfo, null, lp),
                "Failed to create render pass");
            this.id = lp.get(0);
        }
    }
}
