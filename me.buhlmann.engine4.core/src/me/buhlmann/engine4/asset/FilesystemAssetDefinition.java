package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.api.asset2.IAsset2;
import me.buhlmann.engine4.api.asset2.IAssetDefinition;
import me.buhlmann.engine4.utils.Paths;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FilesystemAssetDefinition<T extends IAsset2> implements IAssetDefinition<T>
{
    private final String key;
    private final String source;
    private final Class<T> type;

    @Override
    public String getKey()
    {
        return this.key;
    }

    @Override
    public String getSource()
    {
        return this.source;
    }

    @Override
    public Class<T> getType()
    {
        return this.type;
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException
    {
        return new FileInputStream(Paths.resolve(this.source));
    }

    public FilesystemAssetDefinition(final String key, final String source, final Class<T> type)
    {
        this.key = key;
        this.source = source;
        this.type = type;
    }
}
