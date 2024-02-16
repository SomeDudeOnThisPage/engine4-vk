package me.buhlmann.engine4.core.gfx.vulkan.renderer;

import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.material.IMaterialArchetype;
import me.buhlmann.engine4.api.gfx.primitive.IMesh;
import me.buhlmann.engine4.api.utils.ICollectionMap;
import me.buhlmann.engine4.utils.SetMap;

import java.util.Map;
import java.util.UUID;

/**
 * This class sorts all materials and meshes to be used in a rendering loop.
 * 1. Level: Material Archetypes (containing pipelines and descriptor set layouts)
 * 2. Level: Material Instances (containing the actual material properties to be bound to the descriptor)
 * 3. Level: Meshes (belonging to the set material instance)
 * <br>
 * On the first and second level, there will be no duplicates, and archetypes and material instances will be stored
 * in sets.
 * On the third level, meshes can be added multiple times for different materials.
 */
public class VulkanMaterialQueueCache
{
    private record CompositeMeshReference(String mesh, UUID entity) {}

    private static final class VulkanMaterialCache
    {
        // Second Level - Material Instance -> Meshes
        final ICollectionMap<String, CompositeMeshReference> materials;

        public VulkanMaterialCache()
        {
            this.materials = new SetMap<>();
        }
    }

    // First Level - MaterialArchetype -> Material Instances
    final Map<String, VulkanMaterialCache> archetypes;

    public void addMaterialArchetype(final IAssetReference<? extends IMaterialArchetype> type)
    {
        if (!this.archetypes.containsKey(type.getKey()))
        {
            this.archetypes.put(type.getKey(), new VulkanMaterialCache());
        }
    }

    public void removeMaterialArchetype(final IAssetReference<? extends IMaterialArchetype> type)
    {
        // TODO: handle materials inside cache
        this.archetypes.remove(type.getKey());
    }

    public void addMeshInstance(final IAssetReference<? extends IMaterialArchetype> type,
                                final IAssetReference<? extends IMaterial> material,
                                final IAssetReference<IMesh> mesh,
                                final IEntity entity)
    {
        if (!this.archetypes.containsKey(type.getKey()))
        {
            this.addMaterialArchetype(type);
        }

        final VulkanMaterialCache cache = this.archetypes.get(type.getKey());
        cache.materials.put(material.getKey(), new CompositeMeshReference(mesh.getKey(), entity.getUUID()));
    }

    public void removeMaterialInstance()
    {

    }

    public VulkanMaterialQueueCache()
    {
        this.archetypes = new SetMap<>();
    }
}
