/*package me.buhlmann.engine4.core.gfx.vulkan.factory;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetFactory;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.gfx.ShaderAttribute;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.material.IMaterialFactory;
import me.buhlmann.engine4.api.gfx.primitive.IMesh;
import me.buhlmann.engine4.api.gfx.primitive.IModel;
import me.buhlmann.engine4.api.gfx.primitive.IModelFactory;
import me.buhlmann.engine4.api.gfx.primitive.VertexBufferLayout;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanCommandBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanFence;
import me.buhlmann.engine4.core.gfx.vulkan.asset.VulkanModel;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.IVulkanRenderer;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred.VulkanDeferredRendererConstants;
import me.buhlmann.engine4.factory.AssetFactory;
import me.buhlmann.engine4.utils.StringUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkBufferCopy;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanModelFactory implements IModelFactory
{
    private record VulkanModelDataTransferBuffers(VulkanBuffer vkSourceBuffer, VulkanBuffer vkDestinationBuffer) {}

    @Override
    public IModel load(Object element)
    {
        if (element instanceof IModelFactory.MetaData meta && Engine4.getRenderer() instanceof IVulkanRenderer vkRenderer)
        {
            Engine4.getLogger().trace("[ASSET MANAGER] VulkanModelFactory#load(" + meta + ")");
            final VulkanModel model = new VulkanModel();

            final List<IMesh.MeshData> data = new ArrayList<>();
            final AssimpMeshLoader.Result result = AssimpMeshLoader.load(meta.mesh, meta.material);
            if (result != null)
            {
                final IAssetFactory<IMaterial, ?> factory = Engine4.getAssetManager().getAssetFactory(IMaterial.class);

                data.addAll(result.data().stream().map(AssimpMeshLoader.MeshResult::data).toList());

                if (meta.material != null)
                {
                    final List<IAssetReference<IMaterial>> indexedMaterials = new ArrayList<>();
                    result.mapping().forEach((mappings) ->
                    {
                        mappings.mapping().forEach((mapping) ->
                        {
                            if (mapping.mapping().equals("NAME"))
                            {
                                meta.material.id = mapping.data();
                                return;
                            }

                            for (final IMaterialFactory.MetaData.Uniform uniform : meta.material.uniforms)
                            {
                                if (uniform.mapping.equals(mapping.mapping()))
                                {
                                    uniform.uniform = mapping.data();
                                }
                            }

                            for (final IMaterialFactory.MetaData.Sampler2D sampler : meta.material.samplers)
                            {
                                if (sampler.mapping.equals(mapping.mapping()))
                                {
                                    sampler.source = "editor/materials/" + (StringUtils.isNullOrBlank(mapping.data())
                                        ? "dev_white/altdev_generic01.png"
                                        : mapping.data());
                                    Engine4.getLogger().warning(sampler.source);
                                }
                            }
                        });

                        if (meta.material.id.contains("DefaultMaterial"))
                        {
                            indexedMaterials.add(null);
                            return;
                        }
                        final IMaterial material = factory.load(meta.material);
                        final IAssetReference<IMaterial> reference = AssetFactory.wrap(meta.id, IMaterial.class, material);
                        Engine4.getAssetManager().put(IMaterial.class, meta.id, reference);
                        indexedMaterials.add(reference);
                    });

                    for (AssimpMeshLoader.MeshResult mesh : result.data())
                    {
                        model.setMaterial(mesh.material(), indexedMaterials.get(mesh.material()));
                    }
                }
            }
            model.setData(data);

            this.transfer(meta.id, model, vkRenderer);

            return model;

        }
        return null;
    }

    /*private void transfer(final String key, final VulkanModel model, final IVulkanRenderer renderer)
    {
        final VulkanRenderContext vkContext = renderer.getRenderContext();
        synchronized (vkContext)
        {
            final VulkanCommandBuffer commands = new VulkanCommandBuffer(vkContext, true, true);
            final List<VulkanBuffer> staging = new ArrayList<>();

            // Record commands.
            commands.begin(null);
            for (IMesh.MeshData data : model.getData())
            {
                final VulkanModelFactory.VulkanModelDataTransferBuffers vertices = VulkanModelFactory.createVerticesBuffers(vkContext, data);
                final VulkanModelFactory.VulkanModelDataTransferBuffers indices = VulkanModelFactory.createIndicesBuffers(vkContext, data);

                staging.add(vertices.vkSourceBuffer());
                staging.add(indices.vkSourceBuffer());

                VulkanModelFactory.recordTransferCommand(commands, vertices);
                VulkanModelFactory.recordTransferCommand(commands, indices);

                final VulkanModel.VulkanMesh mesh = new VulkanModel.VulkanMesh(
                    key + "#" + data.name(),
                    vertices.vkDestinationBuffer(),
                    indices.vkDestinationBuffer(),
                    data.indices().length
                );

                final IMesh meshReference = AssetFactory.createAssetReference(mesh.key(), IMesh.class);
                meshReference.setLoadingStage(IAssetReference.LoadingStage.LOADED);
                meshReference.set(mesh);
                model.getMeshes().add(meshReference);
                Engine4.getAssetManager().put(IMesh.class, mesh.key(), meshReference);
            }
            commands.end();

            final VulkanFence fence = new VulkanFence(vkContext.getLogicalDevice(), true);
            fence.reset();

            try (final MemoryStack stack = MemoryStack.stackPush())
            {
                vkContext.getGraphicsQueue().submit(stack.pointers(commands.getNative()), null, null, null, fence);
            }

            fence.hold();
            fence.dispose();
            commands.dispose();
            staging.forEach(VulkanBuffer::dispose);

            Engine4.getLogger().trace("[ASSET MANAGER] VulkanModelFactory#initialize(" + key + ")");
        }
    }

    @Override
    public void initialize(IAssetReference<> reference)
    {
        // No main thread initialization needed.
    }

    private static synchronized VulkanModelFactory.VulkanModelDataTransferBuffers createVerticesBuffers(VulkanDeviceContext vkContext, IMesh.MeshData data)
    {
        final float[] positions = data.vertices();
        final float[] normals = data.normals();
        float[] uv = data.uv();

        int size = 0;
        size += positions.length;
        size += normals == null ? 0 : normals.length;
        size += uv == null ? 0 : uv.length;
        size *= 4;

        final VulkanBuffer vkSourceBuffer = new VulkanBuffer(vkContext, size,
            VK11.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK11.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        final VulkanBuffer vkDestinationBuffer = new VulkanBuffer(vkContext, size,
            VK11.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK11.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK11.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        final FloatBuffer buffer = MemoryUtil.memFloatBuffer(vkSourceBuffer.map(), (int) vkSourceBuffer.getRequestedSize());
        final int components = ShaderAttribute.V_POSITION.getDataType().getComponentAmount();

        for (int vertex = 0; vertex < positions.length; vertex += components)
        {
            for (final VertexBufferLayout.BufferElement element : VulkanDeferredRendererConstants.VK_GEOMETRY_MODEL_PIPELINE_LAYOUT.getElements())
            {
                switch (element.getAttribute())
                {
                    case V_POSITION -> {
                        buffer.put(positions[vertex]);
                        buffer.put(positions[vertex + 1]);
                        buffer.put(positions[vertex + 2]);
                    }
                    case V_NORMAL -> {
                        if (normals == null)
                        {
                            break;
                        }
                        final int position = vertex * ShaderAttribute.V_NORMAL.getDataType().getComponentAmount() / components;
                        buffer.put(normals[position]);
                        buffer.put(normals[position + 1]);
                        buffer.put(normals[position + 2]);
                    }
                    case V_TEXTURE -> {
                        if (uv == null)
                        {
                            break;
                        }
                        final int position = vertex * ShaderAttribute.V_TEXTURE.getDataType().getComponentAmount() / components;
                        buffer.put(uv[position]);
                        buffer.put(uv[position + 1]);
                    }
                }
            }
        }

        vkSourceBuffer.unmap();

        return new VulkanModelFactory.VulkanModelDataTransferBuffers(vkSourceBuffer, vkDestinationBuffer);
    }

    private static synchronized VulkanModelFactory.VulkanModelDataTransferBuffers createIndicesBuffers(VulkanDeviceContext vkContext, IMesh.MeshData meshData)
    {
        final int size = meshData.indices().length * Integer.BYTES;

        final VulkanBuffer vkSourceBuffer = new VulkanBuffer(vkContext, size,
            VK11.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK11.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        final VulkanBuffer vkDestinationBuffer = new VulkanBuffer(vkContext, size,
            VK11.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK11.VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK11.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        final IntBuffer buffer = MemoryUtil.memIntBuffer(vkSourceBuffer.map(), (int) vkSourceBuffer.getRequestedSize());
        buffer.put(meshData.indices());
        vkSourceBuffer.unmap();

        return new VulkanModelFactory.VulkanModelDataTransferBuffers(vkSourceBuffer, vkDestinationBuffer);
    }

    private static synchronized void recordTransferCommand(VulkanCommandBuffer vkCommandBuffer, VulkanModelFactory.VulkanModelDataTransferBuffers vkTransferBuffers)
    {
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack)
                .srcOffset(0)
                .dstOffset(0)
                .size(vkTransferBuffers.vkSourceBuffer().getRequestedSize());

            VK11.vkCmdCopyBuffer(
                vkCommandBuffer.getNative(),
                vkTransferBuffers.vkSourceBuffer().getPointer(),
                vkTransferBuffers.vkDestinationBuffer().getPointer(),
                copyRegion
            );
        }
    }

}
*/