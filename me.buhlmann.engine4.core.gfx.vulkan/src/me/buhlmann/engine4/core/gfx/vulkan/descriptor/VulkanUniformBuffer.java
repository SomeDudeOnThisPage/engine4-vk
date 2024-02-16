package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import com.sun.istack.NotNull;
import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.gfx.uniform.IUniformBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VK11;

import java.util.*;

public class VulkanUniformBuffer extends VulkanBuffer implements IUniformBuffer, IVulkanDisposable
{
    public static class UniformNotFoundException extends RuntimeException
    {
        public UniformNotFoundException(final String uniform, final IUniformBuffer buffer)
        {
            super("could not find uniform '" + uniform + "' in uniform buffer '" + buffer + "'");
        }
    }

    private final VulkanDescriptorSet vkDescriptorSet;
    private final VulkanDescriptorPool vkDescriptorPool;

    private final Map<String, VulkanBufferedUniform<?>> uniforms;
    private final Map<String, Integer> locations;

    private int extent;

    public static VulkanUniformBuffer create(final VulkanDeviceContext vkContext, final int binding, @NotNull final VulkanDescriptor... vkDescriptors)
    {
        long size = 0;
        for (final VulkanDescriptor vkDescriptor : vkDescriptors)
        {
            if (vkDescriptor instanceof VulkanBufferedUniform<?> vkBufferedUniform)
            {
                size += vkBufferedUniform.getAlignment();
            }
        }

        if (size <= 0)
        {
            throw new UnsupportedOperationException("cannot create uniform buffer of size " + size);
        }

        return new VulkanUniformBuffer(vkContext, size, binding, vkDescriptors);
    }

    @Override
    public void dispose()
    {
        super.dispose();
        this.vkDescriptorSet.dispose();
        this.vkDescriptorPool.dispose();
    }

    @Override
    public <T> void set(final String name, final T data)
    {
        final VulkanBufferedUniform<T> uniform = this.getUniform(name);
        if (uniform.get() != data)
        {
            uniform.set(data);
            this.update(name);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> VulkanBufferedUniform<T> getUniform(String uniform)
    {
        if (!this.uniforms.containsKey(uniform))
        {
            throw new UniformNotFoundException(uniform, this);
        }

        return (VulkanBufferedUniform<T>) this.uniforms.get(uniform);
    }

    public void update(String index)
    {
        VulkanBufferedUniform<?> uniform = this.uniforms.getOrDefault(index, null);
        if (uniform == null)
        {
            throw new UniformNotFoundException(index, this);
        }

        final int location = this.locations.getOrDefault(index, -1);
        if (location == -1)
        {
            throw new UniformNotFoundException(index, this);
        }

        final var data = uniform.get();
        if (data instanceof Matrix4f matrix4f)
        {
            VulkanUtils.copy(this, matrix4f, location);
        }
        else if (data instanceof Vector4f vector4f)
        {
            VulkanUtils.copy(this, vector4f, location);
        }
        else if (data instanceof Vector3f vector3f)
        {
            VulkanUtils.copy(this, new Vector4f(vector3f, 1.0f), location);
        }
        else if (data instanceof Integer integer)
        {
            VulkanUtils.copy(this, integer, location);
        }
        else if (data instanceof Float ft)
        {
            VulkanUtils.copy(this, ft, location);
        }
    }

    public void index(final String root, final VulkanBufferedUniform<?> uniform)
    {
        if (uniform instanceof VulkanBufferedUniformStruct<?> struct)
        {
            for (VulkanBufferedUniform<?> sUniform : struct.getUniforms().values())
            {
                this.index(root + uniform.getName() + "." /* append '.' to mirror glsl syntax */, sUniform);
            }
        }
        else if (uniform instanceof VulkanBufferedUniformArray<?> array)
        {
            int i = 0;
            for (VulkanBufferedUniform<?> aUniform : array.getUniforms())
            {
                this.index(root + uniform.getName() + "[" + (i++) + "]", aUniform);
            }
        }
        else
        {
            this.uniforms.put(root + uniform.getName(), uniform);
            this.locations.put(root + uniform.getName(), this.extent);
            Engine4.getLogger().trace("[VULKAN] indexed uniform " + root + uniform.getName() + ", offset = " + this.extent + ", size = " + uniform.getAlignment());
            this.extent += uniform.getAlignment();
        }
    }

    public VulkanDescriptorSetLayout[] getDescriptorSetLayouts()
    {
        return new VulkanDescriptorSetLayout[]
        {
            this.vkDescriptorSet.getDescriptorSetLayout()
        };
    }

    public long getDescriptorSetPointer()
    {
        return this.vkDescriptorSet.getPointer();
    }

    private VulkanUniformBuffer(final VulkanDeviceContext vkContext, final long size, final int location, final VulkanDescriptor... descriptors)
    {
        super(vkContext, size, VK11.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK11.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);

        this.uniforms = new HashMap<>();
        this.locations = new HashMap<>();
        this.extent = 0;

        final List<VulkanDescriptorPool.PooledDescriptorInfo> vkDescriptorTypeCounts = new ArrayList<>();
        vkDescriptorTypeCounts.add(new VulkanDescriptorPool.PooledDescriptorInfo(1, VK11.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER));

        final List<VulkanDescriptorSet.DescriptorBinding> vkDescriptorBindingInfo = new ArrayList<>();
        this.vkDescriptorPool = new VulkanDescriptorPool(vkContext, vkDescriptorTypeCounts);
        vkDescriptorBindingInfo.add(0,
            new VulkanDescriptorSet.DescriptorBinding(
                VK11.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                VK11.VK_SHADER_STAGE_ALL_GRAPHICS,
                0,
                this.getRequestedSize()
            )
        );

        for (VulkanDescriptor descriptor : descriptors)
        {
            if (descriptor instanceof VulkanBufferedUniform<?> vkBufferedUniform)
            {
                this.index("", vkBufferedUniform);
                this.set(vkBufferedUniform.getName(), vkBufferedUniform.get());
            }
            else if (descriptor instanceof VulkanUniformSampler vkUniformSampler)
            {
                vkDescriptorTypeCounts.add(new VulkanDescriptorPool.PooledDescriptorInfo(1, vkUniformSampler.getBinding().vkType));
                vkDescriptorBindingInfo.add(vkUniformSampler.getBinding());
            }
        }

        this.vkDescriptorSet = new VulkanDescriptorSet(
            location,
            this.vkDescriptorPool,
            vkDescriptorBindingInfo.toArray(new VulkanDescriptorSet.DescriptorBinding[0]),
            this,
            vkContext
        );
        Engine4.getLogger().trace("[VULKAN] created uniform buffer with a total extent of " + this.extent);
    }
}
