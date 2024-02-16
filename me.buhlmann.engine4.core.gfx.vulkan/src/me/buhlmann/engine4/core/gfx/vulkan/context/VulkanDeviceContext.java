package me.buhlmann.engine4.core.gfx.vulkan.context;

import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanInstance;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanLogicalDevice;
import me.buhlmann.engine4.core.gfx.vulkan.VulkanPhysicalDevice;

public class VulkanDeviceContext implements IVulkanDisposable
{
    protected final VulkanInstance vkInstance;
    protected final VulkanPhysicalDevice vkPhysicalDevice;
    protected final VulkanLogicalDevice vkLogicalDevice;

    @Override
    public void dispose()
    {
        this.vkLogicalDevice.dispose();
        this.vkPhysicalDevice.dispose();
        this.vkInstance.dispose();
    }

    public VulkanInstance getInstance()
    {
        return this.vkInstance;
    }

    public VulkanPhysicalDevice getPhysicalDevice()
    {
        return this.vkPhysicalDevice;
    }

    public VulkanLogicalDevice getLogicalDevice()
    {
        return this.vkLogicalDevice;
    }

    public VulkanDeviceContext(final VulkanInstance vkInstance, final VulkanPhysicalDevice vkPhysicalDevice, final VulkanLogicalDevice vkLogicalDevice)
    {
        this.vkInstance = vkInstance;
        this.vkPhysicalDevice = vkPhysicalDevice;
        this.vkLogicalDevice = vkLogicalDevice;
    }
}
