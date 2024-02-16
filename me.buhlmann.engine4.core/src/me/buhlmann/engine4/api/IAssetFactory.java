package me.buhlmann.engine4.api;

import me.buhlmann.engine4.asset.AssetReference;

import javax.xml.bind.annotation.*;
import java.util.List;

public interface IAssetFactory<T extends IAsset, M extends IAssetFactory.MetaData>
{
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "abstract-meta-data")
    abstract class MetaData
    {
        @XmlAttribute public String id;
        @XmlAttribute public String initialization;

        public String getInitialization()
        {
            return this.initialization.isEmpty() ? "async" : "sync";
        }

        public abstract void getAssociatedFiles(final List<String> files);
    }

    T load(Object meta);
    void initialize(IAssetReference<T> reference);
    String getTag();
    IAssetReference<T> createAssetReference(final String tag);

    Class<M> getMetaDataClass();
}
