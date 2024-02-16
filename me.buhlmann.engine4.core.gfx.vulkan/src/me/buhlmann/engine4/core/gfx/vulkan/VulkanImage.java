package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

public class VulkanImage extends AbstractVulkanLogicalDeviceContainer implements IVulkanPointer, IVulkanDisposable
{
    public record Data(Vector2i size, int format, int levels, int samples, int layers, int usage)
    {
    }

    private final long vkImage;
    private final long vkMemory;

    private final VulkanImage.Data data;

    @Override
    public long getPointer()
    {
        return this.vkImage;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyImage(this.getLogicalDevice().getNative(), this.vkImage, null);
        VK11.vkFreeMemory(this.getLogicalDevice().getNative(), this.vkMemory, null);
    }

    public VulkanImage.Data getData()
    {
        return this.data;
    }

    public VulkanImage(final VulkanLogicalDevice device, final VulkanImage.Data data)
    {
        super(device);
        this.data = data;

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK11.VK_IMAGE_TYPE_2D)
                .format(data.format())
                .extent(it -> it
                    .width(data.size().x)
                    .height(data.size().y)
                    .depth(1)
                )
                .mipLevels(data.levels())
                .arrayLayers(data.layers())
                .samples(data.samples())
                .initialLayout(VK11.VK_IMAGE_LAYOUT_UNDEFINED)
                .sharingMode(VK11.VK_SHARING_MODE_EXCLUSIVE)
                .tiling(VK11.VK_IMAGE_TILING_OPTIMAL)
                .usage(data.usage());

            final LongBuffer pointer = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateImage(device.getNative(), imageCreateInfo, null, pointer), "Failed to create image");
            this.vkImage = pointer.get(0);

            final VkMemoryRequirements requirements = VkMemoryRequirements.callocStack(stack);
            VK11.vkGetImageMemoryRequirements(device.getNative(), this.vkImage, requirements);

            final VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(requirements.size())
                .memoryTypeIndex(VulkanUtils.getMemoryTypeFromProperties(device.getPhysicalDevice(), requirements.memoryTypeBits(), 0));

            VulkanUtils.check(VK11.vkAllocateMemory(device.getNative(), allocation, null, pointer), "Failed to allocate memory");
            this.vkMemory = pointer.get(0);

            VulkanUtils.check(VK11.vkBindImageMemory(device.getNative(), this.vkImage, this.vkMemory, 0), "Failed to bind image memory");
        }
    }
}
