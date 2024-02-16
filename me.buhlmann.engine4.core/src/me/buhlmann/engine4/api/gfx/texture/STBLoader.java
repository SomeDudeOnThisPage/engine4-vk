package me.buhlmann.engine4.api.gfx.texture;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.utils.StringUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBDXT;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBPerlin;
import org.lwjgl.stb.STBVorbisInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class STBLoader
{
    private final IntBuffer width;
    private final IntBuffer height;
    private final IntBuffer components;
    private ByteBuffer data;

    private final boolean loaded;

    public int width()
    {
        return this.width.get(0);
    }

    public int height()
    {
        return this.height.get(0);
    }

    public int channels()
    {
        return this.components.get(0);
    }

    public ByteBuffer data()
    {
        return this.data;
    }

    public void load(String path, int channels)
    {
        if (!this.loaded)
        {
            this.width.clear();
            this.height.clear();
            this.components.clear();
            this.data = null;


            this.data = STBImage.stbi_load(path, this.width, this.height, this.components, channels);

            if (!StringUtils.isNullOrBlank(STBImage.stbi_failure_reason()))
            {
                Engine4.getLogger().error(STBImage.stbi_failure_reason() + " - " + path);
            }
        }
    }

    public void free()
    {
        if (this.data != null)
        {
            STBImage.stbi_image_free(this.data);
        }
    }

    public STBLoader()
    {
        this.width = BufferUtils.createIntBuffer(1);
        this.height = BufferUtils.createIntBuffer(1);
        this.components = BufferUtils.createIntBuffer(1);
        this.loaded = false;
    }
}
