package me.buhlmann.engine4.api.asset2;

public interface IStreamAssetFactory<T extends IAsset2>
{
    enum Thread
    {
        LOADING_THREAD,
        GFX_THREAD
    }

    final class AssetLoadingException extends Exception
    {
        public AssetLoadingException(final IStreamAssetFactory.Thread thread, final IAssetDefinition<?> definition, final String e)
        {
            this(thread, definition, new Exception(e));
        }

        public AssetLoadingException(final IStreamAssetFactory.Thread thread, final IAssetDefinition<?> definition, final Exception e)
        {
            super("could not load asset '" + definition.getKey() + "' on thread '" + thread.name() + "'", e);
        }
    }

    /**
     * This method <strong>must</strong> load an asset or throw an AssetLoadingException.
     * @param definition
     * @param thread
     * @throws AssetLoadingException
     */
    void load(final IAssetDefinition<T> definition, final IStreamAssetFactory.Thread thread) throws AssetLoadingException;
    T create(final IAssetDefinition<T> definition);
}
