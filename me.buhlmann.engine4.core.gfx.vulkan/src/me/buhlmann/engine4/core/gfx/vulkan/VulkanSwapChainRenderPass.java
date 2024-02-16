package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class VulkanSwapChainRenderPass implements IVulkanPointer, IVulkanDisposable
{
    private final long pointer;

    private final VulkanDeviceContext vkContext;

    @Override
    public void dispose()
    {
        VK11.vkDestroyRenderPass(this.vkContext.getLogicalDevice().getNative(), this.pointer, null);
    }

    @Override
    public long getPointer()
    {
        return this.pointer;
    }

    public VulkanSwapChainRenderPass(final VulkanRenderContext vkContext, int depthImageFormat)
    {
        this.vkContext = vkContext;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            // create color attachment
            final VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(depthImageFormat == -1 ? 1 : 2, stack);
            attachments.get(0)
                .format(vkContext.getSwapChain().getSurfaceFormat().format())
                .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            final VkAttachmentReference.Buffer colorReference = VkAttachmentReference.callocStack(1, stack)
                .attachment(0)
                .layout(VK11.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthReference = null;
            if (depthImageFormat != -1)
            {
                // create depth attachment
                attachments.get(1)
                    .format(depthImageFormat)
                    .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK11.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

                depthReference = VkAttachmentReference.callocStack(stack)
                    .attachment(1)
                    .layout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            }


            final VkSubpassDescription.Buffer subPass = VkSubpassDescription.callocStack(1, stack)
                .pipelineBindPoint(VK11.VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(colorReference.remaining())
                .pColorAttachments(colorReference);

            if (depthReference != null)
            {
                subPass.pDepthStencilAttachment(depthReference);
            }

            final VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.callocStack(1, stack);
            subpassDependencies.get(0)
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
                .pDependencies(subpassDependencies);

            final LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateRenderPass(this.vkContext.getLogicalDevice().getNative(), renderPassInfo, null, lp),
                "Failed to create render pass");
            this.pointer = lp.get(0);
        }
    }
}
