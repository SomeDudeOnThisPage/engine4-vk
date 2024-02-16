package me.buhlmann.engine4.core.gfx.vulkan.descriptor;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class VulkanBufferedUniformStruct<T> extends VulkanBufferedUniform<T>
{
    /** String identifiers mapped to BufferedUniform instances. */
    private final HashMap<String, VulkanBufferedUniform<?>> uniforms;

    @Override
    public T get()
    {
        throw new UnsupportedOperationException("cannot get data of BufferedUniformStruct - get the data from the member" +
            "uniforms instead");
    }

    @Override
    public void set(T data)
    {
        throw new UnsupportedOperationException("cannot set data of BufferedUniformStruct - set the data of the member" +
            "uniforms instead");
    }

    public VulkanBufferedUniformStruct(VulkanBufferedUniform<?>... uniforms)
    {
        this.name = "";
        this.uniforms = new LinkedHashMap<>(); // not ideal but for this implementation we need to keep insertion order

        // alignment is calculated from parts, not from N
        int offset = 0;
        for (VulkanBufferedUniform<?> uniform : uniforms)
        {
            // System.out.println(uniform.name());
            this.uniforms.put(uniform.getName(), uniform);
            offset += VulkanBufferedUniform.alignment(uniform);
        }

        this.alignment = offset;
    }

    public HashMap<String, VulkanBufferedUniform<?>> getUniforms()
    {
        return this.uniforms;
    }
}
