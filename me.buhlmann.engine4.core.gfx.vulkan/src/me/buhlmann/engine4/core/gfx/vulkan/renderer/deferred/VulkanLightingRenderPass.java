package me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred;

import me.buhlmann.engine4.api.entity.IEntityCollection;
import me.buhlmann.engine4.api.gfx.texture.ITexture;
import me.buhlmann.engine4.api.renderer.IRenderer;
import me.buhlmann.engine4.core.gfx.vulkan.*;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.*;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTexture2D;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanTextureSampler;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.IVulkanRenderPass;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanPipeline;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanPipelineCache;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.VulkanShaderProgram;
import me.buhlmann.engine4.factory.AssetFactory;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanLightingRenderPass implements IVulkanRenderPass
{
    private record Frame(
        VulkanFramebuffer vkFramebuffer,
        VulkanCommandBuffer vkCommandBuffer,
        VulkanFence vkFence
    ) implements IVulkanDisposable {
        @Override
        public void dispose()
        {
            this.vkFence.hold();
            this.vkFramebuffer.dispose();
            this.vkFence.dispose();
            this.vkCommandBuffer.dispose();
        }
    }

    private final VulkanRenderContext vkContext;
    private final VulkanSwapChainRenderPass vkRenderPass;
    private final VulkanGBuffer vkGBuffer;
    private final VulkanPipeline vkPipeline;
    private final VulkanShaderProgram vkShaderProgram;
    private final VulkanUniformBuffer vkUniformBuffer0;

    private final List<Frame> vkFrames;

    public VulkanLightingRenderPass(final VulkanRenderContext vkContext, final VulkanGBuffer vkGBuffer)
    {
        this.vkContext = vkContext;
        this.vkGBuffer = vkGBuffer;
        this.vkRenderPass = new VulkanSwapChainRenderPass(this.vkContext, -1);

        // TODO: Custom lighting shaders...
        this.vkShaderProgram = new VulkanShaderProgram(vkContext, new VulkanShaderProgram.ShaderModuleData[]
        {
            new VulkanShaderProgram.ShaderModuleData(VK11.VK_SHADER_STAGE_VERTEX_BIT, "shaders/core-deferred/lighting.vs"),
            new VulkanShaderProgram.ShaderModuleData(VK11.VK_SHADER_STAGE_FRAGMENT_BIT, "shaders/core-deferred/lighting.fs")
        });

        final ITexture vkAttachment0 = new VulkanTexture2D(
            vkGBuffer.getAttachments().get(0).getImage(),
            vkGBuffer.getAttachments().get(0).getImageView(),
            new VulkanTextureSampler(this.vkContext, 1, false)
        );

        final ITexture vkAttachment1 = new VulkanTexture2D(
            vkGBuffer.getAttachments().get(1).getImage(),
            vkGBuffer.getAttachments().get(1).getImageView(),
            new VulkanTextureSampler(this.vkContext, 1, false)
        );

        final ITexture vkAttachment2 = new VulkanTexture2D(
            vkGBuffer.getAttachments().get(2).getImage(),
            vkGBuffer.getAttachments().get(2).getImageView(),
            new VulkanTextureSampler(this.vkContext, 1, false)
        );

        this.vkUniformBuffer0 = VulkanUniformBuffer.create(this.vkContext, 0,
            new VulkanBufferedUniformArray<>("light", 1,
                new VulkanBufferedUniformStruct<>(
                    new VulkanBufferedUniform<>("pos", new Vector3f(-100.0f, -10.0f, 0.0f)),
                    new VulkanBufferedUniform<>("col", new Vector3f()),
                    new VulkanBufferedUniform<>("clq", new Vector3f())
                )
            ),
            new VulkanBufferedUniform<>("amount", 1),
            new VulkanUniformSampler(1, AssetFactory.wrap("vkAttachment0", ITexture.class, vkAttachment0)),
            new VulkanUniformSampler(2, AssetFactory.wrap("vkAttachment0", ITexture.class, vkAttachment1)),
            new VulkanUniformSampler(3, AssetFactory.wrap("vkAttachment0", ITexture.class, vkAttachment2))
        );

        this.vkUniformBuffer0.set("light[0].pos", new Vector3f(-100.0f, -10.0f, 0.0f));
        this.vkUniformBuffer0.set("light[0].col", new Vector3f(1.0f, 0.0f, 0.0f));
        this.vkUniformBuffer0.set("amount", 1);

        final VulkanPipeline.CreationInfo vkPipelineCreationInfo = new VulkanPipeline.CreationInfo(
            this.vkRenderPass.getPointer(),
            this.vkShaderProgram,
            1,
            new VulkanVertexBufferStructure.Empty(),
            0,
            false,
            this.vkUniformBuffer0.getDescriptorSetLayouts()
        );

        this.vkPipeline = new VulkanPipeline(vkContext, new VulkanPipelineCache(this.vkContext), vkPipelineCreationInfo);

        this.vkFrames = new ArrayList<>(vkContext.getSwapChain().getSize());
        for (int i = 0; i < vkContext.getSwapChain().getSize(); i++)
        {
            this.vkFrames.add(this.createFrame(vkContext, i));
        }

        for (int i = 0; i < this.vkFrames.size(); i++)
        {
            this.preRecordCommandBuffer(i);
        }
    }

    private Frame createFrame(final VulkanRenderContext vkContext, int index)
    {
        final VulkanFramebuffer vkFramebuffer = this.createFramebuffer(
            vkContext,
            index
        );

        final VulkanCommandBuffer vkCommandBuffer = new VulkanCommandBuffer(vkContext, true, false);
        final VulkanFence vkFence = new VulkanFence(vkContext.getLogicalDevice(), true);
        return new Frame(vkFramebuffer, vkCommandBuffer, vkFence);
    }

    private VulkanFramebuffer createFramebuffer(final VulkanRenderContext vkContext, final int frame)
    {
        final VkExtent2D vkSwapChainExtent = vkContext.getSwapChain().getSwapChainExtent();

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final LongBuffer pointers = stack.mallocLong(1);
            pointers.put(0, vkContext.getSwapChain().getImageViews()[frame].getPointer());
            return new VulkanFramebuffer(
                vkContext.getLogicalDevice(),
                vkSwapChainExtent.width(),
                vkSwapChainExtent.height(),
                pointers,
                this.vkRenderPass.getPointer()
            );
        }
    }

    @Override
    public void render(IRenderer.Input input, VulkanRenderContext vkContext)
    {
        this.prepareCommandBuffer();
        this.submit(vkContext);
    }

    public void submit(final VulkanRenderContext vkContext)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final VulkanLightingRenderPass.Frame frame = this.vkFrames.get(vkContext.getSwapChain().getCurrentFrame());
            VulkanSwapChain.VulkanSyncSemaphores semaphores = vkContext.getSwapChain().getSemaphores(vkContext.getSwapChain().getCurrentFrame());
            vkContext.getGraphicsQueue().submit(
                stack.pointers(frame.vkCommandBuffer.getNative()),
                stack.longs(semaphores.phase().getPointer()),
                stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                stack.longs(semaphores.completion().getPointer()),
                frame.vkFence
            );
        }
    }

    public void prepareCommandBuffer()
    {
        final Frame vkFrame = this.vkFrames.get(this.vkContext.getSwapChain().getCurrentFrame());
        this.vkUniformBuffer0.set("amount", 1);

        vkFrame.vkFence.hold();
        vkFrame.vkFence.reset();
    }

    public void preRecordCommandBuffer(int index)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final Frame vkFrame = this.vkFrames.get(index);
            VkExtent2D swapChainExtent = this.vkContext.getSwapChain().getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();

            VulkanFramebuffer vkFramebuffer = vkFrame.vkFramebuffer();
            VulkanCommandBuffer vkCommandBuffer = vkFrame.vkCommandBuffer();

            vkCommandBuffer.reset();
            final VkClearValue.Buffer clearValues = VkClearValue.callocStack(1, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 1));

            final VkRect2D renderArea = VkRect2D.callocStack(stack);
            renderArea.offset().set(0, 0);
            renderArea.extent().set(width, height);

            final VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(this.vkRenderPass.getPointer())
                .pClearValues(clearValues)
                .framebuffer(vkFramebuffer.getPointer())
                .renderArea(renderArea);

            vkCommandBuffer.begin(null);
            VkCommandBuffer cmdHandle = vkCommandBuffer.getNative();
            VK11.vkCmdBeginRenderPass(cmdHandle, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);

            VK11.vkCmdBindPipeline(cmdHandle, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, this.vkPipeline.getPointer());

            VkViewport.Buffer viewport = VkViewport.callocStack(1, stack)
                .x(0)
                .y(height)
                .height(-height)
                .width(width)
                .minDepth(0.0f)
                .maxDepth(1.0f);
            VK11.vkCmdSetViewport(cmdHandle, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.callocStack(1, stack)
                .extent(it -> it
                    .width(width)
                    .height(height))
                .offset(it -> it
                    .x(0)
                    .y(0));
            VK11.vkCmdSetScissor(cmdHandle, 0, scissor);

            LongBuffer descriptorSets = stack.mallocLong(1).put(0, this.vkUniformBuffer0.getDescriptorSetPointer());
            VK11.vkCmdBindDescriptorSets(cmdHandle, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, this.vkPipeline.getPipelineLayout(), 0, descriptorSets, null);

            VK11.vkCmdDraw(cmdHandle, 3, 1, 0, 0);

            VK11.vkCmdEndRenderPass(cmdHandle);
            vkCommandBuffer.end();
        }
    }

    @Override
    public void dispose()
    {
        this.vkUniformBuffer0.dispose();
        this.vkPipeline.dispose();
        this.vkFrames.forEach(Frame::dispose);
        this.vkShaderProgram.dispose();
        this.vkRenderPass.dispose();
    }

    @Override
    public Class<? extends IEntityCollection> getEntityCollectionType()
    {
        return null;
    }

    @Override
    public void resize(VulkanRenderContext vkContext)
    {

    }

    @Override
    public long[] getDescriptorSetPointers()
    {
        return new long[0];
    }

    @Override
    public VulkanDescriptorSetLayout[] getDescriptorSetLayouts()
    {
        return new VulkanDescriptorSetLayout[0];
    }

    @Override
    public long getPointer()
    {
        return this.vkRenderPass.getPointer();
    }

    @Override
    public void finish()
    {

    }
}
