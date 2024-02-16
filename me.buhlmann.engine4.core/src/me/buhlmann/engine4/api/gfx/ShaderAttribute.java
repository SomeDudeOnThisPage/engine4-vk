package me.buhlmann.engine4.api.gfx;

import me.buhlmann.engine4.api.gfx.primitive.VertexDataType;

public enum ShaderAttribute
{
    V_POSITION("v_position", 0, VertexDataType.FLOAT3),
    V_NORMAL("v_normal", 2, VertexDataType.FLOAT3),
    V_TEXTURE("v_uv", 1, VertexDataType.FLOAT2),
    V_TANGENT("v_tangent", 3, VertexDataType.FLOAT3),
    V_BITANGENT("v_bitangent", 4, VertexDataType.FLOAT3);

    /**
     * The name of the attribute, as mirrored in shader sources.
     */
    private final String name;

    /**
     * The location of the attribute, as mirrored in shader sources.
     */
    private final int location;

    /**
     * The OpenGL data type of the data.
     */
    private final VertexDataType data;

    public String getName()
    {
        return this.name;
    }

    public int getLocation()
    {
        return this.location;
    }

    public VertexDataType getDataType()
    {
        return this.data;
    }

    public int getSize()
    {
        return this.data.getSize();
    }

    ShaderAttribute(String name, int location, VertexDataType data)
    {
        this.name = name;
        this.location = location;
        this.data = data;
    }
}
