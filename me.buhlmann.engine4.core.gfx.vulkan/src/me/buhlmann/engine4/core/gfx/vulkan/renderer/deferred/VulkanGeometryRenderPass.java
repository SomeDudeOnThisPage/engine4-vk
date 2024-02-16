package me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.api.entity.IEntity;
import me.buhlmann.engine4.api.entity.IEntityCollection;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.primitive.IMesh;
import me.buhlmann.engine4.api.gfx.primitive.IModel;
import me.buhlmann.engine4.api.renderer.DeferredRenderStage;
import me.buhlmann.engine4.api.renderer.IRenderer;
import me.buhlmann.engine4.core.gfx.vulkan.*;
import me.buhlmann.engine4.core.gfx.vulkan.asset.VulkanDeferredMaterial;
import me.buhlmann.engine4.core.gfx.vulkan.asset.VulkanModel;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanBufferedUniform;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanDescriptorSetLayout;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanUniformBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.material.VulkanShaderEffect;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.*;
import me.buhlmann.engine4.entity.component.TransformComponent;
import me.buhlmann.engine4.utils.AssetUtils;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class VulkanGeometryRenderPass implements IVulkanRenderPass
{
    private record Frame(
        VulkanAttachment vkDepthAttachment,
        VulkanFramebuffer vkFramebuffer,
        VulkanCommandBuffer vkCommandBuffer,
        VulkanFence vkFence
    ) implements IVulkanDisposable {
        @Override
        public void dispose()
        {
            this.vkFence.hold();
            this.vkDepthAttachment.dispose();
            this.vkFramebuffer.dispose();
            this.vkFence.dispose();
            this.vkCommandBuffer.dispose();
        }
    }

    private final List<Frame> vkFrames;
    private final VulkanFramebufferRenderPass vkRenderPass;
    private VulkanUniformBuffer vkUniformBuffer0;
    private VulkanGBuffer vkGBuffer;

    public VulkanGeometryRenderPass(final VulkanRenderContext vkContext)
    {
        final VkExtent2D vkSwapChainExtent = vkContext.getSwapChain().getSwapChainExtent();
        this.vkGBuffer = new VulkanGBuffer(new Vector2i(vkSwapChainExtent.width(), vkSwapChainExtent.height()), vkContext);
        this.vkRenderPass = new VulkanFramebufferRenderPass(vkContext, vkGBuffer); // new VulkanSwapChainRenderPass(vkContext, VK11.VK_FORMAT_D32_SFLOAT);

        this.createPassUniformBuffer(vkContext);
        this.vkFrames = new ArrayList<>(vkContext.getSwapChain().getSize());
        for (int i = 0; i < vkContext.getSwapChain().getSize(); i++)
        {
            this.vkFrames.add(this.createFrame(vkContext, i));
        }
    }

    private Frame createFrame(final VulkanRenderContext vkContext, int index)
    {
        final VulkanFramebuffer vkFramebuffer = this.createFramebuffer(
            vkContext,
            this.vkGBuffer.getAttachments()
        );

        final VulkanCommandBuffer vkCommandBuffer = new VulkanCommandBuffer(vkContext, true, false);
        final VulkanFence vkFence = new VulkanFence(vkContext.getLogicalDevice(), true);
        return new Frame(this.vkGBuffer.getDepthAttachment(), vkFramebuffer, vkCommandBuffer, vkFence);
    }

    @Override
    public void dispose()
    {
        Engine4.getLogger().trace("[VULKAN] destroying VulkanGeometryRenderPass");
        this.vkFrames.forEach(Frame::dispose);
        this.vkUniformBuffer0.dispose();
        this.vkRenderPass.dispose();
    }

    @Override
    public Class<? extends IEntityCollection> getEntityCollectionType()
    {
        return SortedEntityCollection.class;
    }

    @Override
    public void resize(final VulkanRenderContext vkContext)
    {
        final List<Frame> vkFramesNew = new ArrayList<>();
        Engine4.getLogger().trace("[VULKAN] resizing frames of GeometryRenderPass - old count: " + this.vkFrames.size() + " - new count: " + vkContext.getSwapChain().getSize());
        for (int i = 0; i < vkContext.getSwapChain().getSize(); i++)
        {
            final Frame old = this.vkFrames.get(i);
            old.vkDepthAttachment().dispose();
            old.vkFramebuffer().dispose();

            final VkExtent2D vkSwapChainExtent = vkContext.getSwapChain().getSwapChainExtent();
            this.vkGBuffer = new VulkanGBuffer(new Vector2i(vkSwapChainExtent.width(), vkSwapChainExtent.height()), vkContext);
            // final VulkanAttachment vkDepthAttachment = this.createDepthAttachment(vkContext);
            final VulkanFramebuffer vkFramebuffer = this.createFramebuffer(
                vkContext,
                this.vkGBuffer.getAttachments()
            );

            final VulkanCommandBuffer vkCommandBuffer = old.vkCommandBuffer();
            final VulkanFence vkFence = old.vkFence();

            final Frame frame = new Frame(
                this.vkGBuffer.getDepthAttachment(),
                vkFramebuffer,
                vkCommandBuffer,
                vkFence
            );

            vkFramesNew.add(frame);
        }

        this.vkFrames.clear();
        this.vkFrames.addAll(vkFramesNew);
    }

    @Override
    public void render(final IRenderer.Input input, final VulkanRenderContext vkContext)
    {
        this.recordCommandBuffer(vkContext, input, input.scene().getECS().get(SortedEntityCollection.class));
        this.submit(vkContext);
    }

    @Override
    public long[] getDescriptorSetPointers()
    {
        return new long[]
        {
            this.vkUniformBuffer0.getDescriptorSetPointer()
        };
    }

    @Override
    public VulkanDescriptorSetLayout[] getDescriptorSetLayouts()
    {
        return this.vkUniformBuffer0.getDescriptorSetLayouts();
    }

    @Override
    public long getPointer()
    {
        return this.vkRenderPass.getPointer();
    }

    @Override
    public void finish()
    {
        this.vkFrames.forEach((frame) -> {
            frame.vkFence.hold();
        });
    }

    public VulkanGBuffer getGBuffer()
    {
        return this.vkGBuffer;
    }

    private void createPassUniformBuffer(final VulkanRenderContext vkContext)
    {
        this.vkUniformBuffer0 = VulkanUniformBuffer.create(vkContext, 0,
            new VulkanBufferedUniform<>("camera_position", new Vector4f()),
            new VulkanBufferedUniform<>("view", new Matrix4f()),
            new VulkanBufferedUniform<>("projection", new Matrix4f())
        );
    }

    private VulkanAttachment createDepthAttachment(final VulkanRenderContext vkContext)
    {
        final VkExtent2D vkSwapChainExtent = vkContext.getSwapChain().getSwapChainExtent();
        return new VulkanAttachment(
            new Vector2i(vkSwapChainExtent.width(), vkSwapChainExtent.height()),
            VK11.VK_FORMAT_D32_SFLOAT,
            VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
            vkContext
        );
    }

    private VulkanFramebuffer createFramebuffer(final VulkanRenderContext vkContext, final List<VulkanAttachment> vkAttachments)
    {
        final VkExtent2D vkSwapChainExtent = vkContext.getSwapChain().getSwapChainExtent();

        try (final MemoryStack stack = MemoryStack.stackPush())
        {
            final LongBuffer pointers = stack.mallocLong(vkAttachments.size());

            for (int i = 0; i < vkAttachments.size(); i++)
            {
                pointers.put(i, vkAttachments.get(i).getImageView().getPointer());
            }

            return new VulkanFramebuffer(
                vkContext.getLogicalDevice(),
                vkSwapChainExtent.width(),
                vkSwapChainExtent.height(),
                pointers,
                this.vkRenderPass.getPointer()
            );
        }
    }

    private IAssetReference<IMaterial> defMat = null;
    private void recordMeshCommand(final MemoryStack stack, int width, int height, final VulkanCommandBuffer vkCommandBuffer, final VulkanModel vkModel, final int index, final Collection<IEntity> entities)
    {
        final IAssetReference<IMesh> mesh = vkModel.getMeshes().get(index);
        if (defMat == null)
        {
            defMat = Engine4.getAssetManager().request(IMaterial.class, "mtl.flat-color");
        }

        final IAssetReference<IMaterial> material = vkModel.getMaterials().size() > index
            ? vkModel.getMaterials().get(index)
            : defMat;

        if (AssetUtils.isNotLoaded(mesh) || AssetUtils.isNotLoaded(material))
        {
            return;
        }

        // Check if we actually have a VulkanMesh and VulkanMaterial.
        if (mesh.get() instanceof VulkanModel.VulkanMesh vkMesh && material.get() instanceof VulkanDeferredMaterial vkMaterial)
        {
            final VulkanShaderEffect vkGeometryEffect = vkMaterial.getShaderEffect(0);
            final VulkanPipeline vkPipeline = vkGeometryEffect.getPipeline();

            vkMaterial.bindPipeline(DeferredRenderStage.GEOMETRY, this, vkCommandBuffer, stack);

            final VkViewport.Buffer viewport = VkViewport.callocStack(1, stack)
                .x(0)
                .y(height)
                .height(-height)
                .width(width)
                .minDepth(0.0f)
                .maxDepth(1.0f);
            VK11.vkCmdSetViewport(vkCommandBuffer.getNative(), 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.callocStack(1, stack)
                .extent(it -> it
                    .width(width)
                    .height(height))
                .offset(it -> it
                    .x(0)
                    .y(0));
            VK11.vkCmdSetScissor(vkCommandBuffer.getNative(), 0, scissor);

            LongBuffer offsets = stack.mallocLong(1);
            offsets.put(0, 0L);

            final LongBuffer vertexBuffer = stack.mallocLong(1);
            final ByteBuffer pushConstantBuffer = stack.malloc(64 * 2);
            vertexBuffer.put(0, vkMesh.vertices().getPointer());
            VK11.vkCmdBindVertexBuffers(vkCommandBuffer.getNative(), 0, vertexBuffer, offsets);
            VK11.vkCmdBindIndexBuffer(vkCommandBuffer.getNative(), vkMesh.indices().getPointer(), 0, VK11.VK_INDEX_TYPE_UINT32);

            // Render all entities with the current mesh.
            for (final IEntity entity : entities)
            {
                vkMaterial.bind(DeferredRenderStage.GEOMETRY, this, vkCommandBuffer, stack);

                final Matrix4f transform = entity.getComponent(TransformComponent.class).getTransformMatrix();
                this.setPushConstants(vkCommandBuffer.getNative(), vkPipeline, transform, pushConstantBuffer);
                VK11.vkCmdDrawIndexed(vkCommandBuffer.getNative(), vkMesh.numIndices(), 1, 0, 0, 0);
            }
        }
        else
        {
            Engine4.getLogger().error("[VULKAN] [RENDERER] got instance of (unloaded) " + mesh.get().getClass().getName() + ", expected loaded VulkanModel.VulkanMesh.");
        }
    }

    public void recordCommandBuffer(final VulkanRenderContext vkContext, IRenderer.Input input, final SortedEntityCollection entities)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final VkExtent2D swapChainExtent = vkContext.getSwapChain().getSwapChainExtent();
            final int width = swapChainExtent.width();
            final int height = swapChainExtent.height();

            final Frame frame = this.vkFrames.get(vkContext.getSwapChain().getCurrentFrame());
            frame.vkFence.hold();
            frame.vkFence.reset();

            frame.vkCommandBuffer.reset();

            VkClearValue.Buffer clearValues = VkClearValue.callocStack(this.vkGBuffer.getAttachments().size(), stack);
            for (final VulkanAttachment vKAttachment : this.vkGBuffer.getAttachments())
            {
                if (vKAttachment.isDepthAttachment())
                {
                    clearValues.apply(v -> v.depthStencil().depth(1.0f));
                }
                else
                {
                    clearValues.apply(v -> v.color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 1));
                }
            }
            clearValues.flip();

            final VkRenderPassBeginInfo vkRenderPassBeginInfo = VkRenderPassBeginInfo.callocStack(stack)
                .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(this.vkRenderPass.getPointer())
                .pClearValues(clearValues)
                .renderArea(a -> a.extent().set(width, height))
                .framebuffer(frame.vkFramebuffer.getPointer());

            frame.vkCommandBuffer.begin(null);
            final VkCommandBuffer cmdHandle = frame.vkCommandBuffer.getNative();

            VK11.vkCmdBeginRenderPass(cmdHandle, vkRenderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);

            this.vkUniformBuffer0.set("projection", input.camera().getProjectionMatrix());
            this.vkUniformBuffer0.set("view", input.camera().getTransform());

            // Iterate all mesh keys.
            for (String index : entities.getKeySet())
            {
                // Check if the mesh referenced exists, and if it is loaded.
                final IAssetReference<IModel> model = Engine4.getAssetManager().request(IModel.class, index);
                if (AssetUtils.isNotLoaded(model))
                {
                    continue;
                }

                for (int i = 0; i < model.get().getMeshes().size(); i++)
                {
                    stack.push();
                    if (model.get() instanceof VulkanModel vkModel)
                    {
                        this.recordMeshCommand(stack, width, height, frame.vkCommandBuffer(), vkModel, i, entities.getEntitiesByIndex(index));
                    }
                    stack.pop();
                }
                Engine4.getAssetManager().release(IModel.class, model.getKey());
            }

            VK11.vkCmdEndRenderPass(cmdHandle);
            frame.vkCommandBuffer.end();
        }
    }

    public void submit(final VulkanRenderContext vkContext)
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            final Frame frame = this.vkFrames.get(vkContext.getSwapChain().getCurrentFrame());
            VulkanSwapChain.VulkanSyncSemaphores semaphores = vkContext.getSwapChain().getSemaphores(vkContext.getSwapChain().getCurrentFrame());
            vkContext.getGraphicsQueue().submit(
                stack.pointers(frame.vkCommandBuffer.getNative()),
                stack.longs(semaphores.acquisition().getPointer()),
                stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                stack.longs(semaphores.phase().getPointer()),
                frame.vkFence
            );
        }
    }

    @Deprecated // TODO: In material system!
    private void setPushConstants(VkCommandBuffer cmdHandle, VulkanPipeline vkPipeline, Matrix4f modelMatrix, ByteBuffer pushConstantBuffer) {
        // projMatrix.get(pushConstantBuffer);
        modelMatrix.get(0, pushConstantBuffer);
        VK11.vkCmdPushConstants(cmdHandle, vkPipeline.getPipelineLayout(), VK11.VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantBuffer);
    }
}
