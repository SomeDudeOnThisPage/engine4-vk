package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import org.joml.*;

public class VulkanBufferedUniform<T> extends VulkanDescriptor
{
    // TODO: actual alignment for other graphics cards
    private static final int N = 4;

    protected String name;
    protected int alignment;
    private T data;

    public static int alignment(final VulkanBufferedUniform<?> uniform)
    {
        if (uniform.data instanceof Integer || uniform.data instanceof Float)
        {
            return N;
        }
        else if (uniform.data instanceof Vector2f || uniform.data instanceof Vector2i)
        {
            return 2 * N;
        }
        else if (uniform.data instanceof Vector3f || uniform.data instanceof Vector3i || uniform.data instanceof Vector4f || uniform.data instanceof Vector4i)
        {
            return 4 * N;
        }
        else if (uniform.data instanceof Matrix3f || uniform.data instanceof Matrix4f)
        {
            // are 3-column/row matrices stored with a base alignment of 16N? Test this!
            return 4 * 4 * N;
        }

        throw new UnsupportedOperationException("could not calculate base uniform buffer alignment for data type '" + uniform.data.getClass().getSimpleName() + "'");
    }

    public int getAlignment()
    {
        return this.alignment;
    }

    public T get()
    {
        return this.data;
    }

    public void set(final T data)
    {
        this.data = data;
    }

    public String getName()
    {
        return this.name;
    }

    protected VulkanBufferedUniform() {}

    public VulkanBufferedUniform(final String name, final T data)
    {
        this.name = name;
        this.data = data;
        this.alignment = VulkanBufferedUniform.alignment(this);
    }
}
