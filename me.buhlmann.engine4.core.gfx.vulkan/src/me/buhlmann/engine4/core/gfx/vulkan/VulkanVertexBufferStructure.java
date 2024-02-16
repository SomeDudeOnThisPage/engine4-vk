package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.gfx.primitive.VertexBufferLayout;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public class VulkanVertexBufferStructure extends VulkanVertexInputStateInfo
{
    public static final class Empty extends VulkanVertexBufferStructure
    {
        public Empty()
        {
            super.vkVertexInputStateInfo = VkPipelineVertexInputStateCreateInfo.calloc();
            super.vkVertexInputStateInfo.sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(null)
                .pVertexAttributeDescriptions(null);
        }
    }

    protected VkVertexInputAttributeDescription.Buffer vkVertexInputAttributeDescriptions;
    protected VkVertexInputBindingDescription.Buffer vkVertexInputBindingDescriptions;

    @Override
    public void dispose()
    {
        super.dispose();
        if (this.vkVertexInputBindingDescriptions != null)
        {
            this.vkVertexInputBindingDescriptions.free();
        }

        if (this.vkVertexInputAttributeDescriptions != null)
        {
            this.vkVertexInputAttributeDescriptions.free();
        }
    }

    protected VulkanVertexBufferStructure()
    {
    }

    public VulkanVertexBufferStructure(final VertexBufferLayout layout)
    {
        this.vkVertexInputStateInfo = VkPipelineVertexInputStateCreateInfo.calloc();
        this.vkVertexInputAttributeDescriptions = VkVertexInputAttributeDescription.calloc(layout.getElements().size());
        this.vkVertexInputBindingDescriptions = VkVertexInputBindingDescription.calloc(1);

        int i = 0;
        for (VertexBufferLayout.BufferElement element : layout.getElements())
        {
            this.vkVertexInputAttributeDescriptions.get(i)
                .binding(0)
                .location(element.getLocation())//.location(i)
                .format(VK11.VK_FORMAT_R32G32B32_SFLOAT)
                .offset(element.getOffset());
            Engine4.getLogger().trace("type = " + element.getType() + ", offset = " + element.getOffset() + ", location = " + element.getLocation());

            i++;
        }

        this.vkVertexInputBindingDescriptions.get(0)
            .binding(0)
            .stride(layout.getStride())
            .inputRate(VK11.VK_VERTEX_INPUT_RATE_VERTEX);

        this.getNative()
            .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            .pVertexBindingDescriptions(this.vkVertexInputBindingDescriptions)
            .pVertexAttributeDescriptions(this.vkVertexInputAttributeDescriptions);
    }
}
