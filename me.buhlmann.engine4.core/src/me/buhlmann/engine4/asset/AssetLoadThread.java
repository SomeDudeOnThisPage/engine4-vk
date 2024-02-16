package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetFactory;
import me.buhlmann.engine4.api.IAssetReference;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

public class AssetLoadThread extends Thread
{
    private boolean terminated;

    private <T extends IAsset> void loadSynchronous(final AssetManager am, final AssetLoadTask<T> task)
    {
        try
        {
            IAssetReference<T> reference = task.reference();
            IAssetFactory<T, ? extends IAssetFactory.MetaData> factory = am.getAssetFactory(reference.getType());
            // I AM SO DUMB
            // HURR DURR WHY DOES THIS CRASH ALL THE TIME WHEN I LOAD MULTIPLE OF ONE ASSET TYPE AT ONCE
            // WHY THE FUCK DID I DESIGN IT IN A WAY THAT THE ASSET MANAGER HOLDS ONLY ONE INSTANCE OF THE FACTORY
            // AT ONE TIME
            // TODO: REDESIGN THIS, AND CREATE A SEPARATE FACTORY INSTANCE FOR EACH ASSET...
            //       For now, just sync on the factory.
            synchronized (factory)
            {
                final JAXBContext context = JAXBContext.newInstance(factory.getMetaDataClass());
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                final Object data = unmarshaller.unmarshal(task.data());
                final T loaded = factory.load(data);
                reference.set(loaded);
                reference.setLoadingStage(IAssetReference.LoadingStage.LOADED_ASYNCHRONOUS);
            }
        }
        catch (final Exception e)
        {
            Engine4.getLogger().error(e);
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public void run()
    {
        if (Engine4.getAssetManager() instanceof AssetManager manager)
        {
            while (!manager.getAssetLoadingQueue().isEmpty() && !this.terminated)
            {
                final AssetLoadTask<? extends IAsset> task = manager.getAssetLoadingQueue().poll();
                if (task != null && task.data() != null && task.reference() != null)
                {
                    this.loadSynchronous(manager, task);
                    Engine4.getLogger().trace("[ASSET MANAGER] loaded asset " + task.reference().getKey() + " synchronously on loading thread");
                    manager.getAssetStagingQueue().add(task.reference());
                }
            }
        }
    }

    public void terminate()
    {
        this.terminated = true;
    }
}
