package me.buhlmann.engine4.api;

import me.buhlmann.engine4.api.exception.AssetNotFoundException;

public interface IAssetFamily<T extends IAsset> extends IDisposable
{
    IAssetReference<T> get(final String asset) throws AssetNotFoundException;
    void put(final String key, final IAssetReference<T> asset);

    void flush();
}
