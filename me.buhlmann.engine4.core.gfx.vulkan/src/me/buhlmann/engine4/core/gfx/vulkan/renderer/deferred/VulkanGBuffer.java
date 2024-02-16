package me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred;

import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanAttachment;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import org.joml.Vector2i;
import org.lwjgl.vulkan.VK11;

import java.util.ArrayList;
import java.util.List;

public class VulkanGBuffer implements IVulkanDisposable
{
    private final List<VulkanAttachment> vkAttachments;
    private final VulkanAttachment vkDepthAttachment;
    private final Vector2i size;

    public VulkanGBuffer(final Vector2i size, final VulkanDeviceContext vkContext)
    {
        this.size = size;
        this.vkAttachments = new ArrayList<>();

        this.vkAttachments.add(new VulkanAttachment(this.size, VK11.VK_FORMAT_R32G32B32A32_SFLOAT, VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, vkContext));
        this.vkAttachments.add(new VulkanAttachment(this.size, VK11.VK_FORMAT_R32G32B32A32_SFLOAT, VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, vkContext));
        this.vkAttachments.add(new VulkanAttachment(this.size, VK11.VK_FORMAT_R32G32B32A32_SFLOAT, VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, vkContext));

        this.vkDepthAttachment = new VulkanAttachment(this.size, VK11.VK_FORMAT_D32_SFLOAT, VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, vkContext);
        this.vkAttachments.add(this.vkDepthAttachment);
    }

    public List<VulkanAttachment> getAttachments()
    {
        return this.vkAttachments;
    }

    public VulkanAttachment getDepthAttachment()
    {
        return this.vkDepthAttachment;
    }

    @Override
    public void dispose()
    {
        this.vkAttachments.forEach(VulkanAttachment::dispose);
    }
}
