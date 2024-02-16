package me.buhlmann.engine4.api.exception;

public class AssetFamilyNotFoundException extends RuntimeException
{
    private final String key;

    @Override
    public String toString()
    {
        return "Asset family with key '" + key + "' could not be found";
    }

    public AssetFamilyNotFoundException(String key)
    {
        this.key = key;
    }
}
