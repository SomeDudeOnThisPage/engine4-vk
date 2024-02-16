package me.buhlmann.engine4.utils;

import com.sun.istack.NotNull;
import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetReference;

public final class AssetUtils
{
    public static synchronized boolean isNotLoaded(@NotNull final IAssetReference<? extends IAsset> reference)
    {
        return !AssetUtils.isLoaded(reference);
    }

    public static synchronized boolean isLoaded(@NotNull final IAssetReference<? extends IAsset> reference)
    {
        return reference.getLoadingStage() == IAssetReference.LoadingStage.LOADED && reference.get() != null;
    }

    private AssetUtils()
    {
    }
}
