package me.buhlmann.engine4.api.gfx.primitive;

import me.buhlmann.engine4.api.IAssetFactory;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.gfx.material.IMaterialFactory;
import me.buhlmann.engine4.asset.AssetReference;

import javax.xml.bind.annotation.*;
import java.util.List;

public interface IModelFactory extends IAssetFactory<IModel, IModelFactory.MetaData>
{
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "model")
    @XmlType(name="IModelFactory_MetaData")
    final class MetaData extends IAssetFactory.MetaData
    {
        @XmlElement public String mesh;
        @XmlElement(nillable = true) public IMaterialFactory.MetaData material;

        @Override
        public void getAssociatedFiles(List<String> files)
        {
            files.add(this.mesh);
        }
    }

    @Override
    default String getTag()
    {
        return "model";
    }

    @Override
    default IAssetReference<IModel> createAssetReference(final String key)
    {
        return new AssetReference<>(IModel.class, key);
    }

    default Class<IModelFactory.MetaData> getMetaDataClass()
    {
        return IModelFactory.MetaData.class;
    }
}
