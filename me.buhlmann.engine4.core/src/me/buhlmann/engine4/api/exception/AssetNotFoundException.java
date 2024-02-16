package me.buhlmann.engine4.api.exception;

public class AssetNotFoundException extends RuntimeException
{
    private final String key;

    @Override
    public String toString()
    {
        return "Asset with key '" + key + "' could not be found";
    }

    public AssetNotFoundException(String key)
    {
        this.key = key;
    }
}
