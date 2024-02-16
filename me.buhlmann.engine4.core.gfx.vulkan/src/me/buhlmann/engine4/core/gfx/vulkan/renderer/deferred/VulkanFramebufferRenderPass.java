package me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanPointer;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanAttachment;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.List;

public class VulkanFramebufferRenderPass implements IVulkanPointer, IVulkanDisposable
{
    private final long vkRenderPass;
    private final VulkanDeviceContext vkContext;

    public VulkanFramebufferRenderPass(final VulkanRenderContext vkContext, final VulkanGBuffer vkGBuffer)
    {
        this.vkContext = vkContext;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkAttachmentDescription.Buffer vkAttachmentDescription = VkAttachmentDescription.callocStack(vkGBuffer.getAttachments().size(), stack);
            for (int i = 0; i < vkGBuffer.getAttachments().size(); i++)
            {
                final VulkanAttachment vkAttachment = vkGBuffer.getAttachments().get(i);
                // create color attachment
                vkAttachmentDescription.get(i)
                    .format(vkAttachment.getImage().getData().format())
                    .samples(VK11.VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK11.VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK11.VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK11.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK11.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .samples(1)
                    .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED);

                if (vkAttachment.isDepthAttachment())
                {
                    vkAttachmentDescription.get(i).finalLayout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
                }
                else
                {
                    vkAttachmentDescription.get(i).finalLayout(VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                }
            }

            final VkAttachmentReference.Buffer vkColorAttachmentReferences = VkAttachmentReference.callocStack(vkGBuffer.getAttachments().size() - 1, stack);
            for (int j = 0; j < vkGBuffer.getAttachments().size() - 1; j++)
            {
                vkColorAttachmentReferences.get(j)
                    .attachment(j)
                    .layout(VK11.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }

            final VkAttachmentReference vkDepthAttachmentReference = VkAttachmentReference.callocStack(stack)
                .attachment(vkGBuffer.getAttachments().size() - 1)
                .layout(VK11.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            final VkSubpassDescription.Buffer vkSubpassDescription = VkSubpassDescription.callocStack(1, stack)
                .pipelineBindPoint(VK11.VK_PIPELINE_BIND_POINT_GRAPHICS)
                .pColorAttachments(vkColorAttachmentReferences)
                .colorAttachmentCount(vkColorAttachmentReferences.capacity())
                .pDepthStencilAttachment(vkDepthAttachmentReference);

            final VkSubpassDependency.Buffer vkSubpassDependency = VkSubpassDependency.callocStack(2, stack);
            vkSubpassDependency.get(0)
                .srcSubpass(VK11.VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK11.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                .dstStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(VK11.VK_ACCESS_MEMORY_READ_BIT)
                .dstAccessMask(VK11.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dependencyFlags(VK11.VK_DEPENDENCY_BY_REGION_BIT);

            vkSubpassDependency.get(1)
                .srcSubpass(0)
                .dstSubpass(VK11.VK_SUBPASS_EXTERNAL)
                .srcStageMask(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstStageMask(VK11.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                .srcAccessMask(VK11.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK11.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dstAccessMask(VK11.VK_ACCESS_MEMORY_READ_BIT)
                .dependencyFlags(VK11.VK_DEPENDENCY_BY_REGION_BIT);

            final VkRenderPassCreateInfo vkRenderPassCreateInfo = VkRenderPassCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(vkAttachmentDescription)
                .pSubpasses(vkSubpassDescription)
                .pDependencies(vkSubpassDependency);

            final LongBuffer pointer = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateRenderPass(this.vkContext.getLogicalDevice().getNative(), vkRenderPassCreateInfo, null, pointer),
                "Failed to create render pass");
            this.vkRenderPass = pointer.get(0);
        }
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public long getPointer()
    {
        return this.vkRenderPass;
    }
}
