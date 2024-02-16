package me.buhlmann.engine4.utils;

public final class StringUtils
{
    private StringUtils()
    {

    }

    public static boolean isNullOrBlank(final String string)
    {
        return string == null || string.isBlank() || string.isEmpty();
    }

    public static boolean isNullOrBlank(final String... strings)
    {
        for (final String string : strings)
        {
            if (StringUtils.isNullOrBlank(string))
            {
                return true;
            }
        }

        return false;
    }
}
