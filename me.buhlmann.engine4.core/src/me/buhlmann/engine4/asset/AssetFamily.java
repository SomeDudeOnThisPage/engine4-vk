package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetFamily;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.IDisposable;
import me.buhlmann.engine4.api.exception.AssetNotFoundException;

import java.util.HashMap;

public class AssetFamily<T extends IAsset> implements IAssetFamily<T>
{
    private final HashMap<String, IAssetReference<T>> assets;

    @Override
    public IAssetReference<T> get(final String key) throws AssetNotFoundException
    {
        if (this.assets.containsKey(key))
        {
            return this.assets.get(key);
        }
        return null;
    }

    @Override
    public void put(final String key, final IAssetReference<T> asset)
    {
        this.assets.put(key, asset);
    }

    @Override
    public void flush()
    {
        for (final IAssetReference<T> asset : this.assets.values())
        {
            if (asset.get() instanceof IDisposable disposable)
            {
                disposable.dispose();
            }
        }
        this.assets.clear();
    }

    @Override
    public void dispose()
    {
        for (final IAssetReference<T> asset : this.assets.values())
        {
            if (asset instanceof IDisposable) {
                ((IDisposable) asset).dispose();
            }
        }
    }

    public AssetFamily()
    {
        this.assets = new HashMap<>();
    }
}
