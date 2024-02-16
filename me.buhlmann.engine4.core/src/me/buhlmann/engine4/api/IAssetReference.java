package me.buhlmann.engine4.api;

import com.sun.istack.NotNull;

public interface IAssetReference<T extends IAsset>
{
    enum LoadingStage
    {
        UNLOADED,
        LOADING_ASYNCHRONOUS,
        LOADED_ASYNCHRONOUS,
        // INITIALIZING_SYNCHRONOUS // no reason for this to exist, as other threads do not interact with the asset
        // at this point anymore
        INITIALIZED_SYNCHRONOUS,
        LOADED
    }
    T get();
    Class<T> getType();
    void set(@NotNull T asset);
    String getKey();
    void setLoadingStage(@NotNull final IAssetReference.LoadingStage stage);
    IAssetReference.LoadingStage getLoadingStage();
}
