package me.buhlmann.engine4.api.gfx.texture;

import me.buhlmann.engine4.api.IAssetFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

public interface ITextureFactory extends IAssetFactory<ITexture, ITextureFactory.MetaData>
{
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "texture")
    final class MetaData extends IAssetFactory.MetaData
    {

        @XmlElement public String source;

        @Override
        public void getAssociatedFiles(List<String> files)
        {
            files.add(this.source);
        }
    }
}
