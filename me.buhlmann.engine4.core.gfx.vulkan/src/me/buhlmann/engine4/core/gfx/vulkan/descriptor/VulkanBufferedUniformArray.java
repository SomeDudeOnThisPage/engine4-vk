package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import java.util.ArrayList;
import java.util.List;

public class VulkanBufferedUniformArray<T> extends VulkanBufferedUniform<T>
{
    private final int limit;
    private final List<VulkanBufferedUniform<?>> uniforms;

    public int getSize()
    {
        return this.limit;
    }

    public List<VulkanBufferedUniform<?>> getUniforms()
    {
        return this.uniforms;
    }

    @Override
    public T get()
    {
        throw new UnsupportedOperationException("cannot get data of BufferedUniformStruct - get the data from the member uniforms instead");
    }

    @Override
    public void set(T data)
    {
        throw new UnsupportedOperationException("cannot set data of BufferedUniformStruct - set the data of the member uniforms instead");
    }

    public VulkanBufferedUniformArray(String name, int limit, VulkanBufferedUniform<?>... uniforms)
    {
        this.name = name;
        this.uniforms = new ArrayList<>();

        this.alignment = limit * uniforms[0].getAlignment(); // total size (max number of elements * alignment per element)
        this.limit = limit;

        for (int i = 0; i < limit; i++)
        {
            if (i < uniforms.length && uniforms[i] != null)
            {
                uniforms[i].name = ""; // clear the name to match the notation
                this.uniforms.add(uniforms[i]);
            }
            else
            {
                // treat the first uniform as a template for all data in the array, as it needs to be filled
                this.uniforms.add(uniforms[0]);
            }

        }
    }
}
