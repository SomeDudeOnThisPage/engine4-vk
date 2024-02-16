package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetReference;
import org.w3c.dom.Element;

public record AssetLoadTask<T extends IAsset>(Element data, IAssetReference<T> reference)
{
}
