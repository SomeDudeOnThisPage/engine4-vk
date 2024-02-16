package me.buhlmann.engine4.core.gfx.vulkan.material;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IDisposable;
import me.buhlmann.engine4.api.gfx.texture.ITexture;
import me.buhlmann.engine4.core.gfx.vulkan.*;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.AbstractVulkanAsset;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanBuffer;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import java.nio.ByteBuffer;

public class VulkanTexture2D extends VulkanTexture implements ITexture, IVulkanDisposable, IDisposable
{
    public record VulkanTextureCreationInfo(String path, Vector2i size, int format, ByteBuffer data) {}

    private final VulkanBuffer vkStagingBuffer;

    public VulkanTexture2D(final VulkanImage vkImage, final VulkanImageView vkImageView, VulkanTextureSampler vkTextureSampler)
    {
        this.vkImage = vkImage;
        this.vkImageView = vkImageView;
        this.vkTextureSampler = vkTextureSampler;
        this.vkStagingBuffer = null;
    }

    public VulkanTexture2D(final VulkanTextureCreationInfo info, final VulkanTextureSampler vkTextureSampler, final VulkanDeviceContext vkContext)
    {
        final VulkanImage.Data vkImageData = new VulkanImage.Data(
            info.size,
            info.format,
            1, // TODO
            1,
            1,
            VK11.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK11.VK_IMAGE_USAGE_SAMPLED_BIT
        );
        super.vkImage = new VulkanImage(vkContext.getLogicalDevice(), vkImageData);
        this.vkTextureSampler = vkTextureSampler;

        final VulkanImageView.Data vkImageViewData = new VulkanImageView.Data();
        vkImageViewData.aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT);
        vkImageViewData.format(info.format);
        super.vkImageView = new VulkanImageView(vkContext.getLogicalDevice(), super.vkImage.getPointer(), vkImageViewData);
        this.vkStagingBuffer = this.createStgBuffer(info.data, vkContext);
    }

    @Override
    public void dispose()
    {
        if (this.vkStagingBuffer != null)
        {
            this.vkStagingBuffer.dispose();
        }
        this.vkTextureSampler.dispose();

        super.dispose();
    }

    private boolean recordedTransition;
    public void recordTextureTransition(VulkanCommandBuffer vkCommandBuffer)
    {
        if (this.vkStagingBuffer != null && !recordedTransition)
        {
            Engine4.getLogger().trace("Recording transition for texture");
            recordedTransition = true;
            try (final MemoryStack stack = MemoryStack.stackPush())
            {
                recordImageTransition(stack, vkCommandBuffer, VK11.VK_IMAGE_LAYOUT_UNDEFINED, VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                recordCopyBuffer(stack, vkCommandBuffer, this.vkStagingBuffer);
                recordImageTransition(stack, vkCommandBuffer, VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }
        }
    }

    private void recordImageTransition(final MemoryStack stack, final VulkanCommandBuffer vkCommandBuffer, final int oldLayout, final int newLayout) {

        final VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack)
            .sType(VK11.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            .oldLayout(oldLayout)
            .newLayout(newLayout)
            .srcQueueFamilyIndex(VK11.VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK11.VK_QUEUE_FAMILY_IGNORED)
            .image(this.vkImage.getPointer())
            .subresourceRange(it -> it
                .aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1));

        int srcStage;
        int srcAccessMask;
        int dstAccessMask;
        int dstStage;

        if (oldLayout == VK11.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
        {
            srcStage = VK11.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            srcAccessMask = 0;
            dstStage = VK11.VK_PIPELINE_STAGE_TRANSFER_BIT;
            dstAccessMask = VK11.VK_ACCESS_TRANSFER_WRITE_BIT;
        }
        else if (oldLayout == VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
        {
            srcStage = VK11.VK_PIPELINE_STAGE_TRANSFER_BIT;
            srcAccessMask = VK11.VK_ACCESS_TRANSFER_WRITE_BIT;
            dstStage = VK11.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            dstAccessMask = VK11.VK_ACCESS_SHADER_READ_BIT;
        }
        else
        {
            throw new RuntimeException("Unsupported layout transition");
        }

        barrier.srcAccessMask(srcAccessMask);
        barrier.dstAccessMask(dstAccessMask);

        VK11.vkCmdPipelineBarrier(vkCommandBuffer.getNative(), srcStage, dstStage, 0, null, null, barrier);
    }

    private void recordCopyBuffer(final MemoryStack stack, final VulkanCommandBuffer vkCommandBuffer, final VulkanBuffer vkBuffer)
    {
        final Vector2i size = this.vkImage.getData().size();
        final VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack)
            .bufferOffset(0)
            .bufferRowLength(0)
            .bufferImageHeight(0)
            .imageSubresource(it ->
                it.aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1)
            )
            .imageOffset(it -> it.x(0).y(0).z(0))
            .imageExtent(it -> it.width(size.x()).height(size.y()).depth(1));

        VK11.vkCmdCopyBufferToImage(
            vkCommandBuffer.getNative(),
            vkBuffer.getPointer(),
            this.vkImage.getPointer(),
            VK11.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            region
        );
    }

    private VulkanBuffer createStgBuffer(ByteBuffer data, VulkanDeviceContext vkContext)
    {
        int size = data.remaining();
        final VulkanBuffer vkStagingBuffer = new VulkanBuffer(
            vkContext,
            size,
            VK11.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK11.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );

        final ByteBuffer buffer = MemoryUtil.memByteBuffer(vkStagingBuffer.map(), (int) vkStagingBuffer.getRequestedSize());
        buffer.put(data);
        data.flip();
        vkStagingBuffer.unmap();

        return vkStagingBuffer;
    }
}
