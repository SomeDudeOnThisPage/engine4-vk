package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanPointer;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTexture;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTexture2D;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.Arrays;

public class VulkanDescriptorSet implements IVulkanPointer, IVulkanDisposable
{
    public static class DescriptorBinding
    {
        public int vkType;
        public int vkStage;
        public int binding;
        public long size;
        public VulkanTexture vkTexture;

        public long getSize()
        {
            return this.size;
        }

        public DescriptorBinding(int vkType, int vkStage, int binding, VulkanTexture vkTexture)
        {
            this.vkType = vkType;
            this.vkStage = vkStage;
            this.binding = binding;
            this.size = 0;
            this.vkTexture = vkTexture;
        }

        public DescriptorBinding(int vkType, int vkStage, int binding, long size)
        {
            this.vkType = vkType;
            this.vkStage = vkStage;
            this.binding = binding;
            this.size = size;
        }
    }

    private final long vkDescriptorSet;
    private final int location;
    private final VulkanDescriptorSetLayout vkDescriptorSetLayout;
    private final VulkanDeviceContext vkContext;
    private final VulkanDescriptorPool vkDescriptorPool;

    public VulkanDescriptorSet(final int location,
                    final VulkanDescriptorPool vkDescriptorPool,
                    final VulkanDescriptorSet.DescriptorBinding[] bindings,
                    final VulkanBuffer vkBuffer,
                    final VulkanDeviceContext vkContext
    )
    {
        this.location = location;
        this.vkContext = vkContext;
        this.vkDescriptorSetLayout = new VulkanDescriptorSetLayout(this.location, vkContext, bindings);
        this.vkDescriptorPool = vkDescriptorPool;

        final long size = Arrays.stream(bindings).mapToLong(VulkanDescriptorSet.DescriptorBinding::getSize).sum();
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkDescriptorSetAllocateInfo vkDescriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(vkDescriptorPool.getPointer())
                .pSetLayouts(stack.mallocLong(1).put(0, this.vkDescriptorSetLayout.getPointer()));

            final LongBuffer pointer = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkAllocateDescriptorSets(vkContext.getLogicalDevice().getNative(), vkDescriptorSetAllocateInfo, pointer), "Failed to create descriptor set");
            this.vkDescriptorSet = pointer.get(0);

            for (final VulkanDescriptorSet.DescriptorBinding binding : bindings)
            {
                if (binding.vkType == VK11.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                {
                    final VkDescriptorImageInfo.Buffer vkDescriptorImageInfo = VkDescriptorImageInfo.callocStack(1, stack)
                        .imageLayout(VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                        .imageView(binding.vkTexture.getImageView().getPointer())
                        .sampler(binding.vkTexture.getSampler().getPointer());

                    final VkWriteDescriptorSet.Buffer vkWriteDescriptorSet = VkWriteDescriptorSet.callocStack(1, stack);
                    vkWriteDescriptorSet.get(0)
                        .sType(VK11.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(this.vkDescriptorSet)
                        .dstBinding(binding.binding)
                        .descriptorType(binding.vkType)
                        .descriptorCount(1)
                        .pImageInfo(vkDescriptorImageInfo);

                    VK11.vkUpdateDescriptorSets(vkContext.getLogicalDevice().getNative(), vkWriteDescriptorSet, null);
                }
                else if (binding.vkType == VK11.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                {
                    final VkDescriptorBufferInfo.Buffer vkDescriptorBufferInfo = VkDescriptorBufferInfo.callocStack(1, stack)
                        .buffer(vkBuffer.getPointer())
                        .offset(0)
                        .range(size);

                    final VkWriteDescriptorSet.Buffer vkWriteDescriptorSet = VkWriteDescriptorSet.callocStack(1, stack);
                    vkWriteDescriptorSet.get(0)
                        .sType(VK11.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(this.vkDescriptorSet)
                        .dstBinding(binding.binding)
                        .descriptorType(binding.vkType)
                        .descriptorCount(1)
                        .pBufferInfo(vkDescriptorBufferInfo);

                    VK11.vkUpdateDescriptorSets(vkContext.getLogicalDevice().getNative(), vkWriteDescriptorSet, null);
                }
            }

        }
    }

    public VulkanDescriptorSetLayout getDescriptorSetLayout()
    {
        return this.vkDescriptorSetLayout;
    }

    @Override
    public void dispose()
    {
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            VulkanUtils.check(VK11.vkFreeDescriptorSets(this.vkContext.getLogicalDevice().getNative(), this.vkDescriptorPool.getPointer(), stack.mallocLong(1).put(0, this.vkDescriptorSet)), "Failed to free descriptor set");
            Engine4.getLogger().trace("[VULKAN] freed descriptor set 0x%x", this.getPointer());
        }

    }

    @Override
    public long getPointer()
    {
        return this.vkDescriptorSet;
    }
}
