package me.buhlmann.engine4.api.gfx.material;

import me.buhlmann.engine4.api.IAssetFactory;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.utils.StringUtils;

import javax.xml.bind.annotation.*;
import java.util.List;

public interface IMaterialFactory extends IAssetFactory<IMaterial, IMaterialFactory.MetaData>
{
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "material")
    @XmlType(name="IMaterialFactory_MetaData")
    final class MetaData extends IAssetFactory.MetaData
    {
        public static final class ShaderProgram
        {
            @XmlElement(nillable = true) public String vs;
            @XmlElement(nillable = true) public String fs;
            @XmlElement(nillable = true) public String combined;

            public boolean isCombinationShader()
            {
                return StringUtils.isNullOrBlank(this.vs) && StringUtils.isNullOrBlank(this.fs) && !StringUtils.isNullOrBlank(this.combined);
            }

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

        public static final class Uniform
        {
            @XmlAttribute public String name;
            @XmlAttribute public String type;
            @XmlAttribute public String mapping;
            @XmlValue public String uniform;
        }

        public static final class Sampler2D
        {
            @XmlAttribute public String name;
            @XmlAttribute public Integer binding;
            @XmlAttribute public String mapping;
            @XmlValue public String source;
        }

        @XmlAttribute public String pipeline;
        @XmlElement public ShaderProgram geometry;
        @XmlElement public ShaderProgram lighting;

        @XmlElement(name = "uniform") public List<Uniform> uniforms;
        @XmlElement(name = "sampler2d") public List<Sampler2D> samplers;

        @Override
        public void getAssociatedFiles(final List<String> files)
        {
            this.geometry.getAssociatedFiles(files);
            this.lighting.getAssociatedFiles(files);
        }
    }

    @Override
    default void initialize(IAssetReference<IMaterial> reference)
    {

    }

    @Override
    default String getTag()
    {
        return "material";
    }

    @Override
    default Class<MetaData> getMetaDataClass()
    {
        return IMaterialFactory.MetaData.class;
    }
}
