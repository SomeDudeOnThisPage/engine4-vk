package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.*;
import me.buhlmann.engine4.api.exception.AssetFamilyNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class AssetManager implements IAssetManager
{
    /**
     * Queue for assets to be loaded from the filesystem asynchronously.
     */
    private final Queue<AssetLoadTask<? extends IAsset>> queue;

    /**
     * Staging-Queue for loaded assets to be initialized synchronously.
     */
    private final Queue<IAssetReference<? extends IAsset>> staging;

    /**
     * Asset-Families grouping assets by type-class.
     */
    private final Map<Class<? extends IAsset>, AssetFamily<? extends IAsset>> families;

    /**
     * Asset-Factories addressed by type-class.
     */
    private final Map<Class<? extends IAsset>, IAssetFactory<? extends IAsset, ?>> factories;

    private final Map<String, Long> trackers;

    private AssetLoadThread thread;

    /**
     * Initializes a single {@link IAssetReference}.
     * @param reference The reference to be initialized.
     * @param <T> {@link IAsset}-bounded type parameter.
     */
    @SuppressWarnings("unchecked")
    private <T extends IAsset> void initialize(final IAssetReference<T> reference)
    {
        ((IAssetFactory<T, ?>) this.factories.get(reference.getType())).initialize(reference);
        reference.setLoadingStage(IAssetReference.LoadingStage.LOADED);
        this.staging.remove(reference);
        this.put(reference.getType(), reference.getKey(), reference);
        if (this.trackers.containsKey(reference.getKey()))
        {
            Engine4.getLogger().info("[ASSET MANAGER] added " + reference.getKey() + " to the fully loaded asset list - loaded in "
                + (System.currentTimeMillis() - this.trackers.get(reference.getKey())) + "ms");
            this.trackers.remove(reference.getKey());
        }
        else
        {
            Engine4.getLogger().info("[ASSET MANAGER] added " + reference.getKey() + " to the fully loaded asset list");
        }
    }

    /* package-private */ Queue<AssetLoadTask<? extends IAsset>> getAssetLoadingQueue()
    {
        return this.queue;
    }

    /* package-private */ Queue<IAssetReference<? extends IAsset>> getAssetStagingQueue()
    {
        return this.staging;
    }

    /* package-private*/ void addLoadingTimePerformanceTracker(final String key)
    {
        this.trackers.put(key, System.currentTimeMillis());
    }

    /**
     * Ticks the {@link AssetManager}, updating the loading and staging queues (if applicable).
     */
    @Override
    public void update()
    {
        // (Re-) start the asset loading thread if we need to load more assets.
        if (!this.queue.isEmpty() && (this.thread == null || !this.thread.isAlive()))
        {
            this.thread = new AssetLoadThread();
            this.thread.start();
        }

        for (final IAssetReference<? extends IAsset> reference : this.staging)
        {
            this.initialize(reference);
        }

        this.staging.clear();
    }

    /**
     * Terminates the {@link AssetManager}, all contained {@link IAsset Assets} and its' loading thread.
     */
    @Override
    public void terminate()
    {
        this.families.values().forEach(IAssetFamily::flush);
        if (this.thread != null)
        {
            this.thread.terminate();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IAsset> IAssetReference<T> request(Class<T> family, String key) throws AssetFamilyNotFoundException
    {
        if (!this.families.containsKey(family))
        {
            this.families.put(family, new AssetFamily<>());
        }

        if (this.families.containsKey(family))
        {
            final IAssetReference<T> reference = (IAssetReference<T>) this.families.get(family).get(key);
            if (reference instanceof IReferenceCounted counted)
            {
                counted.addReference();
            }

            return reference;
        }

        throw new AssetFamilyNotFoundException(family.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IAsset> void release(Class<T> family, String key) throws AssetFamilyNotFoundException
    {
        if (this.families.containsKey(family))
        {
            final IAssetReference<T> reference = (IAssetReference<T>) this.families.get(family).get(key);
            if (reference instanceof IReferenceCounted counted)
            {
                counted.removeReference();

                // TODO: Add to removal queue.
                // if (counted.getReferenceCount() <= 0)
                // {
                // }
            }

            return;
        }

        throw new AssetFamilyNotFoundException(family.getName());
    }

    @Override
    public <T extends IAsset> boolean contains(Class<T> family, String key) throws AssetFamilyNotFoundException
    {
        if (!this.families.containsKey(family))
        {
            throw new AssetFamilyNotFoundException(family.getName());
        }

        return this.families.get(family).get(key) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IAsset> void put(Class<T> family, String key, IAssetReference<T> reference)
    {
        // Engine4.getLogger().trace("[ASSET MANAGER] put asset " + key + " into family " + family.getName());
        if (!this.families.containsKey(family))
        {
            Engine4.getLogger().trace("[ASSET MANAGER] added asset family with key " + family);
            this.families.put(family, new AssetFamily<>());
        }
        ((IAssetFamily<T>) this.families.get(family)).put(key, reference);
    }

    /**
     * @param family Asset family type.
     * @param factory Asset factory.
     * @param <T> Type parameter.
     * @deprecated In future versions, asset factories will be created implicitly by the asset load thread, to allow
     *             loading multiple assets with the same asset type asynchronously.
     */
    @Deprecated
    public <T extends IAsset> void addAssetFactory(Class<T> family, IAssetFactory<T, ?> factory)
    {
        this.factories.put(family, factory);
    }

    @Override
    @Deprecated
    public IAssetFactory<? extends IAsset, ?> getAssetFactory(final String tag)
    {
        for (IAssetFactory<? extends IAsset, ?> factory : this.factories.values())
        {
            if (factory.getTag().equals(tag))
            {
                return factory;
            }
        }

        throw new UnsupportedOperationException("factory " + tag + " not found");
    }

    @Override
    public boolean isLoading()
    {
        return this.queue.isEmpty() && this.staging.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Deprecated
    public <T extends IAsset> IAssetFactory<T, ?> getAssetFactory(Class<T> family)
    {
        return (IAssetFactory<T, ?>) this.factories.get(family);
    }

    public AssetManager()
    {
        this.families = new HashMap<>();
        this.factories = new HashMap<>();
        this.queue = new LinkedBlockingQueue<>();
        this.staging = new LinkedBlockingQueue<>();
        this.trackers = new HashMap<>();
    }
}
