package me.buhlmann.engine4.configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public final class Arguments
{
    @Parameter(names = {"--platform", "-p"}, description = "root folder of the engine platform resources")
    public String platform = "platform";

    @Parameter(names = {"--game", "-g"}, description = "root folder of the game resources")
    public String game = "platform/splash";

    public void parse(final String[] args)
    {
        JCommander.newBuilder()
            .addObject(this)
            .build()
            .parse(args);
    }
}
