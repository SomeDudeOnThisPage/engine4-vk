package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanPointer;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;

public class VulkanDescriptorSetLayout implements IVulkanPointer, IVulkanDisposable
{
    private static final int DESCRIPTOR_COUNT_ARRAY_SIZE = 1;

    private final long vkDescriptorLayout;
    private final int location; // TODO: This is shit. Combine in like a "VulkanDescriptorSetLocation" record or something to pass to pipeline...
    private final VulkanDeviceContext context;

    public VulkanDescriptorSetLayout(final int location, final VulkanDeviceContext context, final VulkanDescriptorSet.DescriptorBinding... bindings)
    {
        this.location = location;
        this.context = context;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkDescriptorSetLayoutBinding.Buffer vkDescriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.callocStack(bindings.length, stack);
            for (int i = 0; i < bindings.length; i++)
            {
                vkDescriptorSetLayoutBindings.get(i)
                    .binding(bindings[i].binding)
                    .descriptorType(bindings[i].vkType)
                    .descriptorCount(VulkanDescriptorSetLayout.DESCRIPTOR_COUNT_ARRAY_SIZE)
                    .stageFlags(bindings[i].vkStage);
            }

            final VkDescriptorSetLayoutCreateInfo vkDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(vkDescriptorSetLayoutBindings);

            final LongBuffer pointer = stack.mallocLong(1);
            VulkanUtils.check(vkCreateDescriptorSetLayout(
                this.context.getLogicalDevice().getNative(),
                vkDescriptorSetLayoutCreateInfo,
                null,
                pointer
            ), "Failed to create descriptor set layout");

            this.vkDescriptorLayout = pointer.get(0);
            Engine4.getLogger().trace("created descriptor set layout");
        }
    }

    public int getLocation()
    {
        return this.location;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyDescriptorSetLayout(this.context.getLogicalDevice().getNative(), this.vkDescriptorLayout, null);
    }

    @Override
    public long getPointer()
    {
        return this.vkDescriptorLayout;
    }
}
