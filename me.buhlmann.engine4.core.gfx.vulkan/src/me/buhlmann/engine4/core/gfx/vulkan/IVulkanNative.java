package me.buhlmann.engine4.core.gfx.vulkan;

import com.sun.istack.NotNull;

public interface IVulkanNative<T>
{
    @NotNull
    T getNative();
}
