package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

public class VulkanImageView implements IVulkanPointer, IVulkanDisposable
{
    public static class Data
    {
        private int aspectMask;
        private int baseArrayLayer;
        private int format;
        private int layerCount;
        private int mipLevels;
        private int viewType;

        public Data()
        {
            this.baseArrayLayer = 0;
            this.layerCount = 1;
            this.mipLevels = 1;
            this.viewType = VK11.VK_IMAGE_VIEW_TYPE_2D;
        }

        public VulkanImageView.Data aspectMask(int aspectMask)
        {
            this.aspectMask = aspectMask;
            return this;
        }

        public VulkanImageView.Data baseArrayLayer(int baseArrayLayer)
        {
            this.baseArrayLayer = baseArrayLayer;
            return this;
        }

        public VulkanImageView.Data format(int format)
        {
            this.format = format;
            return this;
        }

        public VulkanImageView.Data layerCount(int layerCount)
        {
            this.layerCount = layerCount;
            return this;
        }

        public VulkanImageView.Data mipLevels(int mipLevels)
        {
            this.mipLevels = mipLevels;
            return this;
        }

        public VulkanImageView.Data viewType(int viewType)
        {
            this.viewType = viewType;
            return this;
        }
    }

    private final long id;
    private final VulkanLogicalDevice device;

    private final int mipLevels;
    private final int aspectMask;

    @Override
    public long getPointer()
    {
        return this.id;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyImageView(this.device.getNative(), this.id, null);
    }

    public VulkanImageView(VulkanLogicalDevice device, long image, VulkanImageView.Data imageViewData)
    {
        this.device = device;
        this.aspectMask = imageViewData.aspectMask;
        this.mipLevels = imageViewData.mipLevels;
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final LongBuffer lp = stack.mallocLong(1);
            VkImageViewCreateInfo viewCreateInfo = VkImageViewCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(imageViewData.viewType)
                .format(imageViewData.format)
                .subresourceRange(it -> it
                    .aspectMask(this.aspectMask)
                    .baseMipLevel(0)
                    .levelCount(this.mipLevels)
                    .baseArrayLayer(imageViewData.baseArrayLayer)
                    .layerCount(imageViewData.layerCount));

            VulkanUtils.check(VK11.vkCreateImageView(device.getNative(), viewCreateInfo, null, lp),
                "Failed to create image view");
            this.id = lp.get(0);
        }
    }
}
