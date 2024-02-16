package me.buhlmann.engine4.core.gfx.vulkan.context;

import me.buhlmann.engine4.core.gfx.vulkan.*;

public class VulkanRenderContext extends VulkanDeviceContext implements IVulkanDisposable
{
    private final VulkanSurface vkSurface;
    private VulkanSwapChain vkSwapChain;
    private VulkanQueue.VulkanGraphicsQueue vkGraphicsQueue;
    private VulkanQueue.VulkanPresentQueue vkPresentQueue;
    private VulkanCommandPool vkCommandPool;

    public void setGraphicsQueue(final VulkanQueue.VulkanGraphicsQueue vkGraphicsQueue)
    {
        this.vkGraphicsQueue = vkGraphicsQueue;
        this.vkCommandPool = new VulkanCommandPool(this, this.vkGraphicsQueue.getIndex());
    }

    public void setPresentQueue(final VulkanQueue.VulkanPresentQueue vkPresentQueue)
    {
        this.vkPresentQueue = vkPresentQueue;
    }

    public VulkanSurface getSurface()
    {
        return this.vkSurface;
    }

    public void setSwapChain(final VulkanSwapChain vkSwapChain)
    {
        this.vkSwapChain = vkSwapChain;
    }

    public VulkanSwapChain getSwapChain()
    {
        return this.vkSwapChain;
    }

    public VulkanQueue.VulkanGraphicsQueue getGraphicsQueue()
    {
        return this.vkGraphicsQueue;
    }

    public VulkanQueue.VulkanPresentQueue getPresentQueue()
    {
        return this.vkPresentQueue;
    }

    public VulkanCommandPool getCommandPool()
    {
        return this.vkCommandPool;
    }

    @Override
    public void dispose()
    {
        this.vkCommandPool.dispose();
        this.vkSwapChain.dispose();
        this.vkPresentQueue.dispose();
        this.vkGraphicsQueue.dispose();
        this.vkSurface.dispose();
        super.dispose();
    }

    public VulkanRenderContext(final VulkanRenderContext vkContext, final VulkanSwapChain vkSwapChain)
    {
        super(vkContext.getInstance(), vkContext.getPhysicalDevice(), vkContext.getLogicalDevice());
        this.vkSurface = vkContext.vkSurface;
        this.vkGraphicsQueue = vkContext.vkGraphicsQueue;
        this.vkPresentQueue = vkContext.vkPresentQueue;
        this.vkSwapChain = vkSwapChain;
        this.vkCommandPool = vkContext.vkCommandPool;
    }

    public VulkanRenderContext(final VulkanInstance vkInstance,
                               final VulkanPhysicalDevice vkPhysicalDevice,
                               final VulkanLogicalDevice vkLogicalDevice,
                               final VulkanSurface vkSurface,
                               final VulkanSwapChain vkSwapChain)
    {
        super(vkInstance, vkPhysicalDevice, vkLogicalDevice);
        this.vkSurface = vkSurface;
        this.vkSwapChain = vkSwapChain;
        // Bad to pass this out of a constructor...
    }
}
