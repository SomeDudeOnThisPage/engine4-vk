package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanPointer;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import java.nio.LongBuffer;
import java.util.List;

public class VulkanDescriptorPool implements IVulkanPointer, IVulkanDisposable
{
    public record PooledDescriptorInfo(int count, int descriptorType)
    {
    }

    private final VulkanDeviceContext context;
    private final long vkDescriptorPool;

    @Override
    public long getPointer()
    {
        return this.vkDescriptorPool;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyDescriptorPool(this.context.getLogicalDevice().getNative(), this.vkDescriptorPool, null);
    }

    public void free(long vkDescriptorSet)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            VulkanUtils.check(VK11.vkFreeDescriptorSets(
                    this.context.getLogicalDevice().getNative(),
                    this.vkDescriptorPool,
                    stack.mallocLong(1).put(0, vkDescriptorSet)
                ), "Failed to free descriptor set");
        }
    }

    public VulkanDescriptorPool(VulkanDeviceContext context, List<PooledDescriptorInfo> vkPooledDescriptorInfos)
    {
        this.context = context;

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            int maxSets = 0;

            final VkDescriptorPoolSize.Buffer vkDescriptorPoolSize = VkDescriptorPoolSize.callocStack(vkPooledDescriptorInfos.size(), stack);
            for (int i = 0; i < vkPooledDescriptorInfos.size(); i++)
            {
                maxSets += vkPooledDescriptorInfos.get(i).count();
                vkDescriptorPoolSize.get(i)
                    .type(vkPooledDescriptorInfos.get(i).descriptorType())
                    .descriptorCount(vkPooledDescriptorInfos.get(i).count());
            }

            final VkDescriptorPoolCreateInfo vkDescriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .flags(VK11.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                .pPoolSizes(vkDescriptorPoolSize)
                .maxSets(maxSets);

            final LongBuffer pDescriptorPool = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateDescriptorPool(context.getLogicalDevice().getNative(), vkDescriptorPoolCreateInfo, null, pDescriptorPool),
                "Failed to create descriptor pool");
            this.vkDescriptorPool = pDescriptorPool.get(0);
        }
    }
}
