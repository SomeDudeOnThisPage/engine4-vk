package me.buhlmann.engine4.api.configuration;

import org.joml.Vector2i;

public interface IEngineConfiguration
{
    enum VSyncState
    {
        /**
         * Always wait for next vertical refresh period.
         */
        VSYNC_PRESENT_FIFO,

        /**
         * Wait for the next vertical refresh period, unless the one after last presentation of the frame has already passed.
         */
        VSYNC_PRESENT_FIFO_RELAXED,

        /**
         * Never wait for next vertical refresh period.
         */
        VSYNC_PRESENT_IMMEDIATE
    }

    enum RenderingAPI
    {
        VULKAN
        // OPENGL
    }

    VSyncState getVSyncState();

    RenderingAPI getRenderingAPI();

    Vector2i getDesiredWindowSize();
}
