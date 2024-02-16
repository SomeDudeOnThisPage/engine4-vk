package me.buhlmann.engine4.api.asset2;

import me.buhlmann.engine4.api.exception.AssetFamilyNotFoundException;

public interface IAssetManager2
{
    void update();
    void shutdown();

    boolean isLoading();

    <T extends IAsset2> T get(final Class<T> family, final String key) throws AssetFamilyNotFoundException;
    <T extends IAsset2> boolean contains(final Class<T> family, final String key) throws AssetFamilyNotFoundException;
    <T extends IAsset2> void put(final Class<T> family, final String key, final T asset);

    <T extends IAsset2> T load(final IAssetDefinition<T> definition);
}
