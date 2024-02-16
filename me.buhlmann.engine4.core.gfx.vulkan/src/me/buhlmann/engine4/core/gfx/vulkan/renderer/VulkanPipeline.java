package me.buhlmann.engine4.core.gfx.vulkan.renderer;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.*;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanDescriptorSetLayout;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public class VulkanPipeline implements IVulkanPointer, IVulkanDisposable
{
    public record CreationInfo(long vkRenderPass,
                               VulkanShaderProgram shaderProgram,
                               int numColorAttachments,
                               VulkanVertexInputStateInfo viInputStateInfo,
                               int pushConstantSize,
                               boolean hasDepthAttachment,
                               VulkanDescriptorSetLayout... descriptorSetLayouts
    ) implements IVulkanDisposable
    {
        @Override
        public void dispose()
        {
            this.viInputStateInfo.dispose();
        }
    }

    private final long pointer;
    private final long vkPipelineLayout;

    private final VulkanDeviceContext vkContext;

    @Override
    public long getPointer()
    {
        return this.pointer;
    }

    @Override
    public void dispose()
    {
        VK11.vkDestroyPipelineLayout(this.vkContext.getLogicalDevice().getNative(), this.vkPipelineLayout, null);
        VK11.vkDestroyPipeline(this.vkContext.getLogicalDevice().getNative(), this.pointer, null);
    }

    public long getPipelineLayout()
    {
        return this.vkPipelineLayout;
    }

    public VulkanPipeline(VulkanDeviceContext vkContext, VulkanPipelineCache pipelineCache, CreationInfo vkPipelineCreationInfo)
    {
        this.vkContext = vkContext;

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final LongBuffer pointer = stack.mallocLong(1);
            final ByteBuffer main = stack.UTF8("main");

            VulkanShaderProgram.ShaderModule[] shaderModules = vkPipelineCreationInfo.shaderProgram.getModules();
            final VkPipelineShaderStageCreateInfo.Buffer vkPipelineShaderStageCreateInfo = VkPipelineShaderStageCreateInfo.callocStack(shaderModules.length, stack);
            for (int i = 0; i < shaderModules.length; i++)
            {
                vkPipelineShaderStageCreateInfo.get(i)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(shaderModules[i].stage())
                    .module(shaderModules[i].handle())
                    .pName(main);
            }

            final VkPipelineInputAssemblyStateCreateInfo vkPipelineInputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK11.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

            final VkPipelineViewportStateCreateInfo vkPipelineViewportStateCreateInfo = VkPipelineViewportStateCreateInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1)
                    .scissorCount(1);

            final VkPipelineRasterizationStateCreateInfo vkPipelineRasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .polygonMode(VK11.VK_POLYGON_MODE_FILL)
                    .cullMode(VK11.VK_CULL_MODE_NONE)
                    .frontFace(VK11.VK_FRONT_FACE_CLOCKWISE)
                    .lineWidth(1.0f);

            final VkPipelineMultisampleStateCreateInfo vkPipelineMultisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(VK11.VK_SAMPLE_COUNT_1_BIT);

            final VkPipelineColorBlendAttachmentState.Buffer vkPipelineColorBlendAttachmentState = VkPipelineColorBlendAttachmentState.callocStack(vkPipelineCreationInfo.numColorAttachments(), stack);
            for (int i = 0; i < vkPipelineCreationInfo.numColorAttachments(); i++)
            {
                vkPipelineColorBlendAttachmentState.get(i).colorWriteMask(
                    VK11.VK_COLOR_COMPONENT_R_BIT |
                    VK11.VK_COLOR_COMPONENT_G_BIT |
                    VK11.VK_COLOR_COMPONENT_B_BIT |
                    VK11.VK_COLOR_COMPONENT_A_BIT
                );
            }

            final VkPipelineColorBlendStateCreateInfo vkPipelineColorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .pAttachments(vkPipelineColorBlendAttachmentState);

            final VkPipelineDepthStencilStateCreateInfo vkPipelineDepthStencilStateCreateInfo = vkPipelineCreationInfo.hasDepthAttachment()
                ? VkPipelineDepthStencilStateCreateInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(true)
                    .depthWriteEnable(true)
                    .depthCompareOp(VK11.VK_COMPARE_OP_LESS_OR_EQUAL)
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false)
                : null;

            final VkPipelineDynamicStateCreateInfo vkPipelineDynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.callocStack(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(stack.ints(
                        VK11.VK_DYNAMIC_STATE_VIEWPORT,
                        VK11.VK_DYNAMIC_STATE_SCISSOR
                    ));

            final VkPushConstantRange.Buffer vkPushConstantRange = vkPipelineCreationInfo.pushConstantSize() > 0
                ? VkPushConstantRange.callocStack(1, stack)
                    .stageFlags(VK11.VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(0)
                    .size(vkPipelineCreationInfo.pushConstantSize())
                : null;

            final VulkanDescriptorSetLayout[] descriptorSetLayouts = vkPipelineCreationInfo.descriptorSetLayouts();
            final int numLayouts = descriptorSetLayouts != null ? descriptorSetLayouts.length : 0;
            final LongBuffer ppLayout = stack.mallocLong(numLayouts);

            for (int i = 0; i < numLayouts; i++)
            {
                ppLayout.put(i, descriptorSetLayouts[i].getPointer());
            }

            final VkPipelineLayoutCreateInfo vkPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(ppLayout)
                .pPushConstantRanges(vkPushConstantRange);

            VulkanUtils.check(VK11.vkCreatePipelineLayout(this.vkContext.getLogicalDevice().getNative(), vkPipelineLayoutCreateInfo, null, pointer), "Failed to create pipeline layout");
            this.vkPipelineLayout = pointer.get(0);

            final VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.callocStack(1, stack)
                .sType(VK11.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .pStages(vkPipelineShaderStageCreateInfo)
                .pVertexInputState(vkPipelineCreationInfo.viInputStateInfo().getNative())
                .pInputAssemblyState(vkPipelineInputAssemblyStateCreateInfo)
                .pViewportState(vkPipelineViewportStateCreateInfo)
                .pRasterizationState(vkPipelineRasterizationStateCreateInfo)
                .pMultisampleState(vkPipelineMultisampleStateCreateInfo)
                .pColorBlendState(vkPipelineColorBlendStateCreateInfo)
                .pDynamicState(vkPipelineDynamicStateCreateInfo)
                .layout(this.vkPipelineLayout)
                .renderPass(vkPipelineCreationInfo.vkRenderPass);

            if (vkPipelineDepthStencilStateCreateInfo != null)
            {
                pipeline.pDepthStencilState(vkPipelineDepthStencilStateCreateInfo);
            }

            VulkanUtils.check(VK11.vkCreateGraphicsPipelines(this.vkContext.getLogicalDevice().getNative(), pipelineCache.getPointer(), pipeline, null, pointer), "Error creating graphics pipeline");
            this.pointer = pointer.get(0);

            Engine4.getLogger().trace(String.format("[VULKAN] created pipeline %dl", this.pointer));

            // TODO: Actually reuse pipeline caches...
            pipelineCache.dispose();
        }
    }
}
