package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.asset2.IAsset2;
import me.buhlmann.engine4.api.asset2.IAssetDefinition;
import me.buhlmann.engine4.api.asset2.IAssetManager2;
import me.buhlmann.engine4.api.asset2.IStreamAssetFactory;
import me.buhlmann.engine4.api.exception.AssetFamilyNotFoundException;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GCAssetManager implements IAssetManager2
{
    // final AssetDefinition definition = new FilesystemAssetDefinition("texture.my-id", "path:path/to/texture", Texture.class);
    // final Texture2D myTexture = assetManager.load(definition);
    final Map<Class<? extends IAsset2>, Map<String, WeakReference<? extends IAsset2>>> assets;
    final ReferenceQueue<IAsset2> queue;
    final List<IAsset2.IAssetReference> references;

    private final Map<Class<? extends IAsset2>, Class<? extends IStreamAssetFactory<? extends IAsset2>>> factories;
    private final List<Future<?>> futures;
    private final ExecutorService assetLoadingThreadPool;

    public GCAssetManager()
    {
        this.assets = new HashMap<>();
        this.queue = new ReferenceQueue<>();
        this.references = new ArrayList<>();

        this.factories = new HashMap<>();
        this.assetLoadingThreadPool = Executors.newFixedThreadPool(3);
        this.futures = new LinkedList<>();
    }

    @Override
    public <T extends IAsset2> void put(Class<T> family, String key, T asset)
    {
        if (!this.assets.containsKey(family))
        {
            this.assets.put(family, new HashMap<>());
        }

        final Map<String, WeakReference<? extends IAsset2>> assets = this.assets.get(family);
        if (assets.containsKey(key))
        {
            if (assets.get(key).get() != null)
            {
                assets.get(key).get().dispose();
                // TODO: in this situation, cleanup resources?
            }
        }
        assets.put(key, new WeakReference<>(asset));
        this.references.add(new IAsset2.IAssetReference(asset, this.queue));
    }

    @Override
    public void update()
    {
        // Cleanup everything not referenced anymore...
        Reference<? extends IAsset2> reference;
        while ((reference = this.queue.poll()) != null)
        {
            ((IAsset2.IAssetReference) reference).dispose();
        }

        for (final Future<?> future : this.futures)
        {
            if (future.isDone())
            {

            }
        }

        this.futures.removeIf((future) -> future.isDone() || future.isCancelled());
    }

    @Override
    public void shutdown()
    {

    }

    @Override
    public boolean isLoading()
    {
        return !this.futures.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IAsset2> T get(Class<T> clazz, String key) throws AssetFamilyNotFoundException
    {
        final Map<String, WeakReference<? extends IAsset2>> family = this.assets.getOrDefault(clazz, null);
        if (family == null)
        {
            throw new AssetFamilyNotFoundException(clazz.getName());
        }

        if (!family.containsKey(key))
        {
            return null;
            // throw new RuntimeException("asset not found MAKE THIS ITS OWN EXCEPTION CRINGE ASS");
        }

        return (T) family.get(key);
    }

    @Override
    public <T extends IAsset2> boolean contains(Class<T> family, String key) throws AssetFamilyNotFoundException
    {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IAsset2> T load(IAssetDefinition<T> definition)
    {
        try
        {
            final Class<IStreamAssetFactory<T>> clazz = (Class<IStreamAssetFactory<T>>) this.factories.get(definition.getType());
            final IStreamAssetFactory<T> factory = clazz.getDeclaredConstructor().newInstance();
            this.futures.add(this.assetLoadingThreadPool.submit(new StreamAssetLoadTask<>(this, factory, definition)));
            return factory.create(definition);
        }
        catch (final Exception e)
        {
            Engine4.getLogger().error(e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    /* package-private */ <T extends IAsset2> IStreamAssetFactory<T> createAssetFactory(Class<T> clazz)
    {
        if (!this.factories.containsKey(clazz))
        {
            throw new AssetFamilyNotFoundException(clazz.getName());
        }

        try
        {
            return (IStreamAssetFactory<T>) this.factories.get(clazz).getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            throw new AssetFamilyNotFoundException(e.getMessage());
        }
    }
}
