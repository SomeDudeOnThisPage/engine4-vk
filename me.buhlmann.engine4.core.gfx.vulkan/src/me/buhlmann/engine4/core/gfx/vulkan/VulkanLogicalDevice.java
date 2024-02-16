package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;

public class VulkanLogicalDevice implements IVulkanNative<VkDevice>, IVulkanDisposable
{
    private final VkDevice device;
    private final VulkanPhysicalDevice physical;

    private final boolean anisotropy;

    @Override
    public void dispose()
    {
        VK11.vkDestroyDevice(this.device, null);
    }

    public boolean getSupportAnisotropy()
    {
        return this.anisotropy;
    }

    public VulkanPhysicalDevice getPhysicalDevice()
    {
        return this.physical;
    }

    @Override
    public VkDevice getNative()
    {
        return this.device;
    }

    public void idle()
    {
        VK11.vkDeviceWaitIdle(this.device);
    }

    public VulkanLogicalDevice(VulkanPhysicalDevice physical)
    {
        this.physical = physical;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            PointerBuffer required = stack.mallocPointer(1);
            required.put(0, stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.callocStack(stack);
            this.anisotropy = features.samplerAnisotropy();
            if (this.anisotropy)
            {
                features.samplerAnisotropy(true);
            }

            VkQueueFamilyProperties.Buffer queuePropsBuff = physical.getQueueFamilyProperties();
            int number = queuePropsBuff.capacity();
            VkDeviceQueueCreateInfo.Buffer queueCreationInfoBuf = VkDeviceQueueCreateInfo.callocStack(number, stack);
            for (int i = 0; i < number; i++)
            {
                FloatBuffer priorities = stack.callocFloat(queuePropsBuff.get(i).queueCount());
                queueCreationInfoBuf.get(i)
                    .sType(VK11.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(i)
                    .pQueuePriorities(priorities);
            }

            VkDeviceCreateInfo deviceCreationInfo = VkDeviceCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .ppEnabledExtensionNames(required)
                .pEnabledFeatures(features)
                .pQueueCreateInfos(queueCreationInfoBuf);

            PointerBuffer pp = stack.mallocPointer(1);
            VulkanUtils.check(VK11.vkCreateDevice(physical.getNative(), deviceCreationInfo, null, pp), "Failed to create logical device");
            this.device = new VkDevice(pp.get(0), physical.getNative(), deviceCreationInfo);
            Engine4.getLogger().trace("Created logical device.");
        }
    }
}
