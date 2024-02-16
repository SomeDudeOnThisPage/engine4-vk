package me.buhlmann.engine4.api.asset2;

import java.io.IOException;
import java.io.InputStream;

public interface IAssetDefinition<T extends IAsset2>
{
    String getKey();
    // UUID getIDKey();

    String getSource();
    Class<T> getType();
    InputStream getInputStream() throws IOException;
}
