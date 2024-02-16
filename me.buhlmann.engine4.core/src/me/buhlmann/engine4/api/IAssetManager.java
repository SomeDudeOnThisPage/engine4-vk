package me.buhlmann.engine4.api;

import me.buhlmann.engine4.api.exception.AssetFamilyNotFoundException;
import me.buhlmann.engine4.asset.AssetLoadTask;

import java.util.Queue;

public interface IAssetManager
{
    void update();
    void terminate();

    <T extends IAsset> IAssetReference<T> request(final Class<T> family, final String key) throws AssetFamilyNotFoundException;
    <T extends IAsset> void release(final Class<T> family, final String key) throws AssetFamilyNotFoundException;

    <T extends IAsset> boolean contains(final Class<T> family, final String key) throws AssetFamilyNotFoundException;

    <T extends IAsset> void put(final Class<T> family, final String key, final IAssetReference<T> asset);

    IAssetFactory<? extends IAsset, ?> getAssetFactory(final String tag);

    <T extends IAsset> IAssetFactory<T, ? extends IAssetFactory.MetaData> getAssetFactory(final Class<T> family);

    boolean isLoading();
}
