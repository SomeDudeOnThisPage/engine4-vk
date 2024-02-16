package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.asset2.IAsset2;
import me.buhlmann.engine4.api.asset2.IAssetDefinition;
import me.buhlmann.engine4.api.asset2.IStreamAssetFactory;

public class StreamAssetLoadTask<T extends IAsset2> implements Runnable
{
    private final GCAssetManager gcAssetManager;
    private final IAssetDefinition<T> definition;

    public StreamAssetLoadTask(final GCAssetManager gcAssetManager, final IStreamAssetFactory<T> factory, final IAssetDefinition<T> definition)
    {
        this.gcAssetManager = gcAssetManager;
        this.definition = definition;
    }

    @Override
    public void run()
    {
        try
        {
            final IStreamAssetFactory<T> factory = this.gcAssetManager.createAssetFactory(this.definition.getType());
            factory.load(this.definition, IStreamAssetFactory.Thread.LOADING_THREAD);
        }
        catch (final Exception e)
        {
            Engine4.getLogger().error(e);
        }
    }
}
