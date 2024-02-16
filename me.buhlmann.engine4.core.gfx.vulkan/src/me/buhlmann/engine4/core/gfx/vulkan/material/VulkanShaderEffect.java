package me.buhlmann.engine4.core.gfx.vulkan.material;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.material.IMaterialArchetype;
import me.buhlmann.engine4.api.gfx.material.IShaderProgram;
import me.buhlmann.engine4.api.renderer.DeferredRenderStage;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanDescriptorSet;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanVertexBufferStructure;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.*;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.IVulkanRenderPass;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanPipeline;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanPipelineCache;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanShaderProgram;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred.VulkanDeferredRenderer3D;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred.VulkanDeferredRendererConstants;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/*
 * Material -> ShaderEffect[] (for each stage) -> Descriptor Layout & Pipeline & Data
 */
public class VulkanShaderEffect implements IVulkanDisposable
{
    private static final Map<VulkanPipeline.CreationInfo, VulkanPipeline> vkPipelineCache = new HashMap<>();

    private VulkanPipeline vkPipeline; // Fuck it we don't reuse Pipelines for now.
    private VulkanShaderProgram vkShaderProgram; // Fuck it we don't reuse ShaderModules for now.
    private VulkanUniformBuffer vkMaterialUniformBuffer;
    private VulkanVertexBufferStructure vkVertexBufferStructure;

    @Deprecated
    public VulkanDescriptorSet[] getAdditionalDescriptorSets()
    {
        return new VulkanDescriptorSet[] {};
    }

    public VulkanPipeline getPipeline()
    {
        return this.vkPipeline;
    }

    public VulkanUniformBuffer getUniformBuffer()
    {
        return this.vkMaterialUniformBuffer;
    }

    @Override
    public void dispose()
    {
        this.vkMaterialUniformBuffer.dispose();
        // this.vkShaderProgram.dispose();
        // this.vkVertexBufferStructure.dispose();
        this.vkPipeline.dispose();
    }

    private static <T> T[] concatWithStream(T[] array1, T[] array2) {
        return Stream.concat(Arrays.stream(array1), Arrays.stream(array2))
            .toArray(size -> (T[]) Array.newInstance(array1.getClass().getComponentType(), size));
    }

    public static VulkanPipeline createPipeline(final IVulkanRenderPass vkRenderPass, final VulkanShaderProgram vkShaderProgram, final VulkanUniformBuffer vkUniformBuffer, final List<IMaterial.Sampler> samplers, final VulkanDeviceContext vkContext)
    {
        if (Engine4.getRenderer() instanceof VulkanDeferredRenderer3D vkRenderer)
        {
            var vkVertexBufferStructure = new VulkanVertexBufferStructure(VulkanDeferredRendererConstants.VK_GEOMETRY_MODEL_PIPELINE_LAYOUT);

            final VulkanDescriptorSetLayout[] finalLayouts = concatWithStream(
                vkRenderer.getRenderPass(DeferredRenderStage.GEOMETRY).getDescriptorSetLayouts(),
                vkUniformBuffer.getDescriptorSetLayouts()
            );

            final VulkanPipeline.CreationInfo vkPipelineCreationInfo = new VulkanPipeline.CreationInfo(
                vkRenderPass.getPointer(),
                vkShaderProgram,
                3,
                vkVertexBufferStructure,
                128,
                true,
                // Get layouts from render pass AND this material
                finalLayouts
            );

            return new VulkanPipeline(vkContext, new VulkanPipelineCache(vkContext), vkPipelineCreationInfo);
        }
        return null;
    }

    public VulkanShaderEffect(final VulkanPipeline vkPipeline, final VulkanUniformBuffer vkMaterialUniformBuffer)
    {
        this.vkPipeline = vkPipeline;
        this.vkMaterialUniformBuffer = vkMaterialUniformBuffer;
    }

    public VulkanShaderEffect(final VulkanDeviceContext vkContext, final IVulkanRenderPass vkRenderPass, final VulkanShaderProgram vkShaderProgram, final List<IMaterial.Sampler> samplers)
    {
        if (Engine4.getRenderer() instanceof VulkanDeferredRenderer3D vkRenderer)
        {
            this.vkShaderProgram = vkShaderProgram;
            final VulkanDescriptor[] descriptors = new VulkanDescriptor[vkShaderProgram.getMaterialUniforms().length + samplers.size()];

            int i = 0;
            for (final VulkanDescriptor vkDescriptor : vkShaderProgram.getMaterialUniforms())
            {
                descriptors[i++] = vkDescriptor;
            }

            for (final IMaterial.Sampler sampler : samplers)
            {
                descriptors[i++] = new VulkanUniformSampler(sampler);
            }

            this.vkMaterialUniformBuffer = VulkanUniformBuffer.create(vkContext, 0, descriptors);
            this.vkVertexBufferStructure = new VulkanVertexBufferStructure(VulkanDeferredRendererConstants.VK_GEOMETRY_MODEL_PIPELINE_LAYOUT);

            final VulkanDescriptorSetLayout[] finalLayouts = concatWithStream(
                vkRenderer.getRenderPass(DeferredRenderStage.GEOMETRY).getDescriptorSetLayouts(),
                this.vkMaterialUniformBuffer.getDescriptorSetLayouts()
            );

            final VulkanPipeline.CreationInfo vkPipelineCreationInfo = new VulkanPipeline.CreationInfo(
                vkRenderPass.getPointer(),
                this.vkShaderProgram,
                1,
                this.vkVertexBufferStructure,
                128,
                true,
                // Get layouts from render pass AND this material
                finalLayouts
            );

            this.vkPipeline = new VulkanPipeline(vkContext, new VulkanPipelineCache(vkContext), vkPipelineCreationInfo);
        }

    }
}
