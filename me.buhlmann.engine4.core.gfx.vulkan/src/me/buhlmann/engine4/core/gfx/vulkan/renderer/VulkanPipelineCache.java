package me.buhlmann.engine4.core.gfx.vulkan.renderer;

import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanPointer;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import java.nio.LongBuffer;

public class VulkanPipelineCache implements IVulkanPointer, IVulkanDisposable
{
    private final long id;
    private final VulkanDeviceContext context;

    @Override
    public long getPointer()
    {
        return this.id;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyPipelineCache(this.context.getLogicalDevice().getNative(), this.id, null);
    }

    public VulkanPipelineCache(VulkanDeviceContext context)
    {
        this.context = context;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreatePipelineCache(context.getLogicalDevice().getNative(), createInfo, null, lp), "Error creating pipeline cache");
            this.id = lp.get(0);
        }
    }
}
