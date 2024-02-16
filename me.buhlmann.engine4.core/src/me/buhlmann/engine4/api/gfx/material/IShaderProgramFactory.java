package me.buhlmann.engine4.api.gfx.material;

import me.buhlmann.engine4.api.IAssetFactory;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.asset.AssetReference;
import me.buhlmann.engine4.utils.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

public interface IShaderProgramFactory extends IAssetFactory<IShaderProgram, IShaderProgramFactory.MetaData>
{
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "model")
    final class MetaData extends IAssetFactory.MetaData
    {

        @XmlElement(nillable = true) public String vs;
        @XmlElement(nillable = true) public String fs;
        @XmlElement(nillable = true) public String combined;

        public boolean isCombinationShader()
        {
            return StringUtils.isNullOrBlank(this.vs) && StringUtils.isNullOrBlank(this.fs) && !StringUtils.isNullOrBlank(this.combined);
        }

        @Override
        public void getAssociatedFiles(final List<String> files)
        {
            // TODO: Maybe also handle/list includes here?
            this.addAssociatedFile(this.vs, files);
            this.addAssociatedFile(this.fs, files);
            this.addAssociatedFile(this.combined, files);
        }

        private void addAssociatedFile(final String file, final List<String> files)
        {
            if (!StringUtils.isNullOrBlank(file))
            {
                files.add(file);
            }
        }
    }

    @Override
    default IAssetReference<IShaderProgram> createAssetReference(final String key)
    {
        return new AssetReference<>(IShaderProgram.class, key);
    }

    default Class<IShaderProgramFactory.MetaData> getMetaDataClass()
    {
        return IShaderProgramFactory.MetaData.class;
    }

    @Override
    default String getTag()
    {
        return "program";
    }

}
