package me.buhlmann.engine4.core.gfx.vulkan.renderer.deferred;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.annotation.EventListener;
import me.buhlmann.engine4.api.renderer.IRenderer;
import me.buhlmann.engine4.api.renderer.DeferredRenderStage;
import me.buhlmann.engine4.api.scene.IScene;
import me.buhlmann.engine4.core.gfx.vulkan.*;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanRenderContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanUniformBuffer;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.IVulkanRenderPass;
import me.buhlmann.engine4.core.gfx.vulkan.renderer.IVulkanRenderer;
import me.buhlmann.engine4.core.gfx.vulkan.utils.VulkanUtils;
import me.buhlmann.engine4.event.WindowResizeEvent;
import me.buhlmann.engine4.platform.window.GLFWWindow;
import me.buhlmann.engine4.utils.ListMap;

import java.lang.reflect.InvocationTargetException;

public class VulkanDeferredRenderer3D implements IVulkanRenderer, IVulkanDisposable
{
    private VulkanRenderContext vkContext;

    private ListMap<DeferredRenderStage, IVulkanRenderPass> vkRenderStages;

    public VulkanRenderContext getContext()
    {
        return this.vkContext;
    }

    public IVulkanRenderPass getRenderPass(final DeferredRenderStage vkDeferredRenderStage)
    {
        // TODO: Un-shit this.
        return this.vkRenderStages.getAll(vkDeferredRenderStage).stream().findFirst().get();
    }

    public VulkanGBuffer getGBuffer()
    {
        // TODO: Maybe just put this into the renderer itself, as multiple stages operate on it?
        return ((VulkanGeometryRenderPass) this.vkRenderStages.getAll(DeferredRenderStage.GEOMETRY).stream().findFirst().get()).getGBuffer();
    }

    @Override
    public void initialize(GLFWWindow window)
    {
        this.vkContext = VulkanUtils.newRenderContext(window);

        this.vkRenderStages = new ListMap<>();
        this.vkRenderStages.instantiate(DeferredRenderStage.values());
        this.vkRenderStages.put(DeferredRenderStage.GEOMETRY, new VulkanGeometryRenderPass(this.vkContext));
        this.vkRenderStages.put(DeferredRenderStage.LIGHTING_DIRECTIONAL, new VulkanLightingRenderPass(this.vkContext, this.getGBuffer()));

        Engine4.getEventBus().subscribe(this);
    }

    @Override
    public void terminate()
    {
        this.dispose();
    }

    @Override
    public void dispose()
    {
        for (final DeferredRenderStage stage : DeferredRenderStage.values())
        {
            for (final IVulkanRenderPass vkRenderPass : this.vkRenderStages.getAll(stage))
            {
                vkRenderPass.dispose();
            }
        }

        this.vkContext.dispose();
    }

    @Override
    public void bind(IScene scene)
    {
        for (DeferredRenderStage stage : DeferredRenderStage.values())
        {
            for (IVulkanRenderPass pass : this.vkRenderStages.getAll(stage))
            {
                try
                {
                    if (pass.getEntityCollectionType() != null)
                    {
                        scene.getECS().add(pass.getEntityCollectionType().getConstructor().newInstance());
                    }
                }
                catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
                {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    @Override
    public void render(IRenderer.Input input)
    {
        if (input.window().getSize().equals(0, 0))
        {
            return;
        }

        this.vkContext.getSwapChain().next();

        for (final DeferredRenderStage stage : DeferredRenderStage.values())
        {
            for (final IVulkanRenderPass vkRenderPass : this.vkRenderStages.getAll(stage))
            {
                vkRenderPass.render(input, this.vkContext);
            }
        }

        this.vkContext.getSwapChain().present(this.vkContext.getGraphicsQueue());
    }

    @Override
    public void finish()
    {
        for (final DeferredRenderStage stage : DeferredRenderStage.values())
        {
            for (final IVulkanRenderPass vkRenderPass : this.vkRenderStages.getAll(stage))
            {
                vkRenderPass.finish();
            }
        }
    }

    @EventListener(WindowResizeEvent.class)
    public void onWindowResize(final WindowResizeEvent event)
    {
        this.vkContext.getLogicalDevice().idle();
        this.vkContext.getGraphicsQueue().idle();
        this.vkContext.getSwapChain().dispose();

        this.vkContext.setSwapChain(new VulkanSwapChain(
            this.vkContext.getLogicalDevice(),
            this.vkContext.getPhysicalDevice(),
            this.vkContext.getSurface(),
            this.vkContext.getSwapChain().getSize(),
            true // lazy...
        ));

        for (final DeferredRenderStage stage : DeferredRenderStage.values())
        {
            for (final IVulkanRenderPass vkRenderPass : this.vkRenderStages.getAll(stage))
            {
                vkRenderPass.resize(this.vkContext);
            }
        }
    }

    public VulkanRenderContext getRenderContext()
    {
        return this.vkContext;
    }
}
