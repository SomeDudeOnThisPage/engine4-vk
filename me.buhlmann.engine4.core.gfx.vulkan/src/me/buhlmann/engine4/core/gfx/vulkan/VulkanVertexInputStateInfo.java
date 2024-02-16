package me.buhlmann.engine4.core.gfx.vulkan;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

public abstract class VulkanVertexInputStateInfo implements IVulkanNative<VkPipelineVertexInputStateCreateInfo>, IVulkanDisposable
{
    protected VkPipelineVertexInputStateCreateInfo vkVertexInputStateInfo;

    @Override
    public VkPipelineVertexInputStateCreateInfo getNative()
    {
        return this.vkVertexInputStateInfo;
    }

    @Override
    public void dispose()
    {
        this.vkVertexInputStateInfo.free();
    }
}
