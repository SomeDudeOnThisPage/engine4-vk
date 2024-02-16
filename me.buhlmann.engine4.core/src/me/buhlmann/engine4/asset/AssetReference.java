package me.buhlmann.engine4.asset;

import com.sun.istack.NotNull;
import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetReference;

public class AssetReference<T extends IAsset> implements IAssetReference<T>
{
    private final String key;
    private final Class<T> type;

    private T asset;
    private IAssetReference.LoadingStage stage;

    @Override
    public T get()
    {
        // if (!this.getLoadingStage().equals(IAssetReference.LoadingStage.LOADED))
        // {
        //     throw new Exception();
        // }
        return this.asset;
    }

    @Override
    public Class<T> getType()
    {
        return this.type;
    }

    @Override
    public void set(@NotNull T asset)
    {
        if (this.asset == null)
        {
            this.asset = asset;
        }
    }

    @Override
    public String getKey()
    {
        return this.key;
    }

    @Override
    public IAssetReference.LoadingStage getLoadingStage()
    {
        return this.stage;
    }

    @Override
    public void setLoadingStage(final IAssetReference.LoadingStage stage)
    {
        this.stage = stage;
    }

    public AssetReference(final Class<T> type, final String key)
    {
        this.type = type;
        this.key = key;
        this.stage = IAssetReference.LoadingStage.UNLOADED;
    }
}
