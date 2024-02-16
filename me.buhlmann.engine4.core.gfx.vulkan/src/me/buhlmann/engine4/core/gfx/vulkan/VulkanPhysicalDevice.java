package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanPhysicalDevice implements IVulkanNative<VkPhysicalDevice>, IVulkanDisposable
{
    private final VkPhysicalDevice device;
    private final VkPhysicalDeviceFeatures features;

    private final VkPhysicalDeviceMemoryProperties memoryProperties;
    private final VkPhysicalDeviceProperties deviceProperties;
    private final VkQueueFamilyProperties.Buffer familyProperties;
    private final VkExtensionProperties.Buffer extensionProperties;

    protected static PointerBuffer getPhysicalDevices(VulkanInstance instance, MemoryStack stack)
    {
        PointerBuffer pPhysicalDevices;
        // Get number of physical devices
        final IntBuffer buffer = stack.mallocInt(1);
        VulkanUtils.check(VK11.vkEnumeratePhysicalDevices(instance.getNative(), buffer, null), "Failed to get number of physical devices");
        int num = buffer.get(0);
        Engine4.getLogger().trace("Found " + num + " physical device(s)");

        // Populate physical devices list pointer
        pPhysicalDevices = stack.mallocPointer(num);
        VulkanUtils.check(VK11.vkEnumeratePhysicalDevices(instance.getNative(), buffer, pPhysicalDevices), "Failed to get physical devices");
        return pPhysicalDevices;
    }

    public static VulkanPhysicalDevice createPhysicalDevice(VulkanInstance instance, String preferred)
    {
        Engine4.getLogger().trace("Selecting physical devices");
        VulkanPhysicalDevice selected = null;
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            // Get available devices
            final PointerBuffer pPhysicalDevices = VulkanPhysicalDevice.getPhysicalDevices(instance, stack);
            int numDevices = pPhysicalDevices.capacity();
            if (numDevices <= 0)
            {
                throw new RuntimeException("No physical devices found");
            }

            // Populate available devices
            List<VulkanPhysicalDevice> devices = new ArrayList<>();
            for (int i = 0; i < numDevices; i++)
            {
                VkPhysicalDevice vkPhysicalDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), instance.getNative());
                VulkanPhysicalDevice device = new VulkanPhysicalDevice(vkPhysicalDevice);

                String name = device.getName();
                if (device.hasGraphicsQueueFamily() && device.hasKHRSwapChainExtension())
                {
                    Engine4.getLogger().trace("Device " + name + " supports required extensions.");
                    if (preferred != null && preferred.equals(name))
                    {
                        selected = device;
                        break;
                    }
                    devices.add(device);
                }
                else
                {
                    Engine4.getLogger().trace("Device " + name + " does not support required extensions");
                    device.dispose();
                }
            }

            selected = selected == null && !devices.isEmpty() ? devices.remove(0) : selected;

            devices.forEach(IVulkanDisposable::dispose);

            if (selected == null)
            {
                throw new RuntimeException("No suitable physical devices found.");
            }

            Engine4.getLogger().trace("Selected device: " + selected.getName());
        }

        return selected;
    }

    private boolean hasKHRSwapChainExtension()
    {
        boolean result = false;
        int number = this.extensionProperties != null ? this.extensionProperties.capacity() : 0;
        for (int i = 0; i < number; i++)
        {
            String name = this.extensionProperties.get(i).extensionNameString();
            if (KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(name))
            {
                result = true;
                break;
            }
        }

        return result;
    }

    private boolean hasGraphicsQueueFamily()
    {
        boolean result = false;
        int number = this.familyProperties != null ? this.familyProperties.capacity() : 0;
        for (int i = 0; i < number; i++)
        {
            VkQueueFamilyProperties familyProps = this.familyProperties.get(i);
            if ((familyProps.queueFlags() & VK11.VK_QUEUE_GRAPHICS_BIT) != 0)
            {
                result = true;
                break;
            }
        }

        return result;
    }

    public VkPhysicalDeviceMemoryProperties getMemoryProperties()
    {
        return this.memoryProperties;
    }

    @Override
    public VkPhysicalDevice getNative()
    {
        return this.device;
    }

    public VkPhysicalDeviceFeatures getDeviceFeatures()
    {
        return this.features;
    }

    public VkPhysicalDeviceProperties getDeviceProperties()
    {
        return this.deviceProperties;
    }

    public VkQueueFamilyProperties.Buffer getQueueFamilyProperties()
    {
        return this.familyProperties;
    }

    public String getName()
    {
        return this.deviceProperties.deviceNameString();
    }

    @Override
    public void dispose()
    {
        Engine4.getLogger().trace("Destroying physical device '" + this.deviceProperties.deviceNameString() + "'");
        this.memoryProperties.free();
        this.features.free();
        this.familyProperties.free();
        this.extensionProperties.free();
        this.deviceProperties.free();
    }

    public VulkanPhysicalDevice(final VkPhysicalDevice device)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            this.device = device;

            IntBuffer buffer = stack.mallocInt(1);

            this.deviceProperties = VkPhysicalDeviceProperties.calloc();
            VK11.vkGetPhysicalDeviceProperties(this.device, this.deviceProperties);

            VulkanUtils.check(VK11.vkEnumerateDeviceExtensionProperties(this.device, (String) null, buffer, null), "Failed to get number of device extension properties");
            this.extensionProperties = VkExtensionProperties.calloc(buffer.get(0));
            VulkanUtils.check(VK11.vkEnumerateDeviceExtensionProperties(this.device, (String) null, buffer, this.extensionProperties), "Failed to get extension properties");

            VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.device, buffer, null);
            this.familyProperties = VkQueueFamilyProperties.calloc(buffer.get(0));
            VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.device, buffer, this.familyProperties);

            this.features = VkPhysicalDeviceFeatures.calloc();
            VK11.vkGetPhysicalDeviceFeatures(this.device, this.features);

            this.memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
            VK11.vkGetPhysicalDeviceMemoryProperties(this.device, this.memoryProperties);
        }
    }
}
