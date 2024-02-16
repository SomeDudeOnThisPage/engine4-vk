package me.buhlmann.engine4.api.gfx.primitive;

import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.asset2.IAsset2;

public interface IMesh extends IAsset2
{
    record MeshData(String name, float[] vertices, float[] normals, float[] uv, int[] indices) {}
}
