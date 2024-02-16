package me.buhlmann.engine4.utils;

import me.buhlmann.engine4.Engine4;

public final class Paths
{
    public static synchronized String resolve(final String relative)
    {
        return Engine4.getInstance().getArguments().platform + "/" + relative;
    }

    private Paths()
    {

    }
}
