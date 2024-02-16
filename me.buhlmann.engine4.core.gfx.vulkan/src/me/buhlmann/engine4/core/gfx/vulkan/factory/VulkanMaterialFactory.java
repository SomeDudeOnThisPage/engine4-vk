package me.buhlmann.engine4.core.gfx.vulkan.factory;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.material.IMaterialFactory;
import me.buhlmann.engine4.api.gfx.material.IShaderProgram;
import me.buhlmann.engine4.api.gfx.texture.ITexture;
import me.buhlmann.engine4.api.gfx.texture.STBLoader;
import me.buhlmann.engine4.api.renderer.DeferredRenderStage;
import me.buhlmann.engine4.asset.AssetReference;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanCommandBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanFence;
import me.buhlmann.engine4.core.gfx.vulkan.asset.VulkanDeferredMaterial;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanDescriptor;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanUniformBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanUniformSampler;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanShaderEffect;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTexture2D;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTextureSampler;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanPipeline;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanShaderProgram;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred.VulkanDeferredRenderer3D;
import me.buhlmann.engine4.factory.AssetFactory;
import me.buhlmann.engine4.utils.StringUtils;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VulkanMaterialFactory implements IMaterialFactory
{
    private record Pipelines(VulkanPipeline vkGeometryPipeline, VulkanPipeline vkLightingPipeline) {}
    private final Map<String, Pipelines> vkPipelineCache;

    public VulkanMaterialFactory()
    {
        this.vkPipelineCache = new HashMap<>();
    }

    private VulkanShaderProgram createGeometryShader(final VulkanDeviceContext vkContext, final IMaterialFactory.MetaData.ShaderProgram data)
    {
        if (data.isCombinationShader())
        {
            return new VulkanShaderProgram(vkContext, new VulkanShaderProgram.ShaderModuleData[]
            {
                new VulkanShaderProgram.ShaderModuleData(VK11.VK_SHADER_STAGE_VERTEX_BIT, data.combined),
                // TODO: Geometry...
                // TODO: Tesselation...
                new VulkanShaderProgram.ShaderModuleData(VK11.VK_SHADER_STAGE_FRAGMENT_BIT, data.combined),
            });
        }
        else
        {
            return new VulkanShaderProgram(vkContext, new VulkanShaderProgram.ShaderModuleData[]
            {
                new VulkanShaderProgram.ShaderModuleData(VK11.VK_SHADER_STAGE_VERTEX_BIT, data.vs),
                // TODO: Geometry...
                // TODO: Tesselation...
                new VulkanShaderProgram.ShaderModuleData(VK11.VK_SHADER_STAGE_FRAGMENT_BIT, data.fs),
            });
        }
    }

    private float[] parseVectorFloatData(final String data)
    {
        final String[] split = data.split(" ");
        float[] floats = new float[split.length];
        for (int i = 0 ; i < floats.length; i++)
        {
            floats[i] = Float.parseFloat(split[i]);
        }
        return floats;
    }

    private IMaterial.Uniform<?> parseUniform(IMaterialFactory.MetaData.Uniform uniform)
    {
        final String type = uniform.type;
        switch (type)
        {
            case "vector3f":
                return new IMaterial.Uniform<>(uniform.name, new Vector3f(this.parseVectorFloatData(uniform.uniform)));
        }

        throw new IllegalStateException();
    }

    private IMaterial.Sampler parseSampler(final IMaterialFactory.MetaData.Sampler2D uniform, final VulkanRenderContext vkContext)
    {
        final String source = uniform.source;
        final IAssetReference<ITexture> existing = Engine4.getAssetManager().request(ITexture.class, "texture." + source);
        if (existing != null)
        {
            return new IMaterial.Sampler(uniform.binding, existing);
        }

        final STBLoader loader = new STBLoader();
        loader.load(source, 4);
        final int width = loader.width();
        final int height = loader.height();
        final VulkanTexture2D.VulkanTextureCreationInfo info = new VulkanTexture2D.VulkanTextureCreationInfo(
            source,
            new Vector2i(width, height),
            VK11.VK_FORMAT_R8G8B8A8_SRGB,
            loader.data()
        );
        final VulkanCommandBuffer vkCommandBuffer = new VulkanCommandBuffer(vkContext, true, true);
        vkCommandBuffer.begin(null);
        final VulkanTexture2D texture = new VulkanTexture2D(info, new VulkanTextureSampler(vkContext, 1, true), vkContext);
        texture.recordTextureTransition(vkCommandBuffer);
        vkCommandBuffer.end();

        final VulkanFence fence = new VulkanFence(vkContext.getLogicalDevice(), true);
        fence.reset();

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            vkContext.getGraphicsQueue().submit(stack.pointers(vkCommandBuffer.getNative()), null, null, null, fence);
        }

        fence.hold();
        fence.dispose();
        vkCommandBuffer.dispose();

        final IAssetReference<ITexture> reference = AssetFactory.createAssetReference("texture." + source, ITexture.class);
        reference.set(texture);
        Engine4.getAssetManager().put(ITexture.class, "texture." + source, reference);
        return new IMaterial.Sampler(uniform.binding, reference);
    }

    @Override
    public IMaterial load(final Object meta)
    {
        if (meta instanceof IMaterialFactory.MetaData data && Engine4.getRenderer() instanceof final VulkanDeferredRenderer3D renderer)
        {
            final String pipeline = data.pipeline;
            final List<IMaterial.Uniform<?>> uniforms = new ArrayList<>();
            final List<IMaterial.Sampler> samplers = new ArrayList<>();

            for (final IMaterialFactory.MetaData.Uniform uniform : data.uniforms)
            {
                uniforms.add(parseUniform(uniform));
                Engine4.getLogger().trace("parsed uniform in location '" + uniform.name + "' with data '" + uniform.uniform + "' and type '" + uniform.type + "'");
            }

            if (data.samplers != null)
            {
                for (final IMaterialFactory.MetaData.Sampler2D sampler : data.samplers)
                {
                    if (!StringUtils.isNullOrBlank(sampler.source))
                    {
                        samplers.add(parseSampler(sampler, renderer.getContext()));
                        Engine4.getLogger().trace("parsed sampler in location '" + sampler.name + "' with data '" + sampler.source + "' and binding '" + sampler.binding + "'");
                    }
                }
            }

            final VulkanShaderProgram vkGeometryShader = this.createGeometryShader(renderer.getRenderContext(), data.geometry);
            final VulkanDescriptor[] vkDescriptors = new VulkanDescriptor[vkGeometryShader.getMaterialUniforms().length + samplers.size()];

            int i = 0;
            for (final VulkanDescriptor vkDescriptor : vkGeometryShader.getMaterialUniforms())
            {
                vkDescriptors[i++] = vkDescriptor;
            }

            for (final IMaterial.Sampler sampler : samplers)
            {
                vkDescriptors[i++] = new VulkanUniformSampler(sampler);
            }

            final VulkanUniformBuffer vkUniformBuffer = VulkanUniformBuffer.create(renderer.getRenderContext(), 0, vkDescriptors);

            final VulkanShaderProgram vkGeometryShaderProgram = Engine4.getAssetManager().request(IShaderProgram.class, "shader." + data.geometry) == null
                ? this.createGeometryShader(renderer.getRenderContext(), data.geometry)
                : (VulkanShaderProgram) Engine4.getAssetManager().request(IShaderProgram.class, "shader." + data.geometry).get();

            final VulkanPipeline vkGeometryPipeline = this.vkPipelineCache.containsKey(pipeline)
                ? this.vkPipelineCache.get(pipeline).vkGeometryPipeline()
                : VulkanShaderEffect.createPipeline(
                    renderer.getRenderPass(DeferredRenderStage.GEOMETRY),
                    vkGeometryShaderProgram,
                    vkUniformBuffer,
                    samplers,
                    renderer.getRenderContext()
                );

            for (IMaterial.Uniform<?> uniform : uniforms)
            {
                vkUniformBuffer.set(uniform.location(), uniform.data());
            }
            this.vkPipelineCache.put(pipeline, new Pipelines(vkGeometryPipeline, null));

            if (!Engine4.getAssetManager().contains(IShaderProgram.class, "shader." + data.geometry))
            {
                Engine4.getAssetManager().put(IShaderProgram.class, "shader." + data.geometry, AssetFactory.wrap("shader." + data.geometry, IShaderProgram.class, vkGeometryShaderProgram));
            }
            return new VulkanDeferredMaterial(new VulkanShaderEffect(vkGeometryPipeline, vkUniformBuffer), null);
        }

        // TODO
        throw new RuntimeException("TODO error handling when we have a different renderer");
    }

    @Override
    public IAssetReference<IMaterial> createAssetReference(final String tag)
    {
        return new AssetReference<>(IMaterial.class, tag);
    }
}
