package me.buhlmann.engine4.core.gfx.vulkan.asset;

import me.buhlmann.engine4.api.IDisposable;
import me.buhlmann.engine4.api.gfx.material.IDeferredMaterial;
import me.buhlmann.engine4.api.renderer.DeferredRenderStage;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanCommandBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanUniformBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanShaderEffect;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.AbstractVulkanAsset;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.IVulkanRenderPass;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanPipeline;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred.VulkanGeometryRenderPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;

import java.nio.LongBuffer;

public class VulkanDeferredMaterial extends AbstractVulkanAsset implements IDeferredMaterial, IDisposable, IVulkanDisposable
{
    private final VulkanShaderEffect[] vkShaderEffects;

    public void bindPipeline(final DeferredRenderStage stage, final IVulkanRenderPass vkRenderPass, final VulkanCommandBuffer vkCommandBuffer, final MemoryStack stack)
    {
        if (DeferredRenderStage.GEOMETRY == stage && vkRenderPass instanceof VulkanGeometryRenderPass)
        {
            final VulkanPipeline vkPipeline = this.vkShaderEffects[0].getPipeline();
            VK11.vkCmdBindPipeline(vkCommandBuffer.getNative(), VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, vkPipeline.getPointer());
        }
    }

    public void bind(final DeferredRenderStage stage, final IVulkanRenderPass vkRenderPass, final VulkanCommandBuffer vkCommandBuffer, final MemoryStack stack)
    {
        if (DeferredRenderStage.GEOMETRY == stage && vkRenderPass instanceof VulkanGeometryRenderPass)
        {
            final VulkanUniformBuffer vkBuffer = this.vkShaderEffects[0].getUniformBuffer();
            final VulkanPipeline vkPipeline = this.vkShaderEffects[0].getPipeline();

            final LongBuffer vkDescriptorSets = stack.mallocLong(vkRenderPass.getDescriptorSetPointers().length + 1);
            for (int i = 0; i < vkRenderPass.getDescriptorSetPointers().length; i++)
            {
                vkDescriptorSets.put(i, vkRenderPass.getDescriptorSetPointers()[i]);
            }
            vkDescriptorSets.put(vkRenderPass.getDescriptorSetPointers().length, vkBuffer.getDescriptorSetPointer());

            VK11.vkCmdBindDescriptorSets(
                vkCommandBuffer.getNative(),
                VK11.VK_PIPELINE_BIND_POINT_GRAPHICS,
                vkPipeline.getPipelineLayout(),
                0,
                vkDescriptorSets,
                null
            );
        }
    }

    @Override
    public void dispose()
    {
        for (final VulkanShaderEffect vkShaderEffect : this.vkShaderEffects)
        {
            if (vkShaderEffect != null)
            {
                vkShaderEffect.dispose();
            }
        }
    }

    public VulkanShaderEffect getShaderEffect(final int stage)
    {
        return this.vkShaderEffects[stage];
    }

    public VulkanDeferredMaterial(final VulkanShaderEffect vkGeometryShaderEffect, final VulkanShaderEffect vkLightingShaderEffect)
    {
        this.vkShaderEffects = new VulkanShaderEffect[]
        {
            vkGeometryShaderEffect,
            vkLightingShaderEffect
        };

        // Validate uniforms matching between material and expected shader data (uniform buffer).
        /*this.uniforms = uniforms.stream().filter((uniform) -> {
            try
            {
                vkGeometryShaderEffect.getUniformBuffer().getUniform(uniform.location());
                return true;
            }
            catch (UnsupportedOperationException e)
            {
                Engine4.getLogger().warning("discarding uniform '" + uniform.location()
                    + "' of type '" + uniform.data()
                    + "' in module '"
                    + vkGeometryShaderEffect + "' - uniform missing in shader module definition"
                );
                return false;
            }
        }).toList();*/
    }
}
