package me.buhlmann.engine4.core.gfx.vulkan;

import com.sun.istack.NotNull;

public abstract class AbstractVulkanLogicalDeviceContainer
{
    private final VulkanLogicalDevice vkLogicalDevice;

    @NotNull
    public VulkanLogicalDevice getLogicalDevice()
    {
        return this.vkLogicalDevice;
    }

    protected AbstractVulkanLogicalDeviceContainer(@NotNull VulkanLogicalDevice vkLogicalDevice)
    {
        this.vkLogicalDevice = vkLogicalDevice;
    }
}
