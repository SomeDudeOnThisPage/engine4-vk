package me.buhlmann.engine4.core.gfx.vulkan.renderer;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.gfx.material.IShaderProgram;
import me.buhlmann.engine4.core.gfx.vulkan.IVulkanDisposable;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanBufferedUniform;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanShaderCompiler;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;

public class VulkanShaderProgram extends AbstractVulkanAsset implements IShaderProgram, IVulkanDisposable
{
    public record ShaderModule(int stage, long handle) {}
    public record ShaderModuleData(int stage, String file) {}

    private final VulkanDeviceContext vkContext;
    private final ShaderModule[] modules;
    private VulkanBufferedUniform<?>[] vkMaterialUniforms;

    private long createShaderModule(byte[] code)
    {
        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final ByteBuffer pointer = stack.malloc(code.length).put(0, code);

            final VkShaderModuleCreateInfo vkShaderModuleCreateInfo = VkShaderModuleCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(pointer);

            final LongBuffer lp = stack.mallocLong(1);
            // vkCreateShaderModule can be called from multiple threads at once safely!
            VulkanUtils.check(VK11.vkCreateShaderModule(this.vkContext.getLogicalDevice().getNative(), vkShaderModuleCreateInfo, null, lp), "Failed to create shader module");

            return lp.get(0);
        }
    }

    public VulkanBufferedUniform<?>[] getMaterialUniforms()
    {
        return this.vkMaterialUniforms;
    }

    public VulkanShaderProgram.ShaderModule[] getModules()
    {
        return this.modules;
    }

    @Override
    public void dispose()
    {
        for (final ShaderModule module : this.modules)
        {
            VK11.vkDestroyShaderModule(this.vkContext.getLogicalDevice().getNative(), module.handle(), null);
        }
    }

    public VulkanShaderProgram(final VulkanDeviceContext vkContext, final ShaderModuleData[] shaderModuleData)
    {
        try
        {
            this.vkContext = vkContext;
            int numModules = shaderModuleData != null ? shaderModuleData.length : 0;
            this.modules = new ShaderModule[numModules];

            for (int i = 0; i < numModules; i++)
            {
                final VulkanShaderCompiler.ShaderCompilationResult result = VulkanShaderCompiler.compile(vkContext, shaderModuleData[i].file(), shaderModuleData[i].stage());
                final byte[] moduleContents = Files.readAllBytes(new File(result.file()).toPath());
                final long moduleHandle = this.createShaderModule(moduleContents);
                this.modules[i] = new ShaderModule(shaderModuleData[i].stage(), moduleHandle);
                this.vkMaterialUniforms = result.vkMaterialUniforms();
            }
        }
        catch (IOException exception)
        {
            Engine4.getLogger().error("Error reading shader file");
            throw new RuntimeException(exception);
        }
    }

}
