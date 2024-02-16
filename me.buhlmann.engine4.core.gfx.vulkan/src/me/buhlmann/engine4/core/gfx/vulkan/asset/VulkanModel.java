package me.buhlmann.engine4.core.gfx.vulkan.asset;

import me.buhlmann.engine4.api.IDisposable;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.primitive.IMesh;
import me.buhlmann.engine4.api.gfx.primitive.IModel;
import me.buhlmann.engine4.core.gfx.vulkan.*;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.AbstractVulkanAsset;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanBuffer;

import java.util.List;

public class VulkanModel extends AbstractVulkanAsset implements IModel, IDisposable, IVulkanDisposable
{
    public record VulkanMesh(String key, VulkanBuffer vertices, VulkanBuffer indices, int numIndices) implements IMesh, IVulkanDisposable
    {
        @Override
        public void dispose()
        {
            this.vertices.dispose();
            this.indices.dispose();
        }
    }

    private final IMesh[] meshes;
    private final IMaterial[] materials;

    private List<IMesh.MeshData> data;

    public void setData(final List<IMesh.MeshData> data)
    {
        this.data = data;
    }

    public List<IMesh.MeshData> getData()
    {
        return this.data;
    }

    @Override
    public IMesh[] getMeshes()
    {
        return this.meshes;
    }

    @Override
    public IMaterial[] getMaterials()
    {
        return this.materials;
    }

    @Override
    public void setMaterial(final int index, final IMaterial material)
    {
        this.materials[index]= material;
    }

    @Override
    public void dispose()
    {

    }

    public VulkanModel(final int meshes)
    {
        this.meshes = new IMesh[meshes];
        this.materials = new IMaterial[meshes];
    }
}
