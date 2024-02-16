package me.buhlmann.engine4.api.gfx.primitive;

@SuppressWarnings("unused")
public enum VertexDataType
{
    INT1(4, 1, "int"),
    INT2(8, 2, "ivec2"),
    INT3(12, 3, "ivec3"),
    INT4(16, 4, "ivec4"),

    FLOAT1(4, 1, "float"),
    FLOAT2(8, 2, "vec2"),
    FLOAT3(12, 3, "vec3"),
    FLOAT4(16, 4, "vec4"),

    MATRIX2F(16, 4, "mat2"),
    MATRIX3F(36, 9, "mat3"),
    MATRIX4F(64, 16, "mat4"),

    MATRIX2I(16, 4, "ERROR_DATA_TYPE_NOT_SUPPORTED"),
    MATRIX3I(36, 9, "ERROR_DATA_TYPE_NOT_SUPPORTED"),
    MATRIX4I(64, 16, "ERROR_DATA_TYPE_NOT_SUPPORTED"),

    BOOLEAN(1, 1, "bool");

    /**
     * Size in bytes.
     */
    private final int size;

    /**
     * Amount of components - e.g. a 3-Component Vector.
     */
    private final int components;

    private final String name;

    public String getGLSLName()
    {
        return this.name;
    }

    public int getSize()
    {
        return this.size;
    }

    public int getComponentAmount()
    {
        return this.components;
    }

    VertexDataType(int size, int components, String name)
    {
        this.size = size;
        this.components = components;
        this.name = name;
    }
}
