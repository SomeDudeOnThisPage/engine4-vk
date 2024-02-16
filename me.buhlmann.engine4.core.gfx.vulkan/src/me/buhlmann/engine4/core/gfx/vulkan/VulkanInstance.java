package me.buhlmann.engine4.core.gfx.vulkan;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanInstance implements IVulkanNative<VkInstance>, IVulkanDisposable
{
    public static final int MESSAGE_SEVERITY_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT |
        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
    public static final int MESSAGE_TYPE_BITMASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;

    private final VkInstance vkInstance;

    private VkDebugUtilsMessengerCreateInfoEXT debugUtils;
    private long vkDebugHandle;

    @Override
    public VkInstance getNative()
    {
        return this.vkInstance;
    }

    private List<String> getValidationLayers()
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final IntBuffer amount = stack.callocInt(1);
            VK11.vkEnumerateInstanceLayerProperties(amount, null);
            Engine4.getLogger().trace("Instance supports " + amount.get(0) + " layers.");

            final VkLayerProperties.Buffer props = VkLayerProperties.callocStack(amount.get(0), stack);
            VK11.vkEnumerateInstanceLayerProperties(amount, props);

            final List<String> supported = new ArrayList<>();
            for (int i = 0; i < amount.get(0); i++)
            {
                String name = props.get(i).layerNameString();
                supported.add(name);
                Engine4.getLogger().trace("Supported layer: '" + name + "'.");
            }

            final List<String> layers = new ArrayList<>();

            if (supported.contains("VK_LAYER_KHRONOS_validation"))
            {
                layers.add("VK_LAYER_KHRONOS_validation");
                return layers;
            }

            if (supported.contains("VK_LAYER_LUNARG_standard_validation"))
            {
                layers.add("VK_LAYER_LUNARG_standard_validation");
                return layers;
            }

            final List<String> requested = new ArrayList<>();
            requested.add("VK_LAYER_GOOGLE_threading");
            requested.add("VK_LAYER_LUNARG_parameter_validation");
            requested.add("VK_LAYER_LUNARG_object_tracker");
            requested.add("VK_LAYER_LUNARG_core_validation");
            requested.add("VK_LAYER_GOOGLE_unique_objects");

            return requested.stream().filter(supported::contains).toList();
        }
    }

    private static VkDebugUtilsMessengerCreateInfoEXT createDebugCallBack()
    {
        return VkDebugUtilsMessengerCreateInfoEXT
            .calloc()
            .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
            .messageSeverity(VulkanInstance.MESSAGE_SEVERITY_BITMASK)
            .messageType(VulkanInstance.MESSAGE_TYPE_BITMASK)
            .pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0)
                {
                    Engine4.getLogger().info("VkDebugUtilsCallback, " + callbackData.pMessageString());
                }
                else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0)
                {
                    Engine4.getLogger().warning("VkDebugUtilsCallback, " + callbackData.pMessageString());
                }
                else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0)
                {
                    Engine4.getLogger().error("VkDebugUtilsCallback, " + callbackData.pMessageString());
                }
                else
                {
                    Engine4.getLogger().trace("VkDebugUtilsCallback, " + callbackData.pMessageString());
                }
                return VK11.VK_FALSE;
            });
    }

    public void dispose()
    {
        if (this.vkDebugHandle != VK11.VK_NULL_HANDLE)
        {
            EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(this.vkInstance, this.vkDebugHandle, null);
        }

        if (this.debugUtils != null)
        {
            this.debugUtils.pfnUserCallback().free();
            this.debugUtils.free();
        }

        VK11.vkDestroyInstance(this.vkInstance, null);
    }

    public VulkanInstance(final boolean validate)
    {
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final ByteBuffer name = stack.UTF8("Engine4");
            final VkApplicationInfo info = VkApplicationInfo.mallocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(name)
                .applicationVersion(1)
                .pEngineName(name)
                .engineVersion(0)
                .apiVersion(VK11.VK_API_VERSION_1_1)
                .pNext(MemoryUtil.NULL);

            final List<String> validationLayers = this.getValidationLayers();
            final int numValidationLayers = validationLayers.size();
            boolean supportsValidation = validate;
            if (validate && numValidationLayers == 0)
            {
                supportsValidation = false;
                Engine4.getLogger().warning("No supported validation layers found.");
            }

            PointerBuffer required = null;
            if (supportsValidation)
            {
                required = stack.mallocPointer(numValidationLayers);
                for (int i = 0; i < numValidationLayers; i++)
                {
                    Engine4.getLogger().trace("Using validation layer " + validationLayers.get(i));
                    required.put(i, stack.ASCII(validationLayers.get(i)));
                }
            }

            final PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null)
            {
                throw new RuntimeException("Failed to find the GLFW platform surface extensions");
            }

            PointerBuffer requiredExtensions;
            if (supportsValidation)
            {
                ByteBuffer vkDebugUtilsExtension = stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
                requiredExtensions = stack.mallocPointer(glfwExtensions.remaining() + 1);
                requiredExtensions.put(glfwExtensions).put(vkDebugUtilsExtension);
            }
            else
            {
                requiredExtensions = stack.mallocPointer(glfwExtensions.remaining());
                requiredExtensions.put(glfwExtensions);
            }
            requiredExtensions.flip();

            long extension = MemoryUtil.NULL;
            if (supportsValidation)
            {
                this.debugUtils = VulkanInstance.createDebugCallBack();
                extension = this.debugUtils.address();
            }

            VkInstanceCreateInfo instanceInfo = VkInstanceCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(extension)
                .pApplicationInfo(info)
                .ppEnabledLayerNames(required)
                .ppEnabledExtensionNames(requiredExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            VulkanUtils.check(VK11.vkCreateInstance(instanceInfo, null, pInstance), "Error creating instance");
            this.vkInstance = new VkInstance(pInstance.get(0), instanceInfo);

            this.vkDebugHandle = VK11.VK_NULL_HANDLE;
            if (supportsValidation)
            {
                final LongBuffer longBuff = stack.mallocLong(1);
                VulkanUtils.check(EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(this.vkInstance, this.debugUtils, null, longBuff), "Error creating debug utils");
                this.vkDebugHandle = longBuff.get(0);
            }
        }

        Engine4.getLogger().trace("Created Vulkan instance.");
    }
}
