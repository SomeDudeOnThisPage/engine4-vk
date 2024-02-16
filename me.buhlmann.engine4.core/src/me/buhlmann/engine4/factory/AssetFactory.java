package me.buhlmann.engine4.factory;

import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.asset.AssetReference;

public final class AssetFactory
{
    public static <T extends IAsset> IAssetReference<T> createAssetReference(final String key, final Class<T> family)
    {
        return new AssetReference<T>(family, key);
    }

    public static <T extends IAsset> IAssetReference<T> wrap(final String key, final Class<T> family, final T data)
    {
        final IAssetReference<T> reference = new AssetReference<T>(family, key);
        reference.set(data);
        reference.setLoadingStage(IAssetReference.LoadingStage.LOADED);
        return reference;
    }

    private AssetFactory()
    {

    }
}
