package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import org.joml.Vector2i;
import org.lwjgl.vulkan.VK11;

public class VulkanAttachment extends AbstractVulkanLogicalDeviceContainer implements IVulkanDisposable
{
    private /* final */ VulkanDeviceContext vkContext;
    private final VulkanImage vkImage;
    private final VulkanImageView vkImageView;

    private final boolean isDepthAttachment;

    @Override
    public void dispose()
    {
        this.vkImage.dispose();
        this.vkImageView.dispose();
    }

    public VulkanImage getImage()
    {
        return this.vkImage;
    }

    public VulkanImageView getImageView()
    {
        return vkImageView;
    }

    public boolean isDepthAttachment()
    {
        return this.isDepthAttachment;
    }

    public VulkanAttachment(final Vector2i size, int format, int usage, final VulkanDeviceContext vkContext)
    {
        super(vkContext.getLogicalDevice());
        this.vkContext = vkContext;

        int vkImageAspect = 0;
        if ((usage & VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0)
        {
            vkImageAspect = VK11.VK_IMAGE_ASPECT_COLOR_BIT;
            this.isDepthAttachment = false;
        }
        else if ((usage & VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0)
        {
            vkImageAspect = VK11.VK_IMAGE_ASPECT_DEPTH_BIT;
            this.isDepthAttachment = true;
        }
        else
        {
            this.isDepthAttachment = false;
        }

        VulkanImage.Data vkImageData = new VulkanImage.Data(size, format, 1, 1, 1, usage | VK11.VK_IMAGE_USAGE_SAMPLED_BIT);
        this.vkImage = new VulkanImage(vkContext.getLogicalDevice(), vkImageData);

        VulkanImageView.Data vkImageViewData = new VulkanImageView.Data().format(this.vkImage.getData().format()).aspectMask(vkImageAspect);
        this.vkImageView = new VulkanImageView(vkContext.getLogicalDevice(), this.vkImage.getPointer(), vkImageViewData);
    }

    @Deprecated
    public VulkanAttachment(final VulkanLogicalDevice vkDevice, final Vector2i size, int format, int usage)
    {
        super(vkDevice);

        int aspect = 0;
        if ((usage & VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0)
        {
            aspect = VK11.VK_IMAGE_ASPECT_COLOR_BIT;
            this.isDepthAttachment = false;
        }
        else if ((usage & VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0)
        {
            aspect = VK11.VK_IMAGE_ASPECT_DEPTH_BIT;
            this.isDepthAttachment = true;
        }
        else
        {
            this.isDepthAttachment = false;
        }

        VulkanImage.Data vkImageData = new VulkanImage.Data(size, format, 1, 1, 1, usage | VK11.VK_IMAGE_USAGE_SAMPLED_BIT);
        this.vkImage = new VulkanImage(vkDevice, vkImageData);

        VulkanImageView.Data vkImageViewData = new VulkanImageView.Data().format(this.vkImage.getData().format()).aspectMask(aspect);
        this.vkImageView = new VulkanImageView(vkDevice, this.vkImage.getPointer(), vkImageViewData);
    }
}
