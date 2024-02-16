package me.buhlmann.engine4.core.gfx.vulkan.asset;

import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanPipeline;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanShaderProgram;

public class VulkanMaterialType
{
    private record Stage(VulkanPipeline vkPipeline, VulkanShaderProgram... vkShaderPrograms) {}

    private Stage geometry;
    private Stage lighting;
}
