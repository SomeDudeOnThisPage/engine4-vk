package me.buhlmann.engine4.api.gfx.primitive;

import me.buhlmann.engine4.api.gfx.ShaderAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VertexBufferLayout
{
    public static final class BufferElement
    {
        private final ShaderAttribute attribute;
        private int offset;

        public int getLocation()
        {
            return this.attribute.getLocation();
        }

        public void setOffset(int offset)
        {
            this.offset = offset;
        }

        public int getOffset()
        {
            return this.offset;
        }

        public ShaderAttribute getAttribute()
        {
            return this.attribute;
        }

        public VertexDataType getType()
        {
            return this.attribute.getDataType();
        }

        public BufferElement(ShaderAttribute attribute)
        {
            this.attribute = attribute;
        }
    }

    private final List<BufferElement> elements;
    private int stride;

    private void recalculate()
    {
        this.stride = 0;
        int offset = 0;
        for (BufferElement element : this.elements)
        {
            element.setOffset(offset);
            offset += element.getType().getSize();
            this.stride += element.getType().getSize();
        }
    }

    public int getStride()
    {
        return this.stride;
    }

    public List<BufferElement> getElements()
    {
        return this.elements;
    }

    public void addElement(BufferElement element)
    {
        this.elements.add(element);
        this.recalculate();
    }

    public VertexBufferLayout(BufferElement... elements)
    {
        this.elements = new ArrayList<>();
        this.elements.addAll(Arrays.asList(elements));

        this.recalculate();
    }
}
