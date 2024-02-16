package me.buhlmann.engine4.core.gfx.vulkan.material;

import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanPointer;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

public class VulkanTextureSampler implements IVulkanPointer, IVulkanDisposable
{
    private static final int MAX_ANISOTROPY = 16;

    private long vkTextureSampler;
    private VulkanDeviceContext vkContext;

    public VulkanTextureSampler(final VulkanDeviceContext vkContext, final int mip, final boolean anisotropy)
    {
        this.vkContext = vkContext;
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkSamplerCreateInfo vkSamplerCreateInfo = VkSamplerCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(VK11.VK_FILTER_LINEAR)
                .minFilter(VK11.VK_FILTER_LINEAR)
                .addressModeU(VK11.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeV(VK11.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeW(VK11.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .borderColor(VK11.VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                .unnormalizedCoordinates(false)
                .compareEnable(false)
                .compareOp(VK11.VK_COMPARE_OP_ALWAYS)
                .mipmapMode(VK11.VK_SAMPLER_MIPMAP_MODE_LINEAR)
                .minLod(0.0f)
                .maxLod(mip)
                .mipLodBias(0.0f);

            if (anisotropy /* && vkContext.getLogicalDevice().getCapability(SAMPLER_ANISOTROPY) */ && vkContext.getLogicalDevice().getSupportAnisotropy()) {
                vkSamplerCreateInfo
                    .anisotropyEnable(true)
                    .maxAnisotropy(VulkanTextureSampler.MAX_ANISOTROPY);
            }

            final LongBuffer pointer = stack.mallocLong(1);
            VulkanUtils.check(VK11.vkCreateSampler(vkContext.getLogicalDevice().getNative(), vkSamplerCreateInfo, null, pointer), "Failed to create sampler");
            this.vkTextureSampler = pointer.get(0);
        }
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroySampler(this.vkContext.getLogicalDevice().getNative(), this.vkTextureSampler, null);
    }

    @Override
    public long getPointer()
    {
        return this.vkTextureSampler;
    }
}
