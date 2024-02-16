package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import me.buhlmann.engine4.platform.window.GLFWWindow;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

public class VulkanSwapChain implements IVulkanPointer, IVulkanDisposable
{
    public record VulkanSurfaceFormat(int format, int color) {}
    public record VulkanSyncSemaphores(VulkanSemaphore phase, VulkanSemaphore acquisition, VulkanSemaphore completion) implements IVulkanDisposable
    {
        public VulkanSyncSemaphores(VulkanLogicalDevice device)
        {
            this(new VulkanSemaphore(device), new VulkanSemaphore(device), new VulkanSemaphore(device));
        }

        @Override
        public void dispose()
        {
            this.acquisition.dispose();
            this.completion.dispose();
            this.phase.dispose();
        }
    }

    public record VulkanSwapChainSegment(
        VulkanImageView vkImageView,
        VulkanSyncSemaphores vkSyncSemaphores
    ) implements IVulkanDisposable {
        @Override
        public void dispose()
        {
            this.vkImageView.dispose();
            this.vkSyncSemaphores.dispose();
        }
    }

    private final long id;
    private final VulkanLogicalDevice vkLogicalDevice;

    private final VulkanSurfaceFormat format;
    private final VkExtent2D extent;

    private final VulkanSwapChainSegment[] segments;
    private int frame;

    private int getImageCount(final VkSurfaceCapabilitiesKHR surfCapabilities, final int requested)
    {
        final int max = surfCapabilities.maxImageCount();
        final int min = surfCapabilities.minImageCount();
        int result = min;
        if (max != 0)
        {
            result = Math.min(requested, max);
        }

        result = Math.max(result, min);

        Engine4.getLogger().trace(String.format("[VULKAN] Requested %d images, got %d images. Surface capabilities, maxImages: %d, minImages %d",
            requested,
            result,
            max,
            min
        ));

        return result;
    }

    private VulkanSurfaceFormat getSurfaceFormat(VulkanPhysicalDevice physicalDevice, VulkanSurface surface)
    {
        int imageFormat;
        int colorSpace;
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final IntBuffer ip = stack.mallocInt(1);
            VulkanUtils.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getNative(), surface.getPointer(), ip, null),
                "Failed to get the number surface formats");

            int numFormats = ip.get(0);
            if (numFormats <= 0)
            {
                throw new RuntimeException("No surface formats retrieved");
            }

            VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.callocStack(numFormats, stack);
            VulkanUtils.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getNative(),
                surface.getPointer(), ip, surfaceFormats), "Failed to get surface formats");

            imageFormat = VK11.VK_FORMAT_B8G8R8A8_SRGB;
            colorSpace = surfaceFormats.get(0).colorSpace();
            for (int i = 0; i < numFormats; i++)
            {
                final VkSurfaceFormatKHR surfaceFormatKHR = surfaceFormats.get(i);
                if (surfaceFormatKHR.format() == VK11.VK_FORMAT_B8G8R8A8_SRGB && surfaceFormatKHR.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                {
                    imageFormat = surfaceFormatKHR.format();
                    colorSpace = surfaceFormatKHR.colorSpace();
                    break;
                }
            }
        }
        return new VulkanSurfaceFormat(imageFormat, colorSpace);
    }

    @Override
    public long getPointer()
    {
        return this.id;
    }

    public VulkanSyncSemaphores getSemaphores(int index)
    {
        return this.segments[index].vkSyncSemaphores();
    }

    public int getCurrentFrame()
    {
        return this.frame;
    }

    public VkExtent2D getSwapChainExtent()
    {
        return this.extent;
    }

    public VulkanImageView[] getImageViews()
    {
        final VulkanImageView[] views = new VulkanImageView[this.getSize()];
        for (int i = 0; i < this.getSize(); i++)
        {
            views[i] = this.segments[i].vkImageView();
        }
        return views;
    }

    public VulkanSurfaceFormat getSurfaceFormat()
    {
        return this.format;
    }

    public int getSize()
    {
        return this.segments.length;
    }

    private VkExtent2D getSwapChainExtent(GLFWWindow window, VkSurfaceCapabilitiesKHR surfCapabilities)
    {
        final VkExtent2D result = VkExtent2D.calloc();
        if (surfCapabilities.currentExtent().width() == 0xFFFFFFFF)
        {
            // Surface size undefined. Set to the window size if within bounds
            int width = Math.min(window.getSize().x, surfCapabilities.maxImageExtent().width());
            width = Math.max(width, surfCapabilities.minImageExtent().width());

            int height = Math.min(window.getSize().y, surfCapabilities.maxImageExtent().height());
            height = Math.max(height, surfCapabilities.minImageExtent().height());

            result.width(width);
            result.height(height);
        }
        else
        {
            result.set(surfCapabilities.currentExtent());
        }

        return result;
    }

    private VulkanImageView[] createImageViews(final MemoryStack stack, final VulkanLogicalDevice device, final VkSurfaceCapabilitiesKHR capabilities, int format)
    {
        final VulkanImageView[] result;

        final IntBuffer ip = stack.mallocInt(1);
        VulkanUtils.check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getNative(), this.id, ip, null),
            "Failed to get number of surface images");
        int numImages = ip.get(0);

        final LongBuffer swapChainImages = stack.mallocLong(numImages);
        VulkanUtils.check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getNative(), this.id, ip, swapChainImages),
            "Failed to get surface images");

        result = new VulkanImageView[numImages];
        VulkanImageView.Data imageViewData = new VulkanImageView.Data().format(format).aspectMask(VK11.VK_IMAGE_ASPECT_COLOR_BIT);
        for (int i = 0; i < numImages; i++)
        {
            result[i] = new VulkanImageView(device, swapChainImages.get(i), imageViewData);
        }

        return result;
    }

    public boolean present(final VulkanQueue queue)
    {
        boolean resize = false;
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkPresentInfoKHR present = VkPresentInfoKHR.callocStack(stack)
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.longs(this.segments[this.frame].vkSyncSemaphores().completion().getPointer()))
                .swapchainCount(1)
                .pSwapchains(stack.longs(this.id))
                .pImageIndices(stack.ints(this.frame));

            int err = KHRSwapchain.vkQueuePresentKHR(queue.getNative(), present);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR /* || err == KHRSwapchain.VK_SUBOPTIMAL_KHR */ )
            {
                resize = true;
            }
            else if (err != VK11.VK_SUCCESS)
            {
                throw new RuntimeException("Failed to present KHR: " + err);
            }
        }
        this.frame = (this.frame + 1) % this.getSize();
        return resize;
    }

    public boolean next()
    {
        boolean resize = false;
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final IntBuffer ip = stack.mallocInt(1);
            final int err = KHRSwapchain.vkAcquireNextImageKHR(this.vkLogicalDevice.getNative(), this.id, ~0L, this.segments[this.frame].vkSyncSemaphores().acquisition().getPointer(), MemoryUtil.NULL, ip);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR /* || err == KHRSwapchain.VK_SUBOPTIMAL_KHR */ )
            {
                resize = true;
            }
            else if (err != VK11.VK_SUCCESS)
            {
                throw new RuntimeException("Failed to acquire image: " + err);
            }
            this.frame = ip.get(0);
        }

        return resize;
    }

    @Override
    public void dispose()
    {
        Engine4.getLogger().trace("[VULKAN] destroying swap chain " + this.getPointer());
        this.extent.close();
        Arrays.asList(this.segments).forEach(VulkanSwapChainSegment::dispose);;
        KHRSwapchain.vkDestroySwapchainKHR(this.vkLogicalDevice.getNative(), this.id, null);
    }

    public VulkanSwapChain(final VulkanLogicalDevice vkLogicalDevice, final VulkanPhysicalDevice vkPhysicalDevice, final VulkanSurface surface, final int images, final boolean vsync)
    {
        this.vkLogicalDevice = vkLogicalDevice;
        this.frame = 0;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.callocStack(stack);
            VulkanUtils.check(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(vkPhysicalDevice.getNative(), surface.getPointer(), capabilities), "Failed to get surface capabilities");

            this.format = this.getSurfaceFormat(vkPhysicalDevice, surface);
            this.extent = this.getSwapChainExtent(Engine4.getWindow(), capabilities);

            final int numImages = this.getImageCount(capabilities, images);
            this.segments = new VulkanSwapChainSegment[numImages];

            final VkSwapchainCreateInfoKHR vkSwapchainCreateInfo = VkSwapchainCreateInfoKHR.callocStack(stack)
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(surface.getPointer())
                .minImageCount(numImages)
                .imageFormat(this.format.format())
                .imageColorSpace(this.format.color())
                .imageExtent(this.extent)
                .imageArrayLayers(1)
                .imageUsage(VK11.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .imageSharingMode(VK11.VK_SHARING_MODE_EXCLUSIVE)
                .preTransform(capabilities.currentTransform())
                .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .clipped(true);

            if (vsync)
            {
                vkSwapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
            }
            else
            {
                vkSwapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);
            }

            final LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.check(KHRSwapchain.vkCreateSwapchainKHR(this.vkLogicalDevice.getNative(), vkSwapchainCreateInfo, null, lp),
                "Failed to create swap chain");
            this.id = lp.get(0);

            final VulkanImageView[] views = this.createImageViews(stack, this.vkLogicalDevice, capabilities, this.format.format);
            for (int i = 0; i < numImages; i++)
            {
                this.segments[i] = new VulkanSwapChainSegment(
                    views[i],
                    new VulkanSyncSemaphores(this.vkLogicalDevice)
                );
            }
        }
    }
}
